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

package com.intel.icecp.node.channels.ndn;

import com.intel.icecp.common.TestCounter;
import com.intel.icecp.common.TestHelper;
import com.intel.icecp.common.TestMessage;
import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.channels.ChannelTestIT;
import com.intel.icecp.core.channels.OnLatest;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.node.NodeFactory;
import com.intel.icecp.node.pipeline.implementations.MessageFormattingPipeline;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Integration test NdnChannel; this test may fail if the strategy for the channel is not set to broadcast (set with
 * `nfdc set-strategy / /localhost/nfd/strategy/broadcast`) on the NFD used.
 *
 */
public class NdnChannelTestIT extends ChannelTestIT {

    @Override
    public Channel newInstance(String channelName, Pipeline pipeline, Persistence persistence) {
        ScheduledExecutorService eventLoop = NodeFactory.buildEventLoop();
        NdnChannelProvider builder = new NdnChannelProvider("/test/identity", TestHelper.getNfdHostName(), eventLoop);

        try {
            return builder.build(URI.create("ndn:" + channelName),
                    pipeline,
                    persistence);
        } catch (ChannelLifetimeException ex) {
            throw new IllegalArgumentException("Unable to create channel instance.", ex);
        }
    }

    private <T extends Message> Channel<T> openChannel(String channelName, Class<T> messageType) throws ChannelLifetimeException, InterruptedException, ExecutionException {
        final Channel<T> channel = newInstance(channelName, MessageFormattingPipeline.create(messageType, FORMAT),
                PERSISTENCE);
        channel.open().get();
        return channel;
    }

    @Test
    public void testThatItClosesButRetainsPersistentMessages() throws Exception {
        String randomName = "/test/closing/" + TestHelper.generateRandomString(20);
        final Channel<TestMessage> pub = openChannel(randomName, TestMessage.class);
        final Channel<TestMessage> sub = openChannel(randomName, TestMessage.class);
        final TestMessage m1 = TestMessage.buildRandom(50);

        pub.publish(m1);
        pub.close();

        TestMessage m2 = sub.latest().get(2, TimeUnit.SECONDS);
        assertEquals(m1, m2);
    }

    @Test
    public void testReplayScenario() throws Exception {
        String randomName = "/test/replay/" + TestHelper.generateRandomString(20);
        Channel<TestMessage> publisher1 = openChannel(randomName, TestMessage.class);
        Channel<TestMessage> publisher2 = openChannel(randomName, TestMessage.class);
        Channel<TestMessage> subscriber = openChannel(randomName, TestMessage.class);
        final TestMessage message1 = TestMessage.buildRandom(10);
        final TestMessage message2 = TestMessage.buildRandom(10);

        Semaphore lock = new Semaphore(1);
        lock.acquire();
        subscriber.subscribe(message -> {
            if (message.equals(message1)) {
                logger.info("Message 1 received");
            } else if (message.equals(message2)) {
                logger.info("Message 2 received");
            } else {
                fail("Unknown message received");
            }
            lock.release();
        });

        publisher1.publish(message1);
        publisher2.publish(message2);

        // block until the callback releases the lock
        lock.acquire();
    }

