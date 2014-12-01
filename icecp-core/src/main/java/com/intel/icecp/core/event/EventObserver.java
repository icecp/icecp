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
 * Generic interface for watching an {@link EventObservable} subject.
 *
 */
public interface EventObserver<T extends Event> {

    /**
     * Called when an {@link EventObservable} sends an {@link Event}.
     *
     * @param event the {@link Event} sent
     */
    void notify(T event);
}
