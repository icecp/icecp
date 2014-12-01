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
package com.intel.icecp.request_response;

import com.intel.icecp.core.Message;

/**
 * Event handler for handling message requests.
 *
 * @param <REQUEST> the {@link Message} type for the incoming request
 * @param <RESPONSE> the {@link Message} type for the outgoing response
 */
public interface OnRequest<REQUEST extends Message, RESPONSE extends Message> {

    /**
     * Event handler for handling message requests.
     *
     * @param request the incoming request
     * @return the outgoing response
     */
    RESPONSE onRequest(REQUEST request);
}
