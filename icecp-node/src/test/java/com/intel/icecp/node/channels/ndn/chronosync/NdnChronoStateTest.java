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

import com.intel.icecp.node.channels.ndn.chronosync.algorithm.TestState;
import net.named_data.jndn.Name;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Test NdnChronoState
 *
 */
public class NdnChronoStateTest {

    private final NdnChronoState instance = new NdnChronoState(1234, 5678);

    @Test
    public void testMatches() throws Exception {
        assertFalse(instance.matches(new TestState(0)));
        assertFalse(instance.matches(new NdnChronoState(0, 0)));
        assertTrue(instance.matches(new NdnChronoState(1234, 0)));
    }

    @Test
    public void testToBytes() throws Exception {
        assertEquals(16, instance.toBytes().length);
    }

    @Test
    public void testCompareTo() throws Exception {
        assertEquals(0, instance.compareTo(new TestState(0)));
        assertEquals(0, instance.compareTo(new NdnChronoState(0, 0)));
        assertEquals(0, instance.compareTo(new NdnChronoState(1234, 5678)));
        assertTrue(instance.compareTo(new NdnChronoState(1234, 0)) > 0);
        assertTrue(instance.compareTo(new NdnChronoState(1234, 9999)) < 0);
    }

    @Test
    public void testToNdnNameComponents() throws Exception {
        Name name = new Name("/a/b/c/");
        name.append(instance.toClientComponent()).append(instance.toMessageComponent());
        assertEquals(1234, name.get(-2).toNumberWithMarker(NdnChronoState.CLIENT_MARKER));
        assertEquals(5678, name.get(-1).toNumberWithMarker(NdnChronoState.MESSAGE_MARKER));
    }

    @Test
    public void testWireEncoding() throws Exception {
        NdnChronoState a = new NdnChronoState(0, 0);
        NdnChronoState b = new NdnChronoState(0, 1);
        NdnChronoState c = new NdnChronoState(1, 1);
        Set<NdnChronoState> states = new LinkedHashSet<>();
        states.add(a);
        states.add(b);
        states.add(c);

        ByteBuffer buffer = NdnChronoState.wireEncodeMultiple(states);
        Set<NdnChronoState> decoded = NdnChronoState.wireDecodeMultiple(buffer);

        assertEquals(states, decoded);
    }
}