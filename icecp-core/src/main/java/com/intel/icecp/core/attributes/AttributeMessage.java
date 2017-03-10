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

package com.intel.icecp.core.attributes;

import com.intel.icecp.core.Message;

/**
 * Wrapper message for transporting attribute values over channels.
 *
 */
public class AttributeMessage<T> implements Message {

    /**
     * The attribute data
     */
    public final T d;

    /**
     * The timestamp the attribute data was collected
     */
    public final long ts;

    /**
     * Default constructor; necessary for some serialization libraries
     */
    public AttributeMessage() {
        this.d = null;
        this.ts = 0;
    }

    /**
     * Create an attribute message with the current system time as the timestamp
     *
     * @param d the attribute value
     */
    public AttributeMessage(T d) {
        this.d = d;
        this.ts = System.currentTimeMillis();
    }
}
