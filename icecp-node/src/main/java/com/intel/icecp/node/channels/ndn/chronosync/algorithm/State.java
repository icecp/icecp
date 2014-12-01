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

package com.intel.icecp.node.channels.ndn.chronosync.algorithm;

/**
 * An object representing a value in time. States are matchable (see {@link #matches(State)})
 * so that we can determine which states can be compared against each other; states are comparable so that we can
 * determine which instance is the latest/newest/greatest (and likely should be retained).
 *
 */
public interface State extends Comparable<State> {

    /**
     * Check if a state matches another state. States may change over time: e.g. the state of a counter may increment by
     * one. In this case, we want to understand that the new state instance (e.g. counter == 1) matches the previous one
     * (e.g. counter == 0) and should replace it. The same Java instance may be used for both states and this method
     * then must return true.
     *
     * @param other another state
     * @return true if the states match
     */
    boolean matches(State other);

    /**
     * @return the bytes representing this state
     */
    byte[] toBytes();
}
