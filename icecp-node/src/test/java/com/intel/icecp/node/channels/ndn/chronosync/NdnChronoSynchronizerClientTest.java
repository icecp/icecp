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