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
package com.intel.icecp.benchmarks;

import com.intel.icecp.common.TestHelper;
import com.intel.jndn.mock.MockKeyChain;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.util.Blob;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a path-finding tool, not a unit/integration test; it should not be run as a part of regular testing.
 *
 */
public class PureNdnRegistrationIssue {

    private static final Logger logger = Logger.getLogger(PureNdnRegistrationIssue.class.getName());
    private static final Name PREFIX = new Name("/test").append(generateRandomBlob());

    private Face producerFace;
    private Face consumerFace;

    private static Blob generateRandomBlob() {
        byte[] bytes = new byte[10];
        new Random().nextBytes(bytes);
        return new Blob(bytes);
    }

    @Before
    public void before() throws Exception {
        String ip = TestHelper.getNfdHostName();
        this.producerFace = new Face(ip);
        this.consumerFace = new Face(ip);
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(2);

        KeyChain mockKeyChain = MockKeyChain.configure(new Name("/test/server"));
        producerFace.setCommandSigningInfo(mockKeyChain, mockKeyChain.getDefaultCertificateName());
        pool.scheduleAtFixedRate(new EventProcessor(consumerFace), 0, 10, TimeUnit.MILLISECONDS);
        pool.scheduleAtFixedRate(new EventProcessor(producerFace), 0, 10, TimeUnit.MILLISECONDS);
    }

    @Ignore
    @Test
    public void testRetrieval() throws Exception {
        // setup server
        Data servedData = new Data();
        servedData.setContent(new Blob("....."));
        servedData.getMetaInfo().setFreshnessPeriod(0);
        producerFace.registerPrefix(PREFIX, new DataServer(servedData), null);

        // if this is disabled, test occasionally fails
        Thread.sleep(100);

        // send interest
        DataClient client = new DataClient();
        consumerFace.expressInterest(new Interest(PREFIX, 2000), client, client);

        while (!client.isDone()) {
            Thread.sleep(10);
        }

        // verify
        Assert.assertFalse(client.hasError());
        Assert.assertEquals(servedData.getContent().toString(), client.getData().getContent().toString());
    }

    private class DataServer implements OnInterestCallback {

        private final Data data;

        DataServer(Data data) {
            this.data = data;
        }

        @Override
        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
            data.setName(interest.getName());
            try {
                face.putData(data);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Failed to put data.", ex);
            }
        }
    }

    private class DataClient implements OnData, OnTimeout {

        private Data data;
        private Throwable error;

        @Override
        public void onData(Interest interest, Data data) {
            logger.log(Level.INFO, "Data retrieved: " + data.getName().toUri());
            this.data = data;
        }

        @Override
        public void onTimeout(Interest interest) {
            logger.log(Level.SEVERE, "Interest timed out: " + interest.getName().toUri());
            this.error = new TimeoutException();
        }

        public Data getData() {
            return data;
        }

        boolean isDone() {
            return data != null || error != null;
        }

        private boolean hasError() {
            return error != null;
        }
    }

    private class EventProcessor implements Runnable {

        private final Face face;

        EventProcessor(Face face) {
            this.face = face;
        }

        @Override
        public void run() {
            try {
                face.processEvents();
            } catch (IOException | EncodingException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }
}
