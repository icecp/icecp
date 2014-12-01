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

package com.intel.icecp.core.mock;

import com.intel.icecp.core.channels.Token;
import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.channels.ChannelProvider;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.metadata.formats.BytesFormat;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.node.utils.MetadataUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Mock the channels API, allowing only creation of mock channels
 *
 */
public class MockChannels implements Channels {

    private ChannelProvider channelProvider = new MockChannelProvider();
    List<Channel> channels = new ArrayList<>();

    @Override
    public void register(String scheme, ChannelProvider implementation) {
        throw new UnsupportedOperationException("No changes can be made to this mock API.");
    }

    @Override
    public ChannelProvider get(String scheme) {
        return channelProvider;
    }

    @Override
    public void unregister(String scheme) {
        throw new UnsupportedOperationException("No changes can be made to this mock API.");
    }

    @Override
    public void shutdown() {
        // do nothing
    }

    @Override
    public <T extends Message> Channel<T> openChannel(URI uri, Class<T> messageType, Persistence persistence, Metadata... metadata) throws ChannelLifetimeException {
        return openChannel(uri, Token.of(messageType), persistence, metadata);
    }

    @Override
    public <T extends Message> Channel<T> openChannel(URI uri, Token<T> messageType, Persistence persistence, Metadata... metadata) throws ChannelLifetimeException {
    Format messageFormat = chooseFormat(messageType, metadata);
    Channel channel = channelProvider.build(uri, MockMessageFormattingPipeline.create(messageType, messageFormat), persistence, metadata);
        
        try {
            channel.open().get(10, TimeUnit.SECONDS);
            return channel;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new ChannelLifetimeException("failed to open channel", e);
        }
    }

    @Override
    public URI[] getOpenChannels() {
        return channels.stream().map((c) -> c.getName()).toArray(size -> new URI[size]);
    }
    
    //TODO: Move this to a new submodule that can be used by node and mock classes
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
        } else if (BytesMessage.class.isAssignableFrom(messageType.toClass())) {
            return new BytesFormat();
        } else {
            return buildDefaultFormat(messageType);
        }
    }
    
    //TODO: Move this to a new submodule that can be used by node and mock classes
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
}
