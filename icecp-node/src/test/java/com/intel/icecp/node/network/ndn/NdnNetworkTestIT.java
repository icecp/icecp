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

package com.intel.icecp.node.network.ndn;

import com.intel.icecp.common.TestHelper;
import com.intel.jndn.utils.impl.KeyChainFactory;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test NdnNetwork; TODO cannot rely on hard-coded "localhost" references,
 * should handle remote NFDs
 *
 */
public class NdnNetworkTestIT {

    private static final Logger logger = LogManager.getLogger();
    private static final int NUM_THREADS = 2;
    private static final Name GLOBAL_PREFIX = new Name("/test");
    private final ScheduledExecutorService pool;
    NdnNetwork instance;

    public NdnNetworkTestIT() {
        pool = Executors.newScheduledThreadPool(NUM_THREADS);
        instance = newInstance("01234567890", "localhost");
    }

    public NdnNetwork newInstance(String id, String hostname) {
        URI nfdUri = URI.create("tcp4://" + hostname);
        Name identityPrefix = new Name(GLOBAL_PREFIX).append(id);
        Face face = new Face(hostname);

        try {
            KeyChain keyChain = KeyChainFactory.configureTestKeyChain(identityPrefix);
            face.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
        } catch (net.named_data.jndn.security.SecurityException e) {
            throw new IllegalStateException("Failed to set command signature; builder cannot proceed without a secure face.", e);
        }

        pool.scheduleAtFixedRate(new EventProcessor(face), 0, 30, TimeUnit.MILLISECONDS);
        return new NdnNetwork(nfdUri, GLOBAL_PREFIX, identityPrefix, face, pool);
    }

    @Test
    public void testScheme() {
        assertEquals("ndn", instance.scheme());
    }

    @Test
    public void testId() {
        assertNotNull(instance.local());
    }

    @Test
    public void testList() {
        assertEquals(0, instance.list().length);
    }

    @Test
    public void testRtt() throws Exception {
        NdnNetworkEndpoint endpoint = new NdnNetworkEndpoint(new Name("/test/endpoint"), TestHelper.getNfdHostName());
        int rtt = instance.rtt(endpoint).get();
        logger.info(String.format("Retrieved RTT from %s in (ms): %d", endpoint, rtt));
        assertTrue(rtt > 0);
    }

    @Ignore // TODO currently requires a local NFD
    @Test
    public void testAddAndRemove() throws Exception {
        NdnNetworkEndpoint endpoint = new NdnNetworkEndpoint(new Name("/test/endpoint"), TestHelper.getNfdHostName());

        CompletableFuture<Void> added = instance.connect(endpoint);
        added.get();

        assertEquals(1, instance.list().length);

        CompletableFuture<Void> removed = instance.disconnect(endpoint);
        removed.get();

        assertEquals(0, instance.list().length);
    }

    @Ignore // TODO currently requires a local NFD
    @Test
    public void testDiscoverAdvertisedInstance() throws Exception {
        NdnNetwork instance2 = newInstance("other", "localhost");
        instance2.advertise();

        NdnNetworkEndpoint[] found = instance.discover().get();
        assertEquals(1, found.length);
    }

    private static class EventProcessor implements Runnable {

        private final Face face;

        public EventProcessor(Face face) {
            this.face = face;
        }

        @Override
        public void run() {
            try {
                face.processEvents();
            } catch (EncodingException | IOException ex) {
                logger.error(ex);
            }
        }

    }
}
