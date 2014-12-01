/*
 * ******************************************************************************
 *
 * INTEL CONFIDENTIAL
 *
 * Copyright 2013 - 2016 Intel Corporation All Rights Reserved.
 *
 * The source code contained or described herein and all documents related to
 * the source code ("Material") are owned by Intel Corporation or its suppliers
 * or licensors. Title to the Material remains with Intel Corporation or its
 * suppliers and licensors. The Material contains trade secrets and proprietary
 * and confidential information of Intel or its suppliers and licensors. The
 * Material is protected by worldwide copyright and trade secret laws and treaty
 * provisions. No part of the Material may be used, copied, reproduced,
 * modified, published, uploaded, posted, transmitted, distributed, or disclosed
 * in any way without Intel's prior express written permission.
 *
 * No license under any patent, copyright, trade secret or other intellectual
 * property right is granted to or conferred upon you by disclosure or delivery
 * of the Materials, either expressly, by implication, inducement, estoppel or
 * otherwise. Any license under such intellectual property rights must be
 * express and approved by Intel in writing.
 *
 * Unless otherwise agreed by Intel in writing, you may not remove or alter this
 * notice or any other notice embedded in Materials by Intel or Intel's
 * suppliers or licensors in any way.
 *
 * ******************************************************************************
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