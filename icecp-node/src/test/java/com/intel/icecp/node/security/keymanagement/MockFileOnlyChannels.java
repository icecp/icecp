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
package com.intel.icecp.node.security.keymanagement;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.channels.ChannelProvider;
import com.intel.icecp.core.channels.Token;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.node.channels.file.FileChannel;
import com.intel.icecp.node.channels.file.FileChannelProvider;
import com.intel.icecp.node.messages.security.CertificateMessage;
import com.intel.icecp.node.pipeline.implementations.MessageFormattingPipeline;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Mock channels class for testing 
 * 
 */
public class MockFileOnlyChannels implements Channels {

    /**
     * UTILITY: Creates a {@link FileChannel} that loads a 
     * {@link CertificateMessage}
     *
     * @param certificateID Id of the certificate (in this case, the path in the
     * file system)
     * @return A channel to fetch the certificate from a local file
     * @throws Exception If the channel creation fails
     */
    private Channel<CertificateMessage> creteFileChannel(String certificateID) throws ChannelLifetimeException {
        Path root = Paths.get("");
        FileChannelProvider builder = new FileChannelProvider();
        Pipeline pipeline = MessageFormattingPipeline.create(CertificateMessage.class, new JsonFormat<>(CertificateMessage.class));
        Channel<CertificateMessage> channel = builder.build(
                root.resolve(certificateID).toUri(),
                pipeline,
                new Persistence());
        return channel;
    }

    /**
     * Not needed {@inheritDoc }
     */
    @Override
    public void register(String scheme, ChannelProvider implementation) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ChannelProvider get(String scheme) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Not needed {@inheritDoc }
     */
    @Override
    public void unregister(String scheme) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Not needed {@inheritDoc }
     */
    @Override
    public void shutdown() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Not needed {@inheritDoc }
     */
    @Override
    public <T extends Message> Channel<T> openChannel(URI uri, Class<T> messageType, Persistence persistence, Metadata... metadata) throws ChannelLifetimeException {
        return openChannel(uri, Token.of(messageType), persistence, metadata);
    }

    /**
     * Always returns a {@link FileChannel}
     * {@inheritDoc }
     */
    @Override
    public <T extends Message> Channel<T> openChannel(URI uri, Token<T> messageType, Persistence persistence, Metadata... metadata) throws ChannelLifetimeException {
        try {
            Channel ch = creteFileChannel(uri.toString());
            ch.open().get(10, TimeUnit.SECONDS);
            return ch;
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            throw new ChannelLifetimeException("Unable to open the channel", ex);
        }
    }

    /**
     * Not needed {@inheritDoc }
     */
    @Override
    public URI[] getOpenChannels() {
        return null;
    }

}
