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
import java.net.URI;

/**
 * This interface represents receiving a message over a channel, and resonding
 * to it.
 *
 * This interface is one half of the {@link Requestor}/{@link Responder}
 * interfaces for channels.
 *
 *
 * @param <REQUEST> the {@link Message} type to use for the request
 * @param <RESPONSE> the {@link Message} type returned from the request
 */
public interface Responder<REQUEST extends Message, RESPONSE extends Message> {

    void listen(URI requestUri, OnRequest<REQUEST, RESPONSE> callback) throws Exception;

}
