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
