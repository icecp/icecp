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

package com.intel.icecp.node.security.crypto.utils;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import com.intel.icecp.core.security.crypto.exception.hash.HashError;
import com.intel.icecp.node.security.RandomBytesGenerator;
import com.intel.icecp.node.security.SecurityConstants;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for {@link CryptoUtils} class.
 * Currently, it tests only hash computation.
 *
 */
public class CryptoUtilsTest {

    // String used for test
    private final String testString = "I am a string";

    // Hex representation of the hash of testString
    private final String sha256resHex = "fe29bfd7e19d2a352cd9bfaa21176d521b874969dd57e8c72c8668fc8fd8f4fc";

    // String used for test
    private final byte[] testString2 = RandomBytesGenerator.getRandomBytes(100);

    @Test
    public void testHash() throws HashError {
        byte[] res, res2;

        // Test invalid algorithm case
        try {
            // Should throw an exception
            CryptoUtils.hash(testString.getBytes(), "InvalidAlgorithm");
            assertFalse(true);
        } catch (HashError err) {
            // Nothing to do here
        }

        // Test if hash is correct
        res = CryptoUtils.hash(testString.getBytes(), SecurityConstants.SHA256);
        assertTrue(CryptoUtils.bytesToHex(res).equalsIgnoreCase(sha256resHex));

        // Test if hashed are different
        res2 = CryptoUtils.hash(testString2, SecurityConstants.SHA256);
        assertTrue(!CryptoUtils.bytesToHex(res2).equalsIgnoreCase(CryptoUtils.bytesToHex(res)));

    }

}
