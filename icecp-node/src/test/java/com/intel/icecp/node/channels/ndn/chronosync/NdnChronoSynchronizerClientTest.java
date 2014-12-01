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

import com.intel.icecp.node.channels.ndn.chronosync.NdnChronoSynchronizerClient.Callback;
import com.intel.jndn.mock.MockFace;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 */
public class NdnChronoSynchronizerClientTest {

    Face face = setupMockFace();
    Name broadcastPrefix = new Name("/broadcast");
    NdnChronoSynchronizerClient instance = new NdnChronoSynchronizerClient(face, broadcastPrefix);

    @Captor
    ArgumentCaptor<Set<NdnChronoState>> changeCaptor;

    private static MockFace setupMockFace() {
        return new MockFace();
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testStart() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        instance.start(clientId -> {
            assertNotNull(clientId);
            latch.countDown();
        }, thing -> fail("start() should not fail."));

        latch.await(3, TimeUnit.SECONDS);
    }

    @Ignore // ...until jndn-mock is fixed
    @Test
    public void testOnePublisherAndOneSubscriber() throws Exception {
        NdnChronoSynchronizerClient publisher = new NdnChronoSynchronizerClient(face, broadcastPrefix);
        NdnChronoSynchronizerClient subscriber = new NdnChronoSynchronizerClient(face, broadcastPrefix);
        Callback<Set<NdnChronoState>> callback = mock(Callback.class);
        NdnChronoState state1 = new NdnChronoState(0, 0);
        NdnChronoState state2 = new NdnChronoState(0, 1);
        CountDownLatch latch = new CountDownLatch(2);

        publisher.start(clientId -> latch.countDown(), e -> fail("This method should not fail, but did:" + e));
        subscriber.start(clientId -> latch.countDown(), e -> fail("This method should not fail, but did:" + e));
        face.processEvents();
        face.processEvents();
        latch.await(2, TimeUnit.SECONDS);

        subscriber.subscribe(callback);
        face.processEvents();
        // publisher.publish(state1);
        face.processEvents();
        // publisher.publish(state2);
        face.processEvents();
        face.processEvents();
        face.processEvents();

        verify(callback, times(2)).accept(changeCaptor.capture());
    }
}