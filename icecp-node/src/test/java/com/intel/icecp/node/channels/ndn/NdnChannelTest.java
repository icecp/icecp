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

package com.intel.icecp.node.channels.ndn;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.channels.ChannelTest;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.node.NodeFactory;
import com.intel.jndn.mock.MockFace;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Test NdnNotificationChannel; all channel types should be able to pass the tests defined
 * in {@link ChannelTest}
 *
 */
public class NdnChannelTest extends ChannelTest {

    @Override
    public Channel newInstance(String channelName, Pipeline op, Persistence persistence) {
        MockFace face = setupMockFace();
        ScheduledExecutorService eventLoop = NodeFactory.buildEventLoop();
        NdnChannelProvider builder = new NdnChannelProvider("/test/identity", face, face, eventLoop);

        try {
            return builder.build(URI.create("ndn:" + channelName), op, persistence);
        } catch (ChannelLifetimeException ex) {
            throw new IllegalArgumentException("Unable to create channel instance.", ex);
        }
    }

    private MockFace setupMockFace() {
        return new MockFace();
    }

    @Test(expected = TimeoutException.class)
    public void noOnLatestShouldFail() throws Exception {
        Channel instance = newInstance(CHANNEL_NAME, PIPELINE, new Persistence(10));
        instance.latest().get(11, TimeUnit.MILLISECONDS);
    }
}
