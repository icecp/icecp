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

/**
 */
public interface Cache<T> {

    /**
     * Add an item to the cache
     *
     * @param item the new item ID
     * @param item the item instance
     */
    void add(long id, T item);

    /**
     * @param id the item ID
     * @return true if the item exists
     */
    boolean has(long id);

    /**
     * @return the ID of the earliest inserted item
     */
    long earliest();

    /**
     * @return the ID of the latest inserted item
     */
    long latest();

    /**
     * @param id the unique identifier for an item
     * @return the found item or null
     */
    T get(long id);

    /**
     * @param id the item ID
     * @return the removed item or null if none was found
     */
    T remove(long id);

    /**
     * @return the number of items currently in the cache
     */
    int size();
}
