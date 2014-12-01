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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reference implementation for a multi-event observable subject; currently
 * unused (each observable will likely re-implement this since Java allows only
 * descending from one class--and this will likely not be that class).
 *
 */
public class EventObservableImpl implements EventObservable {

    private final Map<Class, List<EventObserver>> observers = new HashMap<>();

    @Override
    public void register(Class eventType, EventObserver observer) {
        if (!observers.containsKey(eventType)) {
            observers.put(eventType, new ArrayList<EventObserver>());
        }
        observers.get(eventType).add(observer);
    }

    @Override
    public void unregister(Class eventType, EventObserver observer) {
        if (observers.containsKey(eventType)) {
            observers.get(eventType).remove(observer);
        }
    }

    @Override
    public boolean hasObserver(Class type) {
        return observers.containsKey(type);
    }

    @Override
    public void notifyApplicableObservers(Event event) {
        Class eventType = event.getClass();
        if (observers.containsKey(eventType)) {
            for (EventObserver o : observers.get(eventType)) {
                o.notify(event);
            }
        }
    }
}
