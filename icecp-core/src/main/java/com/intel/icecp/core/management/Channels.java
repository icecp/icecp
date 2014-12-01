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
