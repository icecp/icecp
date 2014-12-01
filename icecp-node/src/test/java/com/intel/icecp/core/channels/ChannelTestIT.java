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
package com.intel.icecp.core.channels;

import com.intel.icecp.common.TestCounter;
import com.intel.icecp.common.TestMessage;
import com.intel.icecp.core.Channel;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.metadata.formats.CborFormat;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.node.pipeline.implementations.MessageFormattingPipeline;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Common integration test to apply to all channel implementations; to use this test, implement the {@code newInstance}
 * method returning the type of the channel in question.
 *
 */
public abstract class ChannelTestIT implements ChannelInstanceCreator {

    public static final Logger logger = LogManager.getLogger();
    protected static final Persistence PERSISTENCE = new Persistence(2000);
    protected static final Format FORMAT = new CborFormat<>(TestMessage.class);
    private static final String CHANNEL_NAME = "/test/channel";
    private static Pipeline PIPELINE = MessageFormattingPipeline.create(TestMessage.class, FORMAT);
    private static int timeOutValue = 1000;  //in milliseconds
    private static int MAX_TIMEOUT_TWEAKS = 10;
    private static int timeOutValueIncrementor = 10;
    private static boolean setUpIsDone = false;

    @Before
    public void testSetup() {
        // getNetworkTimeoutValue();
    }

    /*
     * This method determines what timeout value to use for the get(timeoutvalue) calls.
	 * The value is in milliseconds.  Once determined, it is stored and used in T2 below.
     */
    public void getNetworkTimeoutValue() {
        if (setUpIsDone) {
            return;
        }

        logger.info("B: Determine TimeoutValue");
        final Channel<TestMessage> channel = newInstance(CHANNEL_NAME + "/latency", PIPELINE, PERSISTENCE);
        try {
            channel.open().get();
            TestMessage message1 = TestMessage.buildRandom(10);
            channel.publish(message1);
            // TODO diagnose why NDN channel subscribe is not ready for a publish
            //Thread.sleep(500);
            Future<TestMessage> latest = channel.latest();

            for (int numTweaks = 0; numTweaks < MAX_TIMEOUT_TWEAKS; numTweaks++) {
                try {
                    latest.get(timeOutValue, TimeUnit.MILLISECONDS);
                    logger.info("B: Timeout Value: " + timeOutValue);
                    break;
                } catch (java.util.concurrent.TimeoutException toe) {
                    timeOutValue += timeOutValueIncrementor;
                    Thread.sleep(500);    //if you don't sleep, it never succeeds.
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("B: Error on get: " + e.getMessage());
                    break;
                }
            }
        } catch (Exception e2) {
            logger.error("B: Error: " + e2.getMessage());
        }

        try {
            channel.close();
        } catch (ChannelLifetimeException e) {
            logger.error("B: failed to close channel");
        }

        setUpIsDone = true;
        if (timeOutValue > MAX_TIMEOUT_TWEAKS * timeOutValueIncrementor) {
            logger.error("Failed to get a timeOutValue, set to max");
        }

        //Add some "fudge".  The timeout value might be right on the edge and only
        //work some of the time.  So just double it, to give it some breathing room.
        timeOutValue *= 2;
    }

    @Test
    public void testPublishAndSubscribe() throws Exception {
        logger.info("T1: TestPubSub");
        final Channel<TestMessage> channelA = newInstance(CHANNEL_NAME, PIPELINE, PERSISTENCE);
        logger.info("T1: Created ChannelA: " + channelA.getName().toString());
        channelA.open().get();
        final Channel<TestMessage> channelB = newInstance(CHANNEL_NAME, PIPELINE, PERSISTENCE);
        logger.info("T1: Created ChannelB: " + channelA.getName().toString());
        channelB.open().get();
        final TestCounter counter = new TestCounter();

        channelA.subscribe(new OnPublish<TestMessage>() {
            @Override
            public void onPublish(TestMessage publishedObject) {
                logger.info("T1: Channel A received message.");
                counter.count++;
            }
        });
        logger.info("T1: Channel A is subscribed.");
        assertEquals(0, counter.count);

        channelB.publish(TestMessage.buildRandom(20));
        logger.info("T1: Channel B has published.");

        waitForCounterAtMost(counter, 2000);
        assertEquals(1, counter.count);

        channelA.close();
        channelB.close();
    }

