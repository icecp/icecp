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

package com.intel.icecp.core.channels;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.attributes.Attributes;

/**
 * Allow dynamic generation of messages when a channel is queried for its latest message
 *
 * @param <T> the type of message allowed by this callback
 */
public interface OnLatest<T extends Message> {

    /**
     * @return the message to send to the requestor or null to use default latest message
     */
    Response<T> onLatest();

    /**
     * Data structure for returning generated messages and their attributes as a response to an {@link OnLatest}
     * request
     *
     * @param <T> the specific type of message
     */
    class Response<T extends Message> {
        /**
         * The generated message
         */
        public final T message;

        /**
         * The attributes of the generated message
         */
        public final Attributes attributes;

        public Response(T message, Attributes attributes) {
            this.message = message;
            this.attributes = attributes;
        }

        public Response(T message) {
            this(message, null);
        }
    }
}
