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

import com.intel.icecp.common.TestCounter;
import com.intel.icecp.common.TestHelper;
import com.intel.icecp.node.NodeFactory;
import com.intel.jndn.utils.impl.KeyChainFactory;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.util.Blob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * This is a path-finding tool, not a unit/integration test; it should not be run as a part of regular testing. It tests
 * bandwidth and latency between two faces connected to an external NFD.
 *
 */
public class PureNdnThroughputBenchmark {

    private static final Logger logger = LogManager.getLogger();
    private static final String CHANNEL_NAME = "/test/channel/asdf";
    private static final String IDENTITY_NAME = "/test/identity";
    private static final long CRANK_INTERVAL_MS = 10;
    private static final long DATA_LIFETIME_MS = 5000;
    private static final long INTEREST_LIFETIME_MS = 2000;
    private static final int NUM_PACKETS = 1000;
    private static final int CONTENT_SIZE_BYTES = 6000;
    private final ScheduledExecutorService eventLoop;
    private long faceCount = 0;

    public PureNdnThroughputBenchmark() {
        eventLoop = NodeFactory.buildEventLoop();
    }

    private static long getSegment(Name name) {
        try {
            return name.get(-1).toSegment();
        } catch (EncodingException ex) {
            logger.error("Failed to parse segment component: " + name.toUri());
            return -1;
        }
    }

    private Face buildFace(String uri) throws net.named_data.jndn.security.SecurityException {
        KeyChain keyChain = KeyChainFactory.configureTestKeyChain(new Name(IDENTITY_NAME));
        Face face = new Face(uri);
        face.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
        turnCrank(face, faceCount++);
        return face;
    }

    private void turnCrank(final Face face, final long id) {
        eventLoop.scheduleAtFixedRate(() -> {
            try {
                logger.debug("{} processing...", id);
                face.processEvents();
            } catch (Exception ex) {
                logger.error("Error processing events.", ex);
            }
        }, 0, CRANK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Ignore
    @Test
    public void testPublishAndSubscribe() throws Exception {
        Face a = buildFace(TestHelper.getNfdHostName());
        Face b = buildFace(TestHelper.getNfdHostName());
        final List<Measurement> measurements = new ArrayList<>();

        final List<Data> content = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Data data = new Data(new Name(CHANNEL_NAME).appendSegment(i));
            data.getMetaInfo().setFreshnessPeriod(DATA_LIFETIME_MS);
            byte[] buffer = new byte[CONTENT_SIZE_BYTES];
            new Random().nextBytes(buffer);
            data.setContent(new Blob(buffer));
            content.add(data);
        }
        logger.info("First packet content: " + content.get(0).getContent().toHex());

        a.registerPrefix(new Name(CHANNEL_NAME), (prefix, interest, face, interestFilterId, filter) -> {
            try {
                long id = interest.getName().get(-1).toSegment();
                measurements.get((int) id).interestReceived = System.nanoTime();
                logger.debug("OnInterest: " + id);
                face.putData(content.get((int) id));
            } catch (Exception ex) {
                logger.error("Failed to send data for: " + interest.toUri());
            }
        }, prefix -> {
            logger.error("Failed to register prefix: " + prefix);
        });

        long startTime = System.nanoTime();
        final TestCounter datasReceived = new TestCounter();
        final TestCounter timeouts = new TestCounter();
        for (int i = 0; i < NUM_PACKETS; i++) {
            Interest interest = new Interest(new Name(CHANNEL_NAME).appendSegment(i));
            interest.setInterestLifetimeMilliseconds(INTEREST_LIFETIME_MS);
            logger.debug("Express interest: " + i);
            measurements.add(new Measurement(System.nanoTime()));

            b.expressInterest(interest, (interest1, data) -> {
                long id = getSegment(data.getName());
                measurements.get((int) id).dataReceived = System.nanoTime();
                datasReceived.count++;
                logger.debug("OnData: " + getSegment(data.getName()) + ", " + data.getContent().size());
            }, interest12 -> {
                logger.debug("OnTimeout: " + getSegment(interest12.getName()));
                timeouts.count++;
            });
        }

        while (timeouts.count + datasReceived.count + 1 < NUM_PACKETS) {

        }
        long endTime = System.nanoTime();
        double totalTime = (endTime - startTime) / (double) 1000000000; // in sec

        logger.info("Event processing interval (ms): " + CRANK_INTERVAL_MS);
        logger.info("Packet size (bytes): " + CONTENT_SIZE_BYTES);
        logger.info("Number of packets sent: " + NUM_PACKETS);
        logger.info("Total time spent (sec): " + totalTime);

        logger.info("Datas received: " + datasReceived.count);
        logger.info("Timeouts: " + timeouts.count);
        logger.info("Average send time (ms): " + Math.round(averageSendTime(measurements) / 1000000));
        logger.info("Average RTT (ms): " + Math.round(averageRoundTripTime(measurements) / 1000000));

        long totalBits = (CONTENT_SIZE_BYTES * datasReceived.count * 8);
        logger.info("Total data transferred (bits): " + totalBits);
        logger.info("Bandwidth (bits/sec): " + Math.round(totalBits / totalTime));
    }

    private float averageSendTime(List<Measurement> measurements) {
        long ns = 0;
        for (Measurement m : measurements) {
            if (m.interestSent > 0 && m.interestReceived > 0) {
                assertTrue(m.interestReceived > m.interestSent);
                long time = m.interestReceived - m.interestSent;
                logger.debug("Send time: {}", time);
                ns += time;
            } else {
                logger.debug("Incomplete measurement: {}", m);
            }
        }
        return ns / (float) measurements.size();
    }

    private float averageRoundTripTime(List<Measurement> measurements) {
        long ns = 0;
        for (Measurement m : measurements) {
            if (m.interestSent > 0 && m.dataReceived > 0) {
                assertTrue(m.dataReceived > m.interestSent);
                long time = m.dataReceived - m.interestSent;
                logger.debug("RTT: {}", time);
                ns += time;
            } else {
                logger.info("Incomplete measurement: {}", m);
            }
        }
        return ns / (float) measurements.size();
    }

    private class Measurement {

        final long interestSent;
        long interestReceived;
        long dataReceived;

        Measurement(long interestSent) {
            this.interestSent = interestSent;
        }

        public String toString() {
            return "[interest sent = " + interestSent + ", interest received = " + interestReceived + ", data received = " + dataReceived + "]";
        }
    }
}
