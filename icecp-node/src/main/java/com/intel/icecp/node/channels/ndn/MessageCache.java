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

package com.intel.icecp.node.channels.ndn;

import com.intel.icecp.core.Message;
import com.intel.icecp.node.utils.BoundedLinkedMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Store a bounded number of messages and allow clearing of expired messages.
 * <p>
 * This class is thread-safe against changes to the underlying map; in other words, a change to the underlying map will
 * affect the looping in {@link #clean()} causing a {@link java.util.ConcurrentModificationException}. To avoid this, we
 * synchronize any method that modifies the map. State-checking methods were left unsynchronized since the underlying
 * map is synchronized (see {@link BoundedLinkedMap}).
 *
 */
public class MessageCache {

    private static final Logger LOGGER = LogManager.getLogger();
    private final BoundedLinkedMap<Long, MessageEntry> messages;
    private final long retention;

    /**
     * @param retention the number of milliseconds to retain messages
     * @param maxSize the max number of messages to retain
     */
    public MessageCache(long retention, int maxSize) {
        this.retention = retention;
        this.messages = new BoundedLinkedMap<>(maxSize);
    }

    /**
     * @param id the message ID
     * @return true if the message exists
     */
    public boolean has(long id) {
        return messages.containsKey(id);
    }

    /**
     * @return the ID of the earliest inserted message
     */
    public long earliest() {
        return messages.isEmpty() ? -1 : messages.earliest();
    }

    /**
     * @return the ID of the latest inserted message
     */
    public long latest() {
        return messages.isEmpty() ? -1 : messages.latest();
    }

    /**
     * @param id the unique identifier for a {@link Message}
     * @return the {@link Message} or null
     */
    public Message get(long id) {
        MessageEntry retrieved = messages.get(id); // the underlying map is synchronized
        return retrieved != null ? retrieved.message : null;
    }

    /**
     * Add a message to the cache
     *
     * @param id the message ID
     * @param message the message instance
     */
    public synchronized void add(long id, Message message) {
        long expiresOn = (retention == Long.MAX_VALUE) ? Long.MAX_VALUE : Math.addExact(System.currentTimeMillis(), retention);
        add(id, message, expiresOn);
    }

    /**
     * Add a message to this publisher; this method was created mainly to test with mock timestamps
     *
     * @param id the message ID
     * @param message the message instance
     * @param expiresOn the milliseconds epoch time to expire the message
     */
    protected void add(long id, Message message, long expiresOn) {
        LOGGER.trace("Caching message {} to expire on {}", id, new Date(expiresOn));
        messages.put(id, new MessageEntry(message, expiresOn));
    }

    /**
     * @param id the message ID
     * @return the removed message or null if none was removed
     */
    public synchronized Message remove(long id) {
        LOGGER.trace("Removing message {}", id);
        MessageEntry removed = messages.remove(id); // the underlying map is synchronized
        return removed != null ? removed.message : null;
    }

    /**
     * @return the expired messages that have been removed from the cache with their IDs as keys
     */
    public synchronized Map<Long, Message> clean() {
        long currentTimestamp = System.currentTimeMillis();
        Map<Long, Message> removed = new HashMap<>();

        // find expired entries
        for (Map.Entry<Long, MessageEntry> entry : messages.entrySet()) {
            if (entry.getValue().expiresOn < currentTimestamp) {
                removed.put(entry.getKey(), entry.getValue().message);
            }
        }

        // now remove to avoid ConcurrentModificationException
        for (long id : removed.keySet()) {
            LOGGER.trace("Cleaning up expired message {}", id);
            messages.remove(id);
        }

        return removed;
    }

    /**
     * @return the earliest time this cache can be shut down without discarding cached messages
     */
    public long getEarliestCloseTime() {
        OptionalLong maxExpiration = messages.values().stream().mapToLong((MessageEntry me) -> me.expiresOn).max();
        return maxExpiration.orElse(System.currentTimeMillis());
    }

    /**
     * Track {@link Message} expiration times
     */
    private class MessageEntry {

        final long expiresOn; // ms timestamp
        final Message message;

        MessageEntry(Message message, long expiresOn) {
            this.expiresOn = expiresOn;
            this.message = message;
        }
    }
}
