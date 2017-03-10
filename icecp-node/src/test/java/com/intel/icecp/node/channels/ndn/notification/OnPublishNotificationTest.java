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
package com.intel.icecp.node.channels.ndn.notification;

import com.intel.icecp.common.TestMessage;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.node.channels.ndn.NdnNotificationChannel;
import com.intel.icecp.node.NodeFactory;
import com.intel.jndn.mock.MockFace;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Test OnPublishNotification
 *
 */
public class OnPublishNotificationTest {

    @Test
    public void testThatCallbackExceptionsAreCaught() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        assertEquals(1, latch.getCount());

        OnPublishNotification instance = new OnPublishNotification(new MockNdnNotificationChannel(), new ExceptionThrowerCallback(), new ExceptionHandlerCallback(latch));
        final Name name = new Name("/dummy/channel").appendVersion(0);
        final Interest incomingInterest = new Interest(name);
        instance.onInterest(name, incomingInterest, new MockFace(), 0, null);

        latch.await(1, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    /**
     * Mock the NdnNotificationChannel interaction, returning a test message immediately
     */
    private class MockNdnNotificationChannel extends NdnNotificationChannel {

        public MockNdnNotificationChannel() {
            super(URI.create("ndn:/dummy/channel"), null, null, null, NodeFactory.buildEventLoop(), new Persistence());
        }

        @Override
        public CompletableFuture get(long version) {
            return CompletableFuture.completedFuture(TestMessage.buildRandom(10));
        }
    }

    /**
     * Throw a runtime exception once the message is received
     */
    private class ExceptionThrowerCallback implements OnPublish {

        @Override
        public void onPublish(Message message) {
            throw new RuntimeException();
        }
    }

    /**
     * Decrement the countdown latch when called
     */
    private class ExceptionHandlerCallback implements OnPublishNotification.OnCallbackFailure {

        private final CountDownLatch latch;

        public ExceptionHandlerCallback(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onCallbackFailure(Throwable throwable) {
            latch.countDown();
        }
    }
}