    @Test
    public void testThatClosedChannelsDoNotInterfere() throws Exception {
        String randomName = "/test/interference/" + TestHelper.generateRandomString(20);

        Channel<TestMessage> subscriber = openChannel(randomName, TestMessage.class);
        CountDownLatch latch = new CountDownLatch(3);
        subscriber.subscribe(message -> {
            String oldName = Thread.currentThread().getName();
            Thread.currentThread().setName("subscriber");
            logger.info("Received a message: " + message.c);
            latch.countDown();
            Thread.currentThread().setName(oldName);
        });

        for (int i = 0; i < 3; i++) {
            //pool.submit(new PublisherRunnable(randomName, i));
            (new PublisherRunnable(randomName, i)).run();
            Thread.sleep(2000); // TODO this test will fail if this sleep is reduced to half of the channel's persistence
        }

        latch.await(10, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    @Test
    public void useOnLatest() throws Exception {
        String randomName = "/test/on-latest/" + TestHelper.generateRandomString(20);
        final Channel<TestMessage> channelA = openChannel(randomName, TestMessage.class);
        final Channel<TestMessage> channelB = openChannel(randomName, TestMessage.class);

        TestMessage expected = TestMessage.buildRandom(10);
        channelA.onLatest(() -> new OnLatest.Response<>(expected));

        TestMessage retrieved = channelB.latest().get(PERSISTENCE.persistFor + 1, TimeUnit.MILLISECONDS);
        assertEquals(expected, retrieved);
    }

    @Test
    public void useOnLatestMultipleTimes() throws Exception {
        String randomName = "/test/on-latest/" + TestHelper.generateRandomString(20);
        final Channel<TestMessage> channelA = openChannel(randomName, TestMessage.class);
        final Channel<TestMessage> channelB = openChannel(randomName, TestMessage.class);

        TestMessage first = TestMessage.buildRandom(10);
        channelA.onLatest(() -> new OnLatest.Response<>(first));

        TestMessage retrieved1 = channelB.latest().get(PERSISTENCE.persistFor + 1, TimeUnit.MILLISECONDS);
        assertEquals(first, retrieved1);

        // NDN will cache the latest message for its persistence period so we must wait for it to expire
        Thread.sleep(PERSISTENCE.persistFor + 1);

        TestMessage second = TestMessage.buildRandom(10);
        channelA.onLatest(() -> new OnLatest.Response<>(second));

        TestMessage retrieved2 = channelB.latest().get(PERSISTENCE.persistFor + 1, TimeUnit.MILLISECONDS);
        assertEquals(second, retrieved2);
    }

    @Ignore // temporarily ignore, TODO move this to some sort of stress test
    @Test
    public void testPublishingManyMessages() throws Exception {
        int numMessagesToPublish = 10000;

        String randomName = "/test/publishing-many/" + TestHelper.generateRandomString(20);
        final Channel<TestMessage> channelA = openChannel(randomName, TestMessage.class);
        final Channel<TestMessage> channelB = openChannel(randomName, TestMessage.class);
        final TestCounter counter = new TestCounter();

        List<Integer> retrieved = new ArrayList<>(numMessagesToPublish);
        channelA.subscribe(publishedObject -> {
            logger.info("Channel A received message # " + publishedObject.c);
            retrieved.add(publishedObject.c);
            synchronized (counter) {
                counter.count++;
            }
        });
        logger.info("First channel is subscribed.");
        assertEquals(0, counter.count);

        for (int i = 0; i < numMessagesToPublish; i++) {
            TestMessage message = TestMessage.buildRandom(20);
            message.c = i;
            channelB.publish(message);
            logger.info("Second channel has published message #" + i);
        }

        waitForCounterAtMost(counter, numMessagesToPublish, 3000);
        channelA.close();
        channelB.close();

        logger.info("Retrieved messages: " + Arrays.toString(retrieved.toArray()));
        logger.info("Missing messages: " + Arrays.toString(missing(retrieved, numMessagesToPublish).toArray()));
        logger.info("Retrieved " + counter.count + " messages");
        System.out.println("Retrieved " + counter.count + " messages");
        assertEquals(numMessagesToPublish, retrieved.size());
        assertEquals(numMessagesToPublish, counter.count);
    }

    private List<Integer> missing(List<Integer> retrieved, int numExpected) {
        List<Integer> expected = IntStream.range(0, numExpected).boxed().collect(Collectors.toList());
        return expected.stream().filter((x) -> !retrieved.contains(x)).collect(Collectors.toList());
    }

    private class PublisherRunnable implements Runnable {

        private final int id;
        private final String randomName;

        PublisherRunnable(String randomName, int id) {
            this.randomName = randomName;
            this.id = id;
        }

        @Override
        public void run() {
            logger.info("Starting publisher {}", id);
            String oldName = Thread.currentThread().getName();
            Thread.currentThread().setName("publisher#" + id);

            try {
                Channel<TestMessage> publisher = openChannel(randomName, TestMessage.class);
                publisher.publish(TestMessage.buildRandom(10));
                publisher.close();
            } catch (Throwable t) {
                logger.error(t);
            }

            Thread.currentThread().setName(oldName);
            logger.info("Stopping publisher {}", id);
        }
    }
}
