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
package com.intel.icecp.node.channels.local;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.channels.ChannelBase;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.core.pipeline.Pipeline;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 *
 */
public class LocalChannel extends ChannelBase {

    private boolean opened = false;

    protected LocalChannel(URI name, Pipeline pipeline) {
        super(name, pipeline);
    }

    @Override
    public CompletableFuture open() throws ChannelLifetimeException {
        opened = true;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isOpen() {
        return opened;
    }

    @Override
    public void close() throws ChannelLifetimeException {
        opened = false;
    }

    @Override
    public void publish(Message message) throws ChannelIOException {

    }

    @Override
    public boolean isPublishing() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void subscribe(OnPublish callback) throws ChannelIOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isSubscribing() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public CompletableFuture latest() throws ChannelIOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
