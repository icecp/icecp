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
package com.intel.icecp.node.channels.file;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.channels.ChannelProvider;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.pipeline.Pipeline;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 *
 */
public class FileChannelProvider implements ChannelProvider {

    private ScheduledExecutorService eventLoop = Executors.newScheduledThreadPool(2);
    public static final String SCHEME = "file";

    public FileChannelProvider() {
    }

    @Override
    public void start(ScheduledExecutorService pool, Configuration configuration) {
        this.eventLoop = pool;
    }

    @Override
    public void stop() {
        // do nothing for now
    }

    @Override
    public <T extends Message> Channel<T> build(URI uri, Pipeline pipeline, Persistence persistence, Metadata... metadata) throws ChannelLifetimeException {
        return new FileChannel(uri, pipeline, eventLoop);
    }

    @Override
    public String scheme() {
        return SCHEME;
    }

}
