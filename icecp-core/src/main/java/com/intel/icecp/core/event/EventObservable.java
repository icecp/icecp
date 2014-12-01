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

/**
 * Generic interface for registering multiple event types to one observable
 * subject.
 *
 */
public interface EventObservable {

    /**
     * @param eventType the type of the {@link Event}
     * @param observer the {@link EventObserver}
     */
    <T extends Event> void register(Class<T> eventType, EventObserver<T> observer);

    /**
     * @param eventType the type of the {@link Event}
     * @param observer the {@link EventObserver}
     */
    <T extends Event> void unregister(Class<T> eventType, EventObserver<T> observer);

    /**
     * @param eventType the type of the {@link Event}
     * @return true if the observed object has an observer registered for the
     * given {@link Event} type
     */
    boolean hasObserver(Class<? extends Event> eventType);

    /**
     * Notify all registered and applicable observers with the given
     * {@link Event}
     *
     * @param event
     */
    void notifyApplicableObservers(Event event);
}
