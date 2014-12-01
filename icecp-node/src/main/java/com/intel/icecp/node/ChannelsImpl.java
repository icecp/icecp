/*
 * Copyright (c) 2017 Intel Corporation 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.icecp.node;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.channels.ChannelProvider;
import com.intel.icecp.core.channels.Token;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.management.ConfigurationManager;
import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.metadata.formats.BytesFormat;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.permissions.ChannelProviderPermission;
import com.intel.icecp.node.pipeline.implementations.MessageFormattingPipeline;
import com.intel.icecp.node.utils.MetadataUtils;
import com.intel.icecp.node.utils.SecurityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Implementation of {@link Channels}. Supports registering a fallback provider with the '*' character that will cover
 * schemes that are not found.
 *
 */
class ChannelsImpl implements Channels {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String FALLBACK_SCHEME = "*";
    private static final int DEFAULT_OPEN_TIMEOUT = 10;
    private final Map<URI, Channel> channels = new ConcurrentHashMap<>();
    private final Map<String, ChannelProvider> registered = new ConcurrentHashMap<>();
    private final ScheduledExecutorService pool;
    private final ConfigurationManager configurationManager;

    ChannelsImpl(ScheduledExecutorService pool, ConfigurationManager configurationManager) {
        this.pool = pool;
        this.configurationManager = configurationManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(String scheme, ChannelProvider implementation) {
        SecurityUtils.checkPermission(new ChannelProviderPermission(scheme,
                ChannelProviderPermission.Action.REGISTER.toString()));
        try {
            registered.put(scheme, implementation);
            Configuration configuration = configurationManager.get(implementation.scheme());
            configuration.load();
            implementation.start(pool, configuration);
        } catch (ChannelIOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregister(String scheme) {
        SecurityUtils.checkPermission(new ChannelProviderPermission(scheme,
                ChannelProviderPermission.Action.UNREGISTER.toString()));
        registered.remove(scheme);

        //pick all channels matching scheme
        List<URI> matchedChannels = channels.keySet().stream().filter(c -> c.getScheme().equals(scheme)).collect(Collectors.toList());

        //close all matching channels
        matchedChannels.stream().forEach(uri -> {
            try {
                channels.get(uri).close();
            } catch (ChannelLifetimeException e) {
                LOGGER.error("Failed to close channel, removing regardless: {}", uri, e);
            } finally {
                channels.remove(uri);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        SecurityUtils.checkPermission(new ChannelProviderPermission(ChannelProviderPermission.Action.SHUTDOWN.toString()));
        // close all channels
        for (Channel channel : channels.values()) {
            try {
                channel.close();
            } catch (ChannelLifetimeException ex) {
                LOGGER.error("Unable to close channel: {}", channel, ex);
            }
        }

        // stop all transports
        registered.values().forEach(ChannelProvider::stop);

        // remove all providers
        registered.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChannelProvider get(String scheme) {
        if(scheme == null || scheme.isEmpty()) throw new IllegalArgumentException("Only non-null, non-empty schemes are expected");
        if (registered.containsKey(scheme)) {
            return registered.get(scheme);
        } else if (registered.containsKey(FALLBACK_SCHEME)) {
            return registered.get(FALLBACK_SCHEME);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Message> Channel<T> openChannel(URI uri, Class<T> messageType, Persistence persistence, Metadata... metadata) throws ChannelLifetimeException {
        return openChannelImpl(uri, Token.of(messageType), persistence, metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Message> Channel<T> openChannel(URI uri, Token<T> messageType, Persistence persistence, Metadata... metadata) throws ChannelLifetimeException {
        return openChannelImpl(uri, messageType, persistence, metadata);
    }

    private <T extends Message> Channel<T> openChannelImpl(URI uri, Token<T> messageType, Persistence persistence, Metadata... metadata) throws ChannelLifetimeException {
        LOGGER.debug("Opening channel: {}", uri);

        ChannelProvider provider = get(uri.getScheme());
        if (provider == null) {
            throw new ChannelLifetimeException("No provider found for scheme on: " + uri);
        }

        Format messageFormat = chooseFormat(messageType, metadata);
        LOGGER.debug("Using format {} for channel: {}", messageFormat.getClass().getSimpleName(), uri);

        // @Moreno: Create a simple MessageFormattingPipeline with the chosen format
        Channel<T> channel = provider.build(uri, MessageFormattingPipeline.create(messageType, messageFormat), persistence, metadata);
        try {
            channel.open().get(DEFAULT_OPEN_TIMEOUT, TimeUnit.SECONDS);
            channels.put(uri, channel);
            return channel;
        } catch (TimeoutException | InterruptedException | ExecutionException ex) {
            throw new ChannelLifetimeException("Failed to open channel: " + uri, ex);
        }
    }

    /**
     * Determine the format to use from (in order): <ol> <li>a specified format</li> <li>a bytes-related message</li>
     * <li>the default</li> </ol>
     *
     * @param <T> a message type
     * @param messageType the type of message, necessary for some serializers
     * @param metadata the list of metadata objects
     * @return a format
     */
    <T extends Message> Format chooseFormat(Token<T> messageType, Metadata[] metadata) {
        Format specified = MetadataUtils.find(Format.class, metadata);
        if (specified != null) {
            return specified;
        } else if (messageType.isAssignableFrom(BytesMessage.class)) {
            return new BytesFormat();
        } else {
            return buildDefaultFormat(messageType);
        }
    }

    /**
     * Build the default format to use; TODO use CBOR?
     *
     * @param <T> the {@link Message} type
     * @param messageType a {@link Class} instance of the {@link Message} type
     * @return a {@link Format}
     */
    private <T extends Message> Format<T> buildDefaultFormat(Token<T> messageType) {
        return (Format<T>) new JsonFormat(messageType);
    }

    /**
     * {@inheritDoc}
     *
     * TODO: if its not open, is it safe to remove from channels list while iterating?
     */
    @Override
    public URI[] getOpenChannels() {
        Set<URI> uris = channels.values().stream().filter(Channel::isOpen).map(Channel::getName).collect(Collectors.toSet());
        return uris.toArray(new URI[]{});
    }
}
