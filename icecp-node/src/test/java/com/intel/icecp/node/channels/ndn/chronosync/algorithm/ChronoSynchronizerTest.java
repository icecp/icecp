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

package com.intel.icecp.node.channels.ndn.chronosync.algorithm;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Test ChronoSynchronizer
 *
 */
public class ChronoSynchronizerTest {

    private static Set<TestState> states;
    private static ChronoSynchronizer.IncomingPendingRequest<TestState> pendingRequest;
    private static ChronoSynchronizer.OutgoingRequestAction outgoingRequestAction;
    private ChronoSynchronizer<TestState> instance;
    @Captor
    private ArgumentCaptor<Set> statesCaptor;

    @Captor
    private ArgumentCaptor<Digest> digestCaptor;

    @BeforeClass
    public static void beforeClass() {
        pendingRequest = mock(ChronoSynchronizer.IncomingPendingRequest.class);
        outgoingRequestAction = mock(ChronoSynchronizer.OutgoingRequestAction.class);
        states = new HashSet<>();
        states.add(new TestState(0));
    }

    @Before
    public void beforeTest() throws NoSuchAlgorithmException {
        instance = buildDefaultSynchronizer();
        statesCaptor = ArgumentCaptor.forClass(Set.class);
        digestCaptor = ArgumentCaptor.forClass(Digest.class);
    }

    private ChronoSynchronizer<TestState> buildDefaultSynchronizer() {
        try {
            MessageDigest digestAlgorithm = MessageDigest.getInstance("SHA-256");
            HistoricalDigestTree<TestState> historicalDigestTree = new HistoricalDigestTree<>(10, digestAlgorithm);
            return new ChronoSynchronizer<>(historicalDigestTree, 0);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @After
    public void afterTest() {
        reset(pendingRequest);
        reset(outgoingRequestAction);
    }

    @Test
    public void testUpdateState() {
        TestState state = mock(TestState.class);
        when(state.toBytes()).thenReturn("...".getBytes());

        instance.onReceivedDigest(instance.currentDigest(), pendingRequest);
        instance.updateState(state);

        verify(pendingRequest, times(1)).satisfy(digestCaptor.capture(), statesCaptor.capture());
        assertEquals(instance.currentDigest(), digestCaptor.getValue());
        assertEquals(instance.currentStates(), statesCaptor.getValue());
    }

    @Test
    public void testThatObserversObserve() throws SynchronizationException {
        ChronoSynchronizer.Observer observer = mock(ChronoSynchronizer.Observer.class);
        Digest digest = new Digest(new byte[]{-33, 63, 97, -104, 4, -87, 47, -37, 64, 87, 25, 45, -60, 61, -41, 72, -22, 119, -118, -36, 82, -68, 73, -116, -24, 5, 36, -64, 20, -72, 17, 25});

        instance.observe(observer);
        instance.onReceivedState(digest, states);

        verify(observer, times(1)).notify(digestCaptor.capture(), statesCaptor.capture());
    }

    @Test
    public void testOnReceivedUnknownDigest() {
        Digest digest = new Digest("0123".getBytes());
        instance.onReceivedDigest(digest, pendingRequest);

        verify(pendingRequest, timeout(1000).times(1)).satisfy(digestCaptor.capture(), statesCaptor.capture());
        assertEquals(0, statesCaptor.getValue().size());
    }

    @Test
    public void testOnReceivedUnknownDigestWithState() {
        TestState state = new TestState(0);
        Digest digest = new Digest("0123".getBytes());

        instance.updateState(state);
        instance.onReceivedDigest(digest, pendingRequest);

        verify(pendingRequest, timeout(1000).times(1)).satisfy(digestCaptor.capture(), statesCaptor.capture());
        assertEquals(1, statesCaptor.getValue().size());
        assertTrue(statesCaptor.getValue().contains(state));
        assertEquals(instance.currentDigest(), digestCaptor.getValue());
    }

    @Test
    public void testOnReceivedKnownDigest() {
        TestState a = new TestState("a", 0);
        TestState b = new TestState("b", 0);

        instance.updateState(a);
        Digest digest = instance.currentDigest();
        instance.updateState(b);
        instance.onReceivedDigest(digest, pendingRequest);

        verify(pendingRequest, timeout(1000).times(1)).satisfy(digestCaptor.capture(), statesCaptor.capture());
        assertEquals(1, statesCaptor.getValue().size());
        assertTrue(statesCaptor.getValue().contains(b));
        assertEquals(instance.currentDigest(), digestCaptor.getValue());
    }

    @Test
    public void testOnReceivedEmptyDigest() {
        TestState a = new TestState("a", 0);
        TestState b = new TestState("b", 0);

        Digest empty = instance.currentDigest();
        instance.updateState(a);
        instance.updateState(b);
        instance.onReceivedDigest(empty, pendingRequest);

        verify(pendingRequest, timeout(1000).times(1)).satisfy(digestCaptor.capture(), statesCaptor.capture());
        assertEquals(2, statesCaptor.getValue().size());
        assertTrue(statesCaptor.getValue().contains(a));
        assertTrue(statesCaptor.getValue().contains(b));
        assertEquals(instance.currentDigest(), digestCaptor.getValue());
    }

    @Test(expected = SynchronizationException.class)
    public void testOnReceivedIncorrectState() throws SynchronizationException {
        Digest digest = new Digest("0123".getBytes());

        instance.onReceivedState(digest, states);
    }

    @Test
    public void testOnReceivedEmptyState() throws SynchronizationException {
        Digest emptyDigest = instance.currentDigest();
        Set<TestState> emptyStates = Collections.emptySet();

        instance.onReceivedState(emptyDigest, emptyStates);

        assertEquals(emptyStates, instance.currentStates());
        assertEquals(emptyDigest, instance.currentDigest());
    }
}
