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

package com.intel.icecp.node.utils;

import com.intel.icecp.common.TestMessage;
import com.intel.icecp.core.Channel;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.mock.MockChannels;

import org.junit.Test;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 */
public class ChannelUtilsTest {
    @Test
    public void joinRelative() throws Exception {
        assertEquals("scheme:/a/b/c/d/e", ChannelUtils.join(URI.create("scheme:/a/b/c"), "d", "e").toString());
    }

    @Test
    public void joinToRoot() throws Exception {
        assertEquals("scheme:/a/b", ChannelUtils.join(URI.create("scheme:/"), "a", "b").toString());
    }

    @Test
    public void joinComponentWithSeparator() throws Exception {
        assertEquals("scheme:/a/b/c/d", ChannelUtils.join(URI.create("scheme:/a/b"), "c/d").toString());
    }

    @Test
    public void joinRootLevelComponent() throws Exception {
        assertEquals("scheme:/a/b/c", ChannelUtils.join(URI.create("scheme:/a/b"), "/c").toString());
    }

    @Test
    public void getResponseChannelUri() throws Exception {
        TestMessage a = TestMessage.buildRandom(3);
        URI uriA = ChannelUtils.getResponseChannelUri(URI.create("icecp:/base"), a);
        TestMessage b = TestMessage.buildRandom(3);
        URI uriB = ChannelUtils.getResponseChannelUri(URI.create("icecp:/base"), b);

        assertFalse(a.equals(b));
        assertFalse(uriA.equals(uriB));
    }

    @Test
    public void nextMessage() throws Exception {
        MockChannels channels = new MockChannels();
        Channel<TestMessage> channel = channels.openChannel(URI.create("/next/message"), TestMessage.class, Persistence.DEFAULT);

        CompletableFuture<TestMessage> future = ChannelUtils.nextMessage(channel);
        assertNull(future.getNow(null));

        TestMessage nextMessage = TestMessage.buildRandom(10);
        channel.publish(nextMessage);

        assertEquals(nextMessage, future.get());
    }


}