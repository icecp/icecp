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
package com.intel.icecp.core.management;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.channels.ChannelProvider;
import com.intel.icecp.core.channels.Token;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelLifetimeException;

import java.net.URI;

/**
 * Interface for creating and tracking channels; uses registered {@link ChannelProvider}s. Use:
 * <p>
 * <pre><code>
 * Channels channels = new Channels();
 * channels.register("ndn", new NdnChannelProvider());
 * channels.openChannel(URI.create("ndn:/..."), SomeMessage.class, ...);
 * </code></pre>
 *
 */
public interface Channels {

    /**
     * Register a scheme with the manager; this will allow for creating channels with the registered scheme
     *
     * @param scheme the scheme for the channels to build, e.g. 'ndn' in an URI like 'ndn:/a/b/c'
     * @param implementation a channel provider for building channels
     */
    void register(String scheme, ChannelProvider implementation);

    /**
     * @param scheme the scheme for the channels to build, e.g. 'ndn' in an URI like 'ndn:/a/b/c'
     * @return the channel provider for a specific scheme
     */
    ChannelProvider get(String scheme);

    /**
     * Unregister a registration, closing all channels opened under this scheme
     *
     * @param scheme the scheme for the channels to build, e.g. 'ndn' in an URI like 'ndn:/a/b/c'
     */
    void unregister(String scheme);

    /**
     * Close all channels and stop all channel registrations
     */
    void shutdown();

    /**
     * (Non-generic) Open an implementation of a channel with the given metadata; what non-included metadata will default to is
     * implementation-specific.  Use this method for non-generic classes such as String.
     *
     * @param <T> the {@link Message} type
     * @param uri the unique identifier for the channel, including the scheme to open it (e.g. 'ndn:').
     * @param messageType a {@link Class} instance of the {@link Message}
     * @param persistence the declared caching/retrieval policy for messages on the channel
     * @param metadata a list of metadata
     * @return the opened channel
     * @throws ChannelLifetimeException if the channel cannot be opened
     */
    <T extends Message> Channel<T> openChannel(URI uri, Class<T> messageType, Persistence persistence, Metadata... metadata) throws ChannelLifetimeException;

    /**
     * (Generic) Open an implementation of a channel with the given metadata; what non-included metadata will default to is
     * implementation-specific.  Use this method for generic classes such as {@literal List<String>}.
     *
     * @param <T> the {@link Message} type
     * @param uri the unique identifier for the channel, including the scheme to open it (e.g. 'ndn:').
     * @param messageType a {@link Token} instance of the {@link Message}
     * @param persistence the declared caching/retrieval policy for messages on the channel
     * @param metadata a list of metadata
     * @return the opened channel
     * @throws com.intel.icecp.core.misc.ChannelLifetimeException if the channel cannot be opened
     */
    <T extends Message> Channel<T> openChannel(URI uri, Token<T> messageType, Persistence persistence, Metadata... metadata) throws ChannelLifetimeException;

    /**
     * @return a list of open channel names
     */
    URI[] getOpenChannels();
}
