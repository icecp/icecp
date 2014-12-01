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