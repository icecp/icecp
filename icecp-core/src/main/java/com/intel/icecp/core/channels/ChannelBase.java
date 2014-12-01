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
import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.event.Event;
import com.intel.icecp.core.event.EventObservable;
import com.intel.icecp.core.event.EventObserver;
import com.intel.icecp.core.pipeline.Pipeline;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base implementation of a channel; extend this in transport-specific sub-classes to simplify development.
 *
 * @param <T> the type of message allowed on this channel
 */
public abstract class ChannelBase<T extends Message> implements Channel<T>, EventObservable {

    /**
     * Contains the sequence of operations to be executed to format a message. This list can be modified according to
     * metadata
     */
    protected final Pipeline<T, InputStream> pipeline;
    private final URI name;
    private final Map<Class, List<EventObserver>> observers = new HashMap<>();

    /**
     * @param name the {@link Channel} name, including its scheme (e.g. "ndn://...").
     * @param formattingPipeline pipeline of operations to be executed for message encoding
     */
    public ChannelBase(URI name, Pipeline<T, InputStream> formattingPipeline) {
        this.name = name;
        this.pipeline = formattingPipeline;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(Class eventType, EventObserver observer) {
        if (!observers.containsKey(eventType)) {
            observers.put(eventType, new ArrayList<>());
        }
        observers.get(eventType).add(observer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregister(Class eventType, EventObserver observer) {
        if (observers.containsKey(eventType)) {
            observers.get(eventType).remove(observer);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasObserver(Class type) {
        return observers.containsKey(type);
    }

    /**
     * Notify applicable observers of a new {@link Event}
     *
     * @param event the {@link Event} to send
     */
    @Override
    public void notifyApplicableObservers(Event event) {
        Type eventType = event.getClass();
        if (observers.containsKey(eventType)) {
            for (EventObserver o : observers.get(eventType)) {
                o.notify(event);
            }
        }
    }

    /**
     * Equality check, based solely on name comparison; channels with different {@link Metadata} but the same name are
     * considered the same channel.
     *
     * @param obj the object to compare
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Channel && (obj == this || getName().equals(((Channel) obj).getName()));
    }

    /**
     * @return a hash code based on the channel name
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.name);
        return hash;
    }

    /**
     * @return a string representation of the {@link Channel}; i.e. the channel name
     */
    @Override
    public String toString() {
        return name.toString();
    }

    /**
     * This implementation currently throws an {@link UnsupportedOperationException}
     *
     * @param callback the callback to call
     */
    @Override
    public void onLatest(OnLatest<T> callback) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }
}
