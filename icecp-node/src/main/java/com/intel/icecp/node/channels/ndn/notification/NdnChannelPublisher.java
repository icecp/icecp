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
package com.intel.icecp.node.channels.ndn.notification;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.channels.OnLatest;
import com.intel.icecp.core.event.EventObservable;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.node.channels.ndn.MessageCache;
import com.intel.icecp.node.channels.ndn.MessageRequestHandler;
import com.intel.icecp.node.channels.ndn.NdnNotificationChannel;
import net.named_data.jndn.Data;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Manage all publishing of {@link Message}s on an {@link com.intel.icecp.node.channels.ndn.NdnNotificationChannel}; this
 * class is now factored out into {@link MessageCache} and {@link MessageRequestHandler} for use with other channel
 * types.
 *
 */
public class NdnChannelPublisher {

    private static final int MAX_CACHED_MESSAGES = 65536;
    private final MessageCache cache;
    private final MessageRequestHandler handler;

    /**
     * Creates a new instance of <code>NdnChannelPublisher</code>
     *
     * @param prefix the NDN prefix of the channel; for building response packets and filtering
     * @param pipeline the operations necessary for converting {@link Message}s to bytes for transmission
     * @param pool the thread pool in which to run the encoding and transmission tasks
     * @param marker the NDN component tag identifying a message ID
     * @param persistence the channel persistence
     * @param observable the observer helper; for alerting watchers to internal events
     * @param filterable the object on which to add the NDN filters
     */
    public NdnChannelPublisher(Name prefix, Pipeline<Message, InputStream> pipeline, ExecutorService pool, int marker, Persistence persistence, EventObservable observable, Filterable filterable) {
        this.cache = new MessageCache(persistence.persistFor, MAX_CACHED_MESSAGES);

        // append filter to avoid metadata and update requests
        handler = new MessageRequestHandler(buildDataTemplate(prefix, persistence), cache, marker, pipeline, pool, observable);
        filterable.addFilter(buildDataFilter(prefix), handler);
    }

    /**
     * @param prefix the NDN name to observe for matching packets
     * @return an {@link InterestFilter} for data packets
     */
    InterestFilter buildDataFilter(Name prefix) {
        return new InterestFilter(new Name(prefix).append(NdnNotificationChannel.DATA_SUFFIX));
    }

    /**
     * @param id the unique identifier for a {@link Message}
     * @return true if this publisher has the {@link Message}
     */
    boolean hasMessage(long id) {
        return cache.has(id);
    }

    /**
     * @param id the unique identifier for a {@link Message}
     * @return the {@link Message} or null
     */
    public Message getMessage(long id) {
        return cache.get(id);
    }

    /**
     * Add a message to this publisher
     *
     * @param id the unique identifier for a {@link Message}
     * @param message the {@link Message} to publish
     */
    public void addMessage(long id, Message message) {
        cache.add(id, message);
    }

    /**
     * Build a {@link Data} template from the {@link Message}; this is used as the template for all segmented packets
     *
     * @param dataName the name of the packet
     * @param persistence the freshness period
     * @return a data template
     */
    private Data buildDataTemplate(Name dataName, Persistence persistence) {
        Data data = new Data(dataName);
        data.getName().append(NdnNotificationChannel.DATA_SUFFIX);
        if (persistence != null) {
            data.getMetaInfo().setFreshnessPeriod(persistence.persistFor);
        }
        return data;
    }

    /**
     * @return the earliest available {@link Message} ID for this publisher or -1 if there are no messages
     */
    public long getEarliestIdAvailable() {
        return cache.earliest();
    }

    /**
     * @return the latest available {@link Message} ID for this publisher or -1 if there are no messages
     */
    long getLatestIdAvailable() {
        return cache.latest();
    }

    /**
     * @return the earliest system time (in ms) that the publisher can close without losing any queued messages
     */
    public long getEarliestCloseTime() {
        return cache.getEarliestCloseTime();
    }

    /**
     * Remove all messages that have lived past their {@link Persistence} lifetime
     *
     * @return the removed messages
     */
    public Map<Long, Message> cleanup() {
        return cache.clean();
    }

    /**
     * Set the OnLatest handler for responding dynamically to requests
     *
     * @param latest the callback returning a response
     */
    public void setLatest(OnLatest latest) {
        handler.setOnLatest(latest);
    }
}
