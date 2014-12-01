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

import com.intel.icecp.common.TestMessage;
import com.intel.icecp.core.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 */
public class MessageCacheTest {

    private static final int RETENTION = 10;
    private static final int MAX_SIZE = 2;
    private static final Logger LOGGER = LogManager.getLogger();
    private MessageCache instance;

    @Before
    public void beforeTest() {
        instance = new MessageCache(RETENTION, MAX_SIZE);
    }

    @Test
    public void testAddingOverLimit() throws Exception {
        instance.add(0, new TestMessage());
        instance.add(1, new TestMessage());
        instance.add(2, new TestMessage());

        assertNotNull(instance.get(2));
        assertNull(instance.get(0));
    }

    @Test
    public void testCleanupAfterExpiration() {
        instance.add(5, null, 0);
        instance.add(3, null, System.currentTimeMillis() - RETENTION * 3);

        Map<Long, Message> removed = instance.clean();

        assertEquals(2, removed.size());
        assertTrue(removed.containsKey((long) 5));
        assertTrue(removed.containsKey((long) 3));
    }

    @Test
    public void testHas() {
        assertFalse(instance.has(0));
        instance.add(0, null);
        assertTrue(instance.has(0));
    }

    @Test
    public void testGet() {
        Message message = TestMessage.build("...", 0, MAX_SIZE, true);
        instance.add(42, message);
        assertEquals(message, instance.get(42));
    }

    @Test
    public void testRemove() {
        Message removed = instance.remove(999);
        assertNull(removed);

        Message added = new TestMessage();
        instance.add(1, added);
        assertEquals(added, instance.remove(1));
    }

    @Test
    public void testEarliest() {
        assertEquals(-1, instance.earliest());

        instance.add(1, new TestMessage());
        instance.add(2, new TestMessage());

        assertEquals(1, instance.earliest());
    }

    @Test
    public void testLatest() {
        assertEquals(-1, instance.latest());

        instance.add(0, new TestMessage());
        instance.add(1, new TestMessage());
        instance.add(2, new TestMessage());

        assertEquals(2, instance.latest());
    }

    /**
     * Add messages while simultaneously cleaning the cache; this should replicate the ConcurrentModificationExceptions
     * Tim was seeing when he would start two modules at the same time (MessageCache.clean() would throw when looping)
     *
     * @throws Exception
     */
    @Test
    public void testConcurrentModification() throws Exception {
        final int NUM_ADDED_MESSAGES = 10000;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch stopLatch = new CountDownLatch(1);

        Thread adder = new Thread(() -> {
            waitFor(startLatch, 1, TimeUnit.SECONDS);

            long id = 0;
            while (id < NUM_ADDED_MESSAGES) {
                LOGGER.trace("Adding to cache");
                instance.add(id++, new TestMessage());
            }

            stopLatch.countDown();
        });

        Thread cleaner = new Thread(() -> {
            waitFor(startLatch, 1, TimeUnit.SECONDS);

            while (stopLatch.getCount() != 0) {
                LOGGER.trace("Cleaning cache");
                instance.clean();
            }
        });

        Thread.UncaughtExceptionHandler handler = (t, e) -> {
            fail("Failed while adding and cleaning concurrently");
            LOGGER.error(e);
            startLatch.countDown();
        };

        adder.setUncaughtExceptionHandler(handler);
        cleaner.setUncaughtExceptionHandler(handler);
        adder.start();
        cleaner.start();

        startLatch.countDown();
        waitFor(stopLatch, 10, TimeUnit.SECONDS);
    }

    private void waitFor(CountDownLatch latch, long timeout, TimeUnit unit) {
        try {
            latch.await(timeout, unit);
        } catch (InterruptedException e) {
            LOGGER.error("Latch interrupted", e);
        }

        assertEquals(0, latch.getCount());
    }
}