    @Test
    public void testLatest() throws Exception {
        logger.info("T2: Test Latest");
        final Channel<TestMessage> channel = newInstance(CHANNEL_NAME + "/T2", PIPELINE, PERSISTENCE);
        logger.info("T2: Created Channel: " + channel.getName().toString());
        channel.open().get();

        TestMessage message1 = TestMessage.buildRandom(10);
        channel.publish(message1);
        logger.info("T2: Channel has published message 1.");

        TestMessage message2 = TestMessage.buildRandom(20);
        channel.publish(message2);
        logger.info("T2: Channel has published message 2.");

        TestMessage message3 = TestMessage.buildRandom(30);
        channel.publish(message3);
        logger.info("T2: Channel has published message 3.");

        Future<TestMessage> latest = channel.latest();
        boolean passed = false;
        for (int retryCount = 0; retryCount < 10; retryCount++) {
            try {
                latest.get(timeOutValue, TimeUnit.MILLISECONDS);
                logger.info("T2: Latest message retrieved, timeOutValue=" + timeOutValue);
                assertEquals(message3.a, latest.get().a);
                passed = true;
                break;
            } catch (java.util.concurrent.TimeoutException toe) {
                logger.info("T2: TimeOutError, try again...");
            }
        }
        channel.close();

        if (!passed) {
            fail("T2: Timeout error getting latest message");
        }

    }

    @Test
    public void testOnSameInstance() throws Exception {
        logger.info("T3: Pub Sub to your self");
        final Channel<TestMessage> channel = newInstance(CHANNEL_NAME + "/T3", PIPELINE, PERSISTENCE);
        logger.info("T3: Created Channel: " + channel.getName().toString());
        channel.open().get();

        final TestCounter counter = new TestCounter();
        channel.subscribe(publishedObject -> {
            logger.info("T3: Channel received message.");
            counter.count++;
        });
        logger.info("T3: Channel is subscribed to itself.");
        assertEquals(0, counter.count);

        channel.publish(TestMessage.build("a", 1.0, (int) System.currentTimeMillis(), true));
        logger.info("T3: Channel has published to itself.");

        waitForCounterAtMost(counter, 4000);
        assertEquals(1, counter.count);

        channel.close();
    }

    @Ignore // TODO see note below about sequencing
    @Test
    public void publishToEachOther() throws Exception {
        Channel<TestMessage> channelA = newInstance(CHANNEL_NAME + "/publish-to-each-other", PIPELINE, PERSISTENCE);
        CompletableFuture openA = channelA.open();
        Channel<TestMessage> channelB = newInstance(CHANNEL_NAME + "/publish-to-each-other", PIPELINE, PERSISTENCE);
        CompletableFuture openB = channelB.open();
        openA.get();
        openB.get();

        TestCounter counter = new TestCounter();

        Subscriber subscriberA = new Subscriber("SubscriberA", counter);
        channelA.subscribe(subscriberA);
        logger.info("Subscriber A subscribed");

        Subscriber subscriberB = new Subscriber("SubscriberB", counter);
        channelA.subscribe(subscriberB);
        logger.info("Subscriber B subscribed");

        TestMessage message = TestMessage.buildRandom(10);
        channelA.publish(message);
        logger.info("Channel A published");

        // TODO fix this in NDN: at this point channel B will also publish a message with sequence 0,
        // the same as what channel A just published, and the channel may not recognize
        // it as a new message
        channelB.publish(message);
        logger.info("Channel B published");

        waitForCounterAtMost(counter, 4, 10000);
        assertEquals(2, subscriberA.receivedMessages);
        assertEquals(2, subscriberB.receivedMessages);
    }

    private void waitForCounterAtMost(TestCounter counter, int milliseconds) throws InterruptedException {
        waitForCounterAtMost(counter, 1, milliseconds);
    }

    protected void waitForCounterAtMost(TestCounter counter, int toCount, int milliseconds) throws InterruptedException {
        long endTime = System.currentTimeMillis() + milliseconds;
        while (System.currentTimeMillis() < endTime && counter.count < toCount) {
            Thread.sleep(30);
        }
    }

    private class Subscriber implements OnPublish<TestMessage> {

        public final String name;
        final TestCounter counter;
        int receivedMessages = 0;

        Subscriber(String name, TestCounter counter) {
            this.name = name;
            this.counter = counter;
        }

        @Override
        public void onPublish(TestMessage publishedObject) {
            counter.count++;
            receivedMessages++;
            logger.info(String.format("%s received message, count=%d", name, receivedMessages));
        }
    }
}
