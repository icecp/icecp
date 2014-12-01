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