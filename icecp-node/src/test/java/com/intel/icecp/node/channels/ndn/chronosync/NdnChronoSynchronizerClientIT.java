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

package com.intel.icecp.node.channels.ndn.chronosync;

import com.intel.icecp.common.TestHelper;
import com.intel.icecp.node.NodeFactory;
import com.intel.jndn.utils.impl.KeyChainFactory;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
public class NdnChronoSynchronizerClientIT {

    public static final int NDN_NETWORK_TIMEOUT_SECONDS = 3;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Name KEYCHAIN_IDENTITY = new Name("/chrono/synchronizer");
    private ScheduledExecutorService executor;
    private Name broadcastPrefix;
    private Client publisher1;
    private Client publisher2;
    private Client subscriber1;
    private Client subscriber2;

    @Before
    public void beforeTest() throws InterruptedException {
        executor = NodeFactory.buildEventLoop();
        broadcastPrefix = new Name("/broadcast/" + new Random().nextLong());

        CountDownLatch latch = new CountDownLatch(4);
        publisher1 = setupClient(broadcastPrefix, latch, 1);
        publisher2 = setupClient(broadcastPrefix, latch, 2);
        subscriber1 = setupClient(broadcastPrefix, latch, 3);
        subscriber2 = setupClient(broadcastPrefix, latch, 4);
        latch.await(NDN_NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private Client setupClient(Name broadcastPrefix, CountDownLatch latch, long clientId) {
        Face face = new Face(TestHelper.getNfdHostName());
        KeyChain keyChain = setupKeyChainOnFace(face, KEYCHAIN_IDENTITY);
        executor.scheduleWithFixedDelay(new NodeEventProcessor(face), 0, 20, TimeUnit.MILLISECONDS);

        NdnChronoSynchronizerClient client = new NdnChronoSynchronizerClient(face, broadcastPrefix, clientId);
        client.start(thing -> {
            LOGGER.info("Started client: {}", thing);
            latch.countDown();
        }, e -> LOGGER.error("Failed to start client", e));

        return new Client(face, keyChain, client);
    }

    private KeyChain setupKeyChainOnFace(Face face, Name identity) {
        KeyChain keyChain;
        try {
            keyChain = KeyChainFactory.configureKeyChain(identity);
            face.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
        } catch (SecurityException e) {
            throw new IllegalStateException("Failed to set command signature; builder cannot proceed without a secure face.", e);
        }
        return keyChain;
    }

    @After
    public void afterTest() {
        executor.shutdownNow();
        stop(publisher1);
        stop(publisher2);
        stop(subscriber1);
        stop(subscriber2);
    }

    private void stop(Client client) {
        client.client.stop();
        client.face.shutdown();
    }

    @Test
    public void testOnePublisherAndOneSubscriber() throws Exception {
        NdnChronoState state1 = new NdnChronoState(0, 0);
        CountDownLatch latch = new CountDownLatch(1);

        subscriber1.client.subscribe(thing -> {
            LOGGER.info("Received {} updated states on client {}", thing.size(), subscriber1.client.clientId());
            assertTrue(thing.contains(state1));
            latch.countDown();
        });
        publisher1.client.publish(state1);

        latch.await(NDN_NETWORK_TIMEOUT_SECONDS * 2, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    @Test
    public void testOnePublisherWithMultipleMessages() throws Exception {
        NdnChronoState state1 = new NdnChronoState(0, 0);
        NdnChronoState state2 = new NdnChronoState(1, 1);
        Set<NdnChronoState> collector = new HashSet<>(2);
        CountDownLatch latch = new CountDownLatch(2);

        subscriber1.client.subscribe(thing -> {
            LOGGER.info("Received {} updated states", thing.size());
            assertEquals(1, thing.size());
            collector.addAll(thing);
            latch.countDown();
        });
        publisher1.client.publish(state1);
        publisher1.client.publish(state2);

        latch.await(NDN_NETWORK_TIMEOUT_SECONDS * 10, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
        assertEquals(Sets.newSet(state1, state2), collector);
    }

    @Test
    public void testMultiplePublishersWithMultipleMessages() throws Exception {
        NdnChronoState state1 = new NdnChronoState(0, 0);
        NdnChronoState state2 = new NdnChronoState(1, 1);
        Set<NdnChronoState> collector1 = new HashSet<>(4);
        Set<NdnChronoState> collector2 = new HashSet<>(4);
        CountDownLatch latch = new CountDownLatch(6); // why 6?

        subscriber1.client.subscribe(thing -> {
            LOGGER.info("Received {} updated states", thing.size());
            collector1.addAll(thing);
            latch.countDown();
        });
        subscriber2.client.subscribe(thing -> {
            LOGGER.info("Received {} updated states", thing.size());
            collector2.addAll(thing);
            latch.countDown();
        });
        publisher1.client.publish(0);
        publisher1.client.publish(1);
        publisher2.client.publish(0);
        publisher2.client.publish(1);

        latch.await(NDN_NETWORK_TIMEOUT_SECONDS * 10, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
        assertEquals(publisher1.client.currentState(), subscriber1.client.currentState());
        assertEquals(publisher1.client.currentState(), publisher2.client.currentState());
    }

    private class Client {
        public final Face face;
        public final KeyChain keyChain;
        public final NdnChronoSynchronizerClient client;

        public Client(Face face, KeyChain keyChain, NdnChronoSynchronizerClient client) {
            this.face = face;
            this.keyChain = keyChain;
            this.client = client;
        }
    }

    private class NodeEventProcessor implements Runnable {

        final Face face;

        public NodeEventProcessor(Face face) {
            this.face = face;
        }

        @Override
        public void run() {
            try {
                face.processEvents();
            } catch (IOException e) {
                LOGGER.error("IO failure.", e);
            } catch (EncodingException e) {
                LOGGER.error("Encoding failure.", e);
            }
        }
    }
}