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
package com.intel.icecp.core;

import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.modules.Modules;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Represent the functionality provided by a device; different device types will
 * add functionality.
 *
 */
public interface Node extends Describable {

    /**
     * @return the device name; this should uniquely identify the device. On an
     * NDN-capable device, this will equal the device's routable prefix.
     */
    String getName();

    /**
     * @return the device name with the default transport scheme (e.g. ndn)
     * applied; this is for use as a base URI for channel creation
     */
    URI getDefaultUri();

    /**
     * @return a reference to the node's state channel; the node should publish
     * messages describing state changes on this channel.
     */
    Channel<Node.State> getStateChannel();

    /**
     * Start the {@link Node}
     */
    void start();

    /**
     * Stop the {@link Node}
     */
    void stop();

    /**
     * @return the modules interface for managing the modules on this node
     * @see Modules
     */
    Modules modules();

    /**
     * Load and start all modules in JAR. Use moduleNameRegex to filter the
     * module names to look for. Example, to only find modules with a name
     * suffix as "_Module" then specify ".*_Module$" in the moduleNameRegex
     * parameter.
     *
     * @param moduleUri a URI pointing to the location of a JAR file containing one or
     * more modules
     * @param configurationUri a URI pointing to a configuration file for the module
     * @return a future to the list of IDs of the running modules
     */
    CompletableFuture<Collection<Long>> loadAndStartModules(URI moduleUri, URI configurationUri);

    /**
     * @return the channels interface for channels and providers on this node
     * @see Channels
     */
    Channels channels();

    /**
     * Open an implementation of a channel with the given metadata; what
     * non-included metadata will default to is implementation-specific.
     *
     * @param <T> the {@link Message} type
     * @param uri the unique identifier for the channel, including the scheme to
     * open it (e.g. 'ndn:').
     * @param messageType a {@link Class} instance of the {@link Message}: This
     * type should be a descendent of {@link Message}. This is necessary for
     * serialization onto the channel.
     * @param persistence the declared caching/retrieval policy for messages on
     * the channel
     * @param metadata a list of metadata
     * @return the opened channel
     * @throws com.intel.icecp.core.misc.ChannelLifetimeException if the channel
     * cannot be opened
     */
    <T extends Message> Channel<T> openChannel(URI uri, Class<T> messageType, Persistence persistence, Metadata... metadata) throws ChannelLifetimeException;

    /**
     * Allowable node states.
     */
    enum State implements Message {
        LOADING, ON, OFF
    }
}
