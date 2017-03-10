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
package com.intel.icecp.node.security.crypto.signature;

import com.intel.icecp.core.security.crypto.signature.SignatureScheme;
import com.intel.icecp.node.security.crypto.key.KeyProvider;
import com.intel.icecp.core.security.crypto.key.asymmetric.KeyPair;
import com.intel.icecp.node.security.RandomBytesGenerator;
import org.junit.Assert;

/**
 * Test for {@link AsymmetricSignatureScheme} subclasses 
 * 
 * Note that only key of size 1024 is allowed on system where the 
 * "Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy"
 * file is not installed. Therefore the field {@link AsymmetricSignatureSchemeTest#KEY_SIZES}
 * contains only 1024.
 * 
 */
public abstract class AsymmetricSignatureSchemeTest {
    
    /** Bytes to sign */
    private final byte[] toSign = RandomBytesGenerator.getRandomBytes(1024);
    
    /**
     * Tests an asymmetric signature creation
     * 
     * @param algorithm Key algorithm
     * @param schemes Asymmetric signature schemes to test
     * @param keySizes Array of key sizes (in bits) to use for testing
     * @throws Exception In case of error
     */
    protected void signTest(String algorithm, SignatureScheme[] schemes, int[] keySizes) throws Exception {
        for (SignatureScheme s : schemes) {
            for (int size : keySizes) {
                KeyPair kp = KeyProvider.generateKeyPair(algorithm, size);
                Assert.assertNotNull(s.sign(toSign, kp.getPrivateKey()));
            }
        }
    }
    
    /**
     * Tests an asymmetric signature verification
     * 
     * @param algorithm Key algorithm
     * @param schemes Asymmetric signature schemes to test
     * @param keySizes Array of key sizes (in bits) to use for testing
     * @throws Exception If verification fails (and consequently the test)
     */
    protected void verifyTest(String algorithm, SignatureScheme[] schemes, int[] keySizes) throws Exception {
        for (SignatureScheme s : schemes) {
            for (int size : keySizes) {
                KeyPair kp = KeyProvider.generateKeyPair(algorithm, size);
                // If fails, throws an exception and invalidates the test
                s.verify(s.sign(toSign, kp.getPrivateKey()), toSign, kp.getPublicKey());
            }
        }
    }
    

}
