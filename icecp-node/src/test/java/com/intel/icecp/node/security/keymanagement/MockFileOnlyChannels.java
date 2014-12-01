/*
 * ******************************************************************************
 *
 * INTEL CONFIDENTIAL
 *
 * Copyright 2013 - 2016 Intel Corporation All Rights Reserved.
 *
 * The source code contained or described herein and all documents related to
 * the source code ("Material") are owned by Intel Corporation or its suppliers
 * or licensors. Title to the Material remains with Intel Corporation or its
 * suppliers and licensors. The Material contains trade secrets and proprietary
 * and confidential information of Intel or its suppliers and licensors. The
 * Material is protected by worldwide copyright and trade secret laws and treaty
 * provisions. No part of the Material may be used, copied, reproduced,
 * modified, published, uploaded, posted, transmitted, distributed, or disclosed
 * in any way without Intel's prior express written permission.
 *
 * No license under any patent, copyright, trade secret or other intellectual
 * property right is granted to or conferred upon you by disclosure or delivery
 * of the Materials, either expressly, by implication, inducement, estoppel or
 * otherwise. Any license under such intellectual property rights must be
 * express and approved by Intel in writing.
 *
 * Unless otherwise agreed by Intel in writing, you may not remove or alter this
 * notice or any other notice embedded in Materials by Intel or Intel's
 * suppliers or licensors in any way.
 *
 * ******************************************************************************
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
