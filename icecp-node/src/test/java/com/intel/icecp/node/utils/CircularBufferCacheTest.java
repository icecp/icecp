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

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 */
public class CircularBufferCacheTest {

    private static final Logger LOGGER = LogManager.getLogger();
    private CircularBufferCache<Object> instance;
    private static final int MAX_CACHE_SIZE = 10000;
    private static final int TOTAL_INSERTIONS = 100000;

    @Before
    public void before() {
        instance = new CircularBufferCache<>(2);
    }

    @Test
    public void add() throws Exception {
        Object object0 = new Object();
        Object object1 = new Object();
        Object object2 = new Object();

        instance.add(1, object0);
        assertEquals(1, instance.size());

        instance.add(3, object1);
        assertEquals(2, instance.size());

        instance.add(5, object2);
        assertEquals(2, instance.size());

        assertNull(instance.get(99));
        assertEquals(5, instance.latest());
        assertEquals(object1, instance.get(3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addingNonIncrementingId() throws Exception {
        instance.add(4, new Object());
        instance.add(3, new Object());
    }

    @Test
    public void has() throws Exception {
        assertFalse(instance.has(99));
        instance.add(0, new Object());
        assertEquals(0, instance.earliest());
        instance.add(1, new Object());
        assertEquals(0, instance.earliest());
    }

    @Test
    public void earliest() throws Exception {
        assertEquals(-1, instance.earliest());
        instance.add(0, new Object());
        assertEquals(0, instance.earliest());
        instance.add(1, new Object());
        assertEquals(0, instance.earliest());
    }

    @Test
    public void latest() throws Exception {
        assertEquals(-1, instance.latest());
        instance.add(0, new Object());
        assertEquals(0, instance.latest());
        instance.add(1, new Object());
        assertEquals(1, instance.latest());
    }

    @Test
    public void get() throws Exception {
        assertNull(instance.get(99));
        assertNull(instance.get(instance.latest()));

        Object item = new Object();
        instance.add(34, item);
        assertEquals(item, instance.get(34));
    }

    @Test
    public void getFromWrappedArray() throws Exception {
        CircularBufferCache<Integer> cache = new CircularBufferCache<>(5);
        for(int i = 1; i < 8; i++){
            cache.add(i, i);
        }

        assertNull(cache.get(1));
        assertNull(cache.get(2));
        assertEquals(3, (int) cache.get(3));
        assertEquals(4, (int) cache.get(4));
        assertEquals(5, (int) cache.get(5));
        assertEquals(6, (int) cache.get(6));
        assertEquals(7, (int) cache.get(7));
    }

    @Test
    public void remove() throws Exception {
        instance.add(11, new Object());
        assertNotNull(instance.get(11));
        instance.remove(11);
        assertNull(instance.get(11));
    }

    @Test
    public void benchmarkHashMapInsertion(){
        TimedLoop<HashMap<Integer, Object>> loop = new TimedLoop<>(() -> new HashMap<>(MAX_CACHE_SIZE), (i, t) -> t.put(i, new Object()));
        LOGGER.info("Inserted {} items into an initially {}-sized hash map in (ms): {}", TOTAL_INSERTIONS, MAX_CACHE_SIZE, loop.time(TOTAL_INSERTIONS));
    }

    @Test
    public void benchmarkHashMapRetrieval(){
        final HashMap<Integer, Object> hashMap = new HashMap<>(MAX_CACHE_SIZE);
        TimedLoop<HashMap<Integer, Object>> loop = new TimedLoop<>(() -> hashMap, (i, t) -> t.put(i, new Object()));
        LOGGER.info("Prepared {}-sized hash map in (ms): {}", MAX_CACHE_SIZE, MAX_CACHE_SIZE, loop.time(MAX_CACHE_SIZE));

        final Object[] x = {null};
        TimedLoop<HashMap<Integer, Object>> loop2 = new TimedLoop<>(() -> hashMap, (i, t) -> x[0] = t.get(i));
        LOGGER.info("Retrieved {} items from hash map in (ms): {}", MAX_CACHE_SIZE, loop2.time(MAX_CACHE_SIZE));
    }

    @Test
    public void benchmarkCacheInsertion(){
        TimedLoop<CircularBufferCache<Object>> loop = new TimedLoop<>(() -> new CircularBufferCache<>(MAX_CACHE_SIZE), (i, t) -> t.add(i, new Object()));
        LOGGER.info("Inserted {} items into a {}-sized cache in (ms): {}", TOTAL_INSERTIONS, MAX_CACHE_SIZE, loop.time(TOTAL_INSERTIONS));
    }

    @Test
    public void benchmarkCacheRetrieval(){
        final CircularBufferCache<Object> cache = new CircularBufferCache<>(MAX_CACHE_SIZE);
        TimedLoop<CircularBufferCache<Object>> loop = new TimedLoop<>(() -> cache, (i, t) -> t.add(i, new Object()));
        LOGGER.info("Prepared {}-sized cache in (ms): {}", MAX_CACHE_SIZE, MAX_CACHE_SIZE, loop.time(MAX_CACHE_SIZE));

        final Object[] x = {null};
        TimedLoop<CircularBufferCache<Object>> loop2 = new TimedLoop<>(() -> cache, (i, t) -> x[0] = t.get(i));
        LOGGER.info("Retrieved {} items from cache in (ms): {}", MAX_CACHE_SIZE, loop2.time(MAX_CACHE_SIZE));
    }

    @Test
    public void benchmarkBoundedLinkedMapInsertion(){
        TimedLoop<BoundedLinkedMap<Integer, Object>> loop = new TimedLoop<>(() -> new BoundedLinkedMap<>(MAX_CACHE_SIZE), (i, t) -> t.put(i, new Object()));
        LOGGER.info("Inserted {} items into a {}-sized bounded map in (ms): {}", TOTAL_INSERTIONS, MAX_CACHE_SIZE, loop.time(TOTAL_INSERTIONS));
    }

    @Test
    public void benchmarkBoundedLinkedMapRetrieval(){
        final BoundedLinkedMap<Integer, Object> boundedMap = new BoundedLinkedMap<>(MAX_CACHE_SIZE);
        TimedLoop<BoundedLinkedMap<Integer, Object>> loop = new TimedLoop<>(() -> boundedMap, (i, t) -> t.put(i, new Object()));
        LOGGER.info("Prepared {}-sized bounded map in (ms): {}", MAX_CACHE_SIZE, MAX_CACHE_SIZE, loop.time(MAX_CACHE_SIZE));

        final Object[] x = {null};
        TimedLoop<BoundedLinkedMap<Integer, Object>> loop2 = new TimedLoop<>(() -> boundedMap, (i, t) -> x[0] = t.get(i));
        LOGGER.info("Retrieved {} items from bounded map in (ms): {}", MAX_CACHE_SIZE, loop2.time(MAX_CACHE_SIZE));
    }

    /**
     * Helper class for timing
     * @param <T>
     */
    private class TimedLoop<T> {
        final Supplier<T> before;
        final BiConsumer<Integer, T> each;

        public TimedLoop(Supplier<T> before, BiConsumer<Integer, T> each) {
            this.before = before;
            this.each = each;
        }

        /**
         * Run {@link #each} repeatedly passing in the one-time result of {@link #before}.
         * @param iterations the number of times to run
         * @return the number of milliseconds elapsed
         */
        public long time(int iterations){
            T t = before.get();
            long startTime = System.nanoTime();
            for(int i = 0; i < iterations; i++){
                each.accept(i, t);
            }
            long endTime = System.nanoTime();
            return (endTime - startTime) / 1000000;
        }
    }
}