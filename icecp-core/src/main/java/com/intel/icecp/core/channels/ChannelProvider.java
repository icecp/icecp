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
