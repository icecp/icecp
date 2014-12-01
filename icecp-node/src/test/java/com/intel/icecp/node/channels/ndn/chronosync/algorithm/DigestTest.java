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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test Digest
 *
 */
public class DigestTest {

    @Test
    public void testValue() throws Exception {
        byte[] bytes = "01234".getBytes();
        Digest instance = new Digest(bytes);
        assertArrayEquals(bytes, instance.toBytes());
    }

    @Test
    public void testHashCodeAndEquals() throws Exception {
        Digest a1 = new Digest("asdfghjkl;".getBytes());
        Digest a2 = new Digest("asdfghjkl;".getBytes());
        Digest b = new Digest(new byte[]{});

        assertEquals(a1.hashCode(), a2.hashCode());
        assertTrue(a1.equals(a2));

        assertNotSame(a1.hashCode(), b.hashCode());
        assertFalse(a1.equals(b));

        // for code coverage! otherwise useless...
        assertTrue(a1.equals(a1));
        assertFalse(a1.equals(null));
        assertFalse(a1.equals(new Object()));
    }

    @Test
    public void testHexConversionFromBytes() {
        Digest digest = new Digest(new byte[]{0, 1, 2});

        String hex = digest.toHex();

        assertEquals("000102", hex);
    }
}