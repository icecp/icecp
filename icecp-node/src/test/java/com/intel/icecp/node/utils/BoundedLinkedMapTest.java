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