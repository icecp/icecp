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

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.misc.ChannelIOException;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @param <T>
 */
public interface HistoricalChannel<T extends Message> extends Channel<T> {

    /**
     * Retrieve the earliest message available on this channel. The channel
     * serializes the message using the channel's Format so that it can be
     * deserialized by any other device. Access will be controlled with the
     * ChannelPermission grant; ChannelPermission must control what channels are
     * allowed by the caller (e.g. new ChannelPermission("subscribe",
     * "/some/prefix/*")).
     *
     * @return the latest {@link com.intel.icecp.core.Message} published on this
     * channel
     * @throws ChannelIOException when the channel fails to receive
     * {@link Message}s
     */
    CompletableFuture<T> earliest() throws ChannelIOException;

    /**
     * Retrieve a specific message from the channel. TODO think this through...
     *
     * @param id the {@link Message} ID
     * @return the latest {@link com.intel.icecp.core.Message} published on this
     * channel
     * @throws ChannelIOException when the channel fails to receive
     * {@link Message}s
     */
    CompletableFuture<T> get(long id) throws ChannelIOException;
}
