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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Linked hash map exposing the earliest and latest entries added; it is bounded to a configurable size to save memory.
 * When benchmarked against CircularBufferCache, this class had slower insertions but faster reads and was therefore
 * retained for use.
 * <p>
 * It limits the amount of memory by replacing the oldest added element; this involves overriding the LinkedHashMap
 * implementation's removeEldestEntry() method; see the <a href="https://docs.oracle.com/javase/8/docs/api/java/util/LinkedHashMap.html#removeEldestEntry-java.util.Map.Entry-">Javadoc
 * entry</a> for more information.
 * <p>
 * Additionally, we implemented Josh Bloch's item 16 of Effective Java so that calls are forwarded to the underlying
 * LinkedHashMap. This allows us to decorate with some custom behavior and synchronize as we need.
 * <p>
 * This class is coarsely thread-safe; every public method is synchronized for one-at-a-time access to the underlying
 * map.
 *
 */
public class BoundedLinkedMap<K, V> implements Map<K, V> {
    private final LinkedHashMap<K, V> map;
    private final int maxSize;
    private K latest;

    /**
     * @param maxSize the maximum allowed number of records to store
     */
    public BoundedLinkedMap(int maxSize) {
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<K, V>(this.maxSize) {
            @Override
            public boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxSize;
            }
        };
    }

    /**
     * @return the earliest key added to this set or null if none are added
     */
    public synchronized K earliest() {
        for (K key : map.keySet()) {
            return key; // the LinkedHashMap guarantees iteration in order of insertion
        }
        return null;
    }

    /**
     * @return the latest key added to this set or null if none are added
     */
    public synchronized K latest() {
        return latest;
    }

    @Override
    public synchronized V put(K key, V value) {
        latest = key;
        return map.put(key, value);
    }

    @Override
    public synchronized int size() {
        return map.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public synchronized boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public synchronized V get(Object key) {
        return map.get(key);
    }


    @Override
    public synchronized V remove(Object key) {
        V value = map.remove(key);
        if (key == latest) latest = findLatest(map);
        return value;
    }

    @Override
    public synchronized void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
        latest = findLatest(map);
    }

    @Override
    public synchronized void clear() {
        map.clear();
        latest = null;
    }

    @Override
    public synchronized Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public synchronized Collection<V> values() {
        return map.values();
    }

    @Override
    public synchronized Set<java.util.Map.Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public String toString() {
        return map.toString();
    }

    /**
     * To find the latest key in a LinkedHashMap, iterate and return the last one found. The LinkedHashMap guarantees
     * iteration in order of insertion
     *
     * @param m the map to inspect
     * @return the latest key added to the map
     */
    private K findLatest(LinkedHashMap<K, V> m) {
        K newLatest = null;
        for (K key : m.keySet()) {
            newLatest = key;
        }
        return newLatest;
    }
}
