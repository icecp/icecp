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
package com.intel.icecp.core.event;

import com.intel.icecp.core.misc.OnPublish;
import java.net.URI;

/**
 * Defines an event service; all triggered events should go through this service
 * so that a consistent implementation is available for all event messaging
 * (e.g. publish to channel vs in-memory only)
 *
 */
public interface Events {

    /**
     * Notify subscribers that an event has occurred
     *
     * @param event the event to trigger
     */
    void notify(Event event);

    /**
     * Listen to all events
     *
     * @param callback callback fired when an event is triggered
     */
    void listen(OnPublish<Event> callback);

    /**
     * Listen to a specific event type
     *
     * @param <T>
     * @param suffix TODO remove and figure this out from type
     * @param type the type of the event to listen for
     * @param callback callback fired when an event is triggered
     */
    <T extends Event> void listen(URI suffix, Class<T> type, OnPublish<T> callback);
}
