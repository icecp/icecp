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

package com.intel.icecp.node.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test BoundedLinkedMapTest
 *
 */
public class BoundedLinkedMapTest {

    private static final Logger LOGGER = LogManager.getLogger();
    private BoundedLinkedMap<String, Object> instance;

    @Before
    public void beforeTest() {
        instance = new BoundedLinkedMap<>(2);
    }

    @Test
    public void testUsage() {
        Object object0 = new Object();
        Object object1 = new Object();
        Object object2 = new Object();

        instance.put("0", object0);
        assertEquals(1, instance.size());

        instance.put("1", object1);
        assertEquals(2, instance.size());

        instance.put("2", object2);
        assertEquals(2, instance.size());

        assertNull(instance.get("0"));
        assertEquals("2", instance.latest());
    }

    @Test
    public void testEarliestLatest() {
        assertNull(instance.earliest());
        assertNull(instance.latest());

        instance.put(".", new Object());
        assertEquals(instance.earliest(), instance.latest());

        instance.put("..", new Object());
        assertEquals(".", instance.earliest());
        assertEquals("..", instance.latest());

        instance.put("...", new Object());
        assertEquals("..", instance.earliest());
        assertEquals("...", instance.latest());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(instance.isEmpty());
        instance.put("...", new Object());
        assertFalse(instance.isEmpty());
    }

    @Test
    public void testContainsKey() {
        assertFalse(instance.containsKey("..."));
        instance.put("...", new Object());
        assertTrue(instance.containsKey("..."));
    }

    @Test
    public void testContainsValue() {
        Object o = new Object();
        assertFalse(instance.containsValue(o));
        instance.put("...", o);
        assertTrue(instance.containsValue(o));
    }

    @Test
    public void testRemove() {
        Object o = new Object();
        String key = "...";

        instance.put(key, o);
        assertTrue(instance.containsKey(key));
        assertTrue(instance.containsValue(o));

        instance.remove(key);
        assertFalse(instance.containsKey(key));
        assertFalse(instance.containsValue(o));
        assertEquals(0, instance.size());
    }

    @Test
    public void testPutAll() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("1", new Object());
        map.put("2", new Object());
        map.put("99", new Object());

        instance.putAll(map);
        LOGGER.debug("Map passed to putAll(): {}", map.toString());
        LOGGER.debug("Resulting bounded map after putAll(): {}", instance.toString());

        assertEquals(2, instance.size()); // note: this is not 3 because the max size is bounded
        assertEquals("2", instance.latest()); // note: put all doesn't do the FIFO replacement
    }

    @Test
    public void testClear() {
        instance.put("...", new Object());

        instance.clear();

        assertEquals(0, instance.size());
        assertNull(instance.get("..."));
        assertNull(instance.latest());
        assertNull(instance.earliest());
    }

    @Test
    public void testConversions() {
        instance.put("...", new Object());

        assertEquals(1, instance.keySet().size());
        assertEquals(1, instance.values().size());
        assertEquals(1, instance.entrySet().size());
    }

    @Test
    public void testPerformanceAgainstArrayList() {
        int numMessages = 10000;
        BoundedLinkedMap<Integer, Object> map = new BoundedLinkedMap<>(numMessages);
        ArrayList<Object> list = new ArrayList<>(numMessages);

        long mapPutTime = measure(numMessages, i -> map.put(i, new Object()));
        long listPutTime = measure(numMessages, i -> list.add(i, new Object()));
        LOGGER.info("Custom map put has overhead of {}% versus list put", toPercent((mapPutTime - listPutTime) / (double) listPutTime));

        long mapGetTime = measure(numMessages, map::get);
        long listGetTime = measure(numMessages, list::get);
        LOGGER.info("Custom map get has overhead of {}% versus list get", toPercent((mapGetTime - listGetTime) / (double) listPutTime));
    }

    private long measure(int numTimes, Consumer<Integer> work) {
        long start = System.nanoTime();
        for (int i = 0; i < numTimes; i++) {
            work.accept(i);
        }
        return System.nanoTime() - start;
    }

    private double toPercent(double number) {
        return Math.round(number * 100);
    }
}