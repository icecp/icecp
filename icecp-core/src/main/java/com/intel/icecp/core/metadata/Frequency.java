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
package com.intel.icecp.core.metadata;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Metadata;

/**
 * Define the frequency for retrieving messages from a channel. This represents
 * a contract from the Channel that it must satisfy under non-error conditions.
 *
 */
public class Frequency implements Metadata {

    /**
     * Get the average interval, in milliseconds, that messages will be
     * available from a {@link Channel}; in NDN implementations, this may be
     * used as the polling frequency for a StreamChannel.
     */
    public final long average;

    /**
     * Get the maximum interval, in milliseconds, between messages from a
     * {@link Channel}. In NDN implementations, this may be used as an upper
     * bound for Interest timeouts.
     */
    public final long maximum;

    /**
     * Get the minimum interval, in milliseconds, between messages from a
     * {@link Channel}.
     */
    public final long minimum;

    /**
     * Default constructor
     *
     * @param minimum see {@link #minimum}
     * @param average see {@link #average}
     * @param maximum see {@link #maximum}
     */
    public Frequency(long minimum, long average, long maximum) {
        if (!(minimum <= average && average <= maximum)) {
            throw new IllegalArgumentException("Frequency must follow the following rule: minimum <= average <= maximum");
        }
        this.average = average;
        this.maximum = maximum;
        this.minimum = minimum;
    }

    /**
     * Set frequency to 1000 ms, +-500ms.
     */
    public Frequency() {
        this.average = 1000;
        this.maximum = 1500;
        this.minimum = 500;
    }
}
