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
package com.intel.icecp.core.channels;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.pipeline.Pipeline;

import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Define a common interface for building a {@link Channel}; since network IO may involve some one-time setup and tear
 * down, the helper methods {@link #start(ScheduledExecutorService, Configuration)} and {@link #stop()} are present and
 * will be called before (or after, in the case of stop()) any channels are built.
 *
 */
public interface ChannelProvider {

    /**
     * @return the name of the scheme for this channel (e.g. "ndn")
     */
    String scheme();

    /**
     * One-time startup method to run before any channels are built.
     *
     * @param pool the thread pool provided by the node; a common thread pool is used so that the node can control
     * execution (e.g. shutdown)
     * @param configuration the specific configuration for this network scheme
     */
    void start(ScheduledExecutorService pool, Configuration configuration);

    /**
     * One-time method to tear down anything created in {@link #start(ScheduledExecutorService, Configuration)}.
     */
    void stop();

    /**
     * Build a {@link Channel}.
     *
     * @param <T> the message type
     * @param uri the URI of the channel to build
     * @param pipeline the channel pipeline for serializing/deserializing messages
     * @param persistence the persistence for all messages on the channel
     * @param metadata the channel metadata
     * @return an unopened {@link Channel}
     * @throws ChannelLifetimeException if the channel cannot be opened
     */
    <T extends Message> Channel<T> build(URI uri, Pipeline pipeline, Persistence persistence, Metadata... metadata) throws ChannelLifetimeException;
}
