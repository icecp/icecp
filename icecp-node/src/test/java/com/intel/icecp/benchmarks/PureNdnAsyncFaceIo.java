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

package com.intel.icecp.benchmarks;

import com.intel.icecp.common.TestHelper;
import com.intel.jndn.utils.impl.KeyChainFactory;
import net.named_data.jndn.Face;
import net.named_data.jndn.ForwardingFlags;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnRegisterSuccess;
import net.named_data.jndn.ThreadPoolFace;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.transport.AsyncTcpTransport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * This is a path-finding tool, not a unit/integration test; it should not be run as a part of regular testing.
 *
 */
public class PureNdnAsyncFaceIo {

    private static final Logger LOGGER = LogManager.getLogger();

    @Ignore
    @Test
    public void testAsynchronousNdnCommunication() throws Exception {
        KeyChain keyChain = KeyChainFactory.configureTestKeyChain(new Name("/test/identity"));
        ScheduledExecutorService cachedThreadPool = Executors.newScheduledThreadPool(10);
        String hostName = TestHelper.getNfdHostName();
        LOGGER.info("Using NFD: {}", hostName);

        // setup faces: two for listening and one for sending Interests
        Name name1 = new Name("/a/b/c");
        Name name2 = new Name("/a/b");
        Face face1 = setupAsyncFace(hostName, keyChain, cachedThreadPool);
        Face face2 = setupAsyncFace(hostName, keyChain, cachedThreadPool);

        // setup callback
        CountDownLatch faceRegistered = new CountDownLatch(2);
        CountDownLatch interestSeen = new CountDownLatch(1);
        OnInterestCallback callback = (prefix, interest, face, interestFilterId, filter) -> {
            LOGGER.info("Interest received on prefix: " + prefix.toUri());
            interestSeen.countDown();
        };
        OnRegisterFailed failed = prefix -> {
            LOGGER.error("Failed to register: {}", prefix);
            faceRegistered.countDown();
        };
        OnRegisterSuccess succeeded = (prefix, registeredPrefixId) -> {
            LOGGER.info("Registered: {}", prefix);
            faceRegistered.countDown();
        };

        // register prefixes
        ForwardingFlags flags = new ForwardingFlags();
        flags.setCapture(true);
        face1.registerPrefix(name1, callback, failed, succeeded, flags);
        face2.registerPrefix(name2, callback, failed, succeeded, flags);

        // once registered, send interest
        faceRegistered.await(5, TimeUnit.SECONDS);
        face2.expressInterest(new Interest(name1), null);

        // wait for callbacks to fire
        interestSeen.await(5, TimeUnit.SECONDS);

        // only one callback should fire
        assertEquals(0, interestSeen.getCount());
    }

    private Face setupAsyncFace(String nfdHostName, KeyChain keyChain, ScheduledExecutorService cachedThreadPool) throws SecurityException {
        AsyncTcpTransport transport = new AsyncTcpTransport(cachedThreadPool);
        AsyncTcpTransport.ConnectionInfo connectionInfo = new AsyncTcpTransport.ConnectionInfo(nfdHostName);
        Face face = new ThreadPoolFace(cachedThreadPool, transport, connectionInfo);
        face.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
        return face;
    }
}