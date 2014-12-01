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
