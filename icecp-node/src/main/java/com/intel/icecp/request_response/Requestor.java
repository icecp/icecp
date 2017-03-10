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
import java.util.concurrent.CompletableFuture;

/**
 * This interface represents making a request over a channel. The returned
 * Future can be used synchronously or asynchronously to get the response.
 *
 * This interface is one half of the {@link Requestor}/{@link Responder}
 * interfaces for channels.
 *
 *
 * @param <REQUEST> the {@link Message} type to use for the request
 * @param <RESPONSE> the {@link Message} type returned from the request
 */
public interface Requestor<REQUEST extends Message, RESPONSE extends Message> {

    CompletableFuture<RESPONSE> request(URI uri, REQUEST message) throws Exception;
}
