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

import com.intel.icecp.common.TestMessage;
import com.intel.icecp.core.Channel;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.node.pipeline.implementations.MessageFormattingPipeline;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Common test to apply to all channel implementations; to use this test,
 * implement the {@link #newInstance(String, Pipeline, Persistence)} method
 * returning the type of the channel in question.
 *
 */
public abstract class ChannelTest implements ChannelInstanceCreator {

    public static final Logger logger = LogManager.getLogger();
    public static final String CHANNEL_NAME = "/test/channel";
    public static final Format FORMAT = new JsonFormat<>(TestMessage.class);
    private static final int CHANNEL_PERSISTENCE_MS = 1000;
    public static final Persistence PERSISTENCE = new Persistence(CHANNEL_PERSISTENCE_MS);
    public static Pipeline PIPELINE = MessageFormattingPipeline.create(TestMessage.class, FORMAT);
    private final Channel instance;

    public ChannelTest() {
        instance = newInstance(CHANNEL_NAME, PIPELINE, PERSISTENCE);
    }

    @Test
    public void testGetName() {
        assertNotNull(instance.getName());
    }

    @Test
    public void testThatItOpens() throws ChannelLifetimeException {
        instance.open();
    }

    @Test
    public void testThatItCloses() throws ChannelLifetimeException {
        instance.close();
    }

    @Test
    public void testPublish_GenericType() throws Exception {
        instance.publish(TestMessage.buildRandom(50));
        instance.publish(TestMessage.buildRandom(500));
        instance.publish(TestMessage.buildRandom(5000));
        instance.publish(TestMessage.buildRandom(50000));
    }

    @Test(expected = TimeoutException.class)
    public void testGetLatestMethodFailure() throws Exception {
        instance.latest().get(10, TimeUnit.MILLISECONDS);
    }

    @Test(expected = ChannelLifetimeException.class)
    public void testCannotReopenChannelWhenCloseIsScheduled() throws Exception {
        instance.publish(TestMessage.buildRandom(50));
        instance.close();
        instance.open();
        fail("'open' should have thrown a ChannelLifetimeException because the channel is scheduled to close");
    }

    @Test
    public void testCanReopenClosedChannel() throws Exception {
        instance.publish(TestMessage.buildRandom(50));
        instance.close();
        Thread.sleep((long) (1.2 * CHANNEL_PERSISTENCE_MS));  // Wait for message persistence to expire before reopening channel
        instance.open();
    }
}
