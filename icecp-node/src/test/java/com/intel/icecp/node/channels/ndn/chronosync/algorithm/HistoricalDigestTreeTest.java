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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test {@link HistoricalDigestTree}
 *
 */
public class HistoricalDigestTreeTest {

    private static final Logger LOGGER = LogManager.getLogger();
    HistoricalDigestTree<TestState> instance;

    public HistoricalDigestTreeTest() throws NoSuchAlgorithmException {
        MessageDigest digestAlgorithm = MessageDigest.getInstance("SHA-256");
        instance = new HistoricalDigestTree<>(4, digestAlgorithm);
    }

    @Test
    public void testDigestCreation() {
        assertEquals(32, instance.digest().toBytes().length);
        assertTrue(instance.isEmpty(instance.digest()));
        instance.add(new TestState(0));
        assertFalse(instance.isEmpty(instance.digest()));
    }

    @Test
    public void testThatDigestsAreNotMangled() {
        instance.add(new TestState(1234));
        Digest digest1 = instance.digest();
        Digest digest2 = instance.digest();
        assertEquals(digest1, digest2);
    }

    @Test
    public void testThatDigestsAreCalculatedOnce() {
        TestState state = mock(TestState.class);
        when(state.toBytes()).thenReturn("000".getBytes());

        instance.add(state);

        verify(state, times(1)).toBytes();
    }

    @Test
    public void testUpdatingState() {
        Digest empty = instance.digest();
        assertEquals(0, instance.all().size());

        TestState state1 = new TestState(1);
        boolean changed = instance.add(state1);
        Digest current = instance.digest();

        assertTrue(changed);
        assertFalse(empty.equals(current));
        assertEquals(1, instance.all().size());

        changed = instance.add(state1);

        assertFalse(changed);
        assertTrue(current.equals(instance.digest()));
        assertEquals(1, instance.all().size());
    }

    @Test
    public void testAddingSameTypeOfStates() {
        TestState state1 = new TestState(1);
        instance.add(state1);
        assertEquals(1, instance.all().size());

        TestState state2 = new TestState(2);
        TestState state3 = new TestState(3);
        instance.add(state2, state3);

        assertTrue(state2.matches(state3));
        assertEquals(1, instance.all().size());
    }

    @Test
    public void testAddingDissimilarStates() {
        TestState state1 = new TestState("a", 1);
        TestState state2 = new TestState("a", 2);
        TestState state3 = new TestState("b", 1);

        instance.add(state1);
        assertEquals(1, instance.all().size());
        instance.add(state2, state3);
        assertEquals(2, instance.all().size());
    }

    @Test
    public void testIsNewer() {
        TestState state1 = new TestState(1);
        TestState state2 = new TestState(2);
        assertTrue(instance.isNewer(state2, state1));
        assertFalse(instance.isNewer(state2, state2));
        assertFalse(instance.isNewer(state1, state2));
    }

    @Test
    public void testDigestLogging() {
        Digest digest1 = instance.digest();
        assertTrue(instance.isKnown(digest1));

        instance.add(new TestState(0));
        Digest digest2 = instance.digest();
        assertTrue(instance.isKnown(digest1));
        assertTrue(instance.isKnown(digest2));

        Digest digest3 = new Digest(new byte[]{0, 1, 2, 3});
        assertFalse(instance.isKnown(digest3));
    }

    @Test
    public void testIsCurrent() {
        instance.add(new TestState(0));
        Digest digest = instance.digest();
        assertTrue(instance.isCurrent(digest));

        instance.add(new TestState(1));
        assertFalse(instance.isCurrent(digest));
    }

    @Test
    public void testComplement() {
        TestState state1 = new TestState("a", 1);
        TestState state2 = new TestState("a", 2);
        TestState state3 = new TestState("b", 1);
        Digest empty = instance.digest();

        instance.add(state1, state3);
        Set<TestState> diff1 = instance.complement(empty);
        assertTrue(diff1.contains(state1));
        assertFalse(diff1.contains(state2));
        assertTrue(diff1.contains(state3));

        Set<TestState> diff2 = instance.complement(instance.digest());
        assertEquals(0, diff2.size());

        instance.add(state2);
        Set<TestState> diff3 = instance.complement(empty);
        assertFalse(diff3.contains(state1));
        assertTrue(diff3.contains(state2));
        assertTrue(diff3.contains(state3));
    }

    @Test
    public void testComplementOfUnknownDigest() {
        Digest unknown = new Digest("...".getBytes());
        TestState state0 = new TestState(0);
        instance.add(state0);

        Set<TestState> diff = instance.complement(unknown);
        assertTrue(diff.contains(state0));
    }

    @Test
    public void testInsertionPerformance() {
        final int numStates = 10000;
        long start = System.nanoTime();

        for (int i = 0; i < numStates; i++) {
            instance.add(new TestState(i));
            instance.digest();
        }

        long end = System.nanoTime();
        LOGGER.info("Added {} states ({} bytes) in (ms): {}", numStates, numStates * 4, (end - start) / 1000000.0);
    }

    @Test
    public void testThatHistoryLimitIsEnforced() {
        instance.add(new TestState(0));
        Digest digest = instance.digest();

        instance.add(new TestState(1));
        instance.add(new TestState(2));
        instance.add(new TestState(3));
        assertTrue(instance.isKnown(digest));

        instance.add(new TestState(4));
        assertFalse(instance.isKnown(digest));
    }
}
