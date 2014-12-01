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

/**
 * Maintains a limited cache of objects, replacing the older elements first. It relies on one user-level condition when
 * adding new items: the ID passed to {@link #add(long, Object)} must be greater than the latest ID held in the cache.
 * This is necessary to guarantee fast look up times (i.e. O(lg n)) when retrieving objects from the cache.
 * <p>
 * This class is thread-safe at a very coarse level: any methods that may retrieve or modify the state of the cache are
 * synchronized and cannot be interleaved.
 *
 */
public class CircularBufferCache<T> implements Cache<T> {

    private static final Logger LOGGER = LogManager.getLogger();
    private final long[] ids;
    private final T[] items;
    private int first;
    private int last;

    /**
     * Create a cache limited to the max size
     *
     * @param maxSize the max number of items to store in the cache
     */
    @SuppressWarnings("unchecked")
    public CircularBufferCache(int maxSize) {
        ids = new long[maxSize];
        items = (T[]) new Object[maxSize];
        first = -1;
        last = -1;
    }

    @Override
    public synchronized void add(long id, T item) {
        LOGGER.debug("Adding item with ID {}: {}", id, item);
        if (last != -1 && id <= ids[last])
            throw new IllegalArgumentException(String.format("The passed ID must be greater than the latest ID held in the cache: %d <= %d", id, ids[last]));

        // set marker locations
        last = (last + 1) % ids.length;
        if (last == first || first == -1) {
            first = (first + 1) % ids.length;
        }

        // store data
        ids[last] = id;
        items[last] = item;
    }

    @Override
    public synchronized boolean has(long id) {
        return !isEmpty() && (ids[first] == id || ids[last] == id || search(id) != -1);
    }

    @Override
    public synchronized long earliest() {
        return first == -1 ? -1 : ids[first];
    }

    @Override
    public synchronized long latest() {
        return last == -1 ? -1 : ids[last];
    }

    @Override
    public synchronized T get(long id) {
        LOGGER.debug("Retrieving ID {}", id);
        int index = search(id);
        return index == -1 ? null : items[index];
    }

    @Override
    public synchronized T remove(long id) {
        LOGGER.debug("Removing ID {}", id);
        int index = search(id);
        T removed = index == -1 ? null : items[index];
        items[index] = null;
        return removed;
    }

    @Override
    public synchronized int size() {
        if (last == -1) return 0;
        int diff = last - first + 1;
        return diff > 0 ? diff : ids.length + diff;
    }

    /**
     * @return true if the cache is empty
     */
    private boolean isEmpty() {
        return last == -1;
    }

    /**
     * Search for the passed ID; uses a binary search over the shifted buffer to find the index of the ID
     *
     * @param id the ID to search for
     * @return the index of the ID or -1 if not found
     */
    private int search(long id) {
        if (isEmpty() || id > ids[last] || id < ids[first]) return -1;
        if (id == ids[last]) return last;
        if (id == ids[first]) return first;

        int previous = first;
        int size = size();
        int end = log(size);
        for (int i = 0; i < end; i++) {
            size /= 2;
            int compare = (previous + size) % ids.length;
            if (ids[compare] == id) return compare;
            else if (ids[compare] < id) previous += size;
        }

        LOGGER.debug("Could not find ID {}", id);
        return -1;
    }

    /**
     * See discussion at http://stackoverflow.com/a/3305710/3113580
     *
     * @param n the input number
     * @return the base-2 logarithm of n
     */
    private int log(int n) {
        if (n == 0)
            return 0; // or throw exception
        return 31 - Integer.numberOfLeadingZeros(n);
    }
}
