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
