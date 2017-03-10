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
