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
