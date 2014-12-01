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

package com.intel.icecp.core.attributes;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Module;
import com.intel.icecp.core.Node;
import com.intel.icecp.core.channels.OnLatest;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.misc.OnPublish;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * @deprecated this is only present to support {@link Module#run(Node, Configuration, Channel, long)}; it will be
 * removed in 0.11.*; TODO remove this
 */
public class StateChannelAdapter implements Channel<Module.State> {
    private Attributes attributes;
    private boolean isPublishing = false;
    private boolean isSubscribing = false;

    public StateChannelAdapter(Attributes attributes) {
        this.attributes = attributes;
    }

    @Override
    public URI getName() {
        throw new UnsupportedOperationException("getName() should not be called; this class is deprecated and will be removed.");
    }

    @Override
    public CompletableFuture<Void> open() throws ChannelLifetimeException {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() throws ChannelLifetimeException {
        throw new ChannelLifetimeException("Cannot close this channel, it is a local adapter with no network communication", new UnsupportedOperationException("This class is deprecated and will be removed."));
    }

    @Override
    public void publish(Module.State message) throws ChannelIOException {
        if (!attributes.has(ModuleStateAttribute.class)) {
            try {
                attributes.add(new ModuleStateAttribute());
            } catch (AttributeRegistrationException e) {
                throw new RuntimeException("StateChannelAdapter publish failed to register state attribute", e);
            }
        }

        try {
            attributes.set(ModuleStateAttribute.class, message);
            this.isPublishing = true;
        } catch (AttributeNotFoundException | AttributeNotWriteableException e) {
            throw new ChannelIOException(e);
        }
    }

    @Override
    public boolean isPublishing() {
        return this.isPublishing;
    }

    @Override
    public void subscribe(OnPublish<Module.State> callback) throws ChannelIOException {
        OnAttributeChanged<Module.State> callbackAdapter = (name1, oldValue, newValue) -> callback.onPublish(newValue);
        attributes.observe(ModuleStateAttribute.NAME, callbackAdapter);
        this.isSubscribing = true;
    }

    @Override
    public boolean isSubscribing() {
        return this.isSubscribing;
    }

    @Override
    public CompletableFuture<Module.State> latest() throws ChannelIOException {
        CompletableFuture<Module.State> future = new CompletableFuture<>();
        try {
            future.complete(attributes.get(ModuleStateAttribute.class));
        } catch (AttributeNotFoundException e) {
            throw new ChannelIOException(e);
        }
        return future;
    }

    @Override
    public void onLatest(OnLatest<Module.State> callback) {
        throw new UnsupportedOperationException("onLatest() should not be called; this class is deprecated and will be removed.");
    }
}