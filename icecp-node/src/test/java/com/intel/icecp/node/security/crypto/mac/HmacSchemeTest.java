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
package com.intel.icecp.node.security.crypto.mac;

import com.intel.icecp.core.security.crypto.key.SecretKey;
import com.intel.icecp.core.security.crypto.mac.MacScheme;
import com.intel.icecp.node.security.RandomBytesGenerator;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.node.security.crypto.cipher.symmetric.AesSymmetricCipherTest;
import com.intel.icecp.node.security.crypto.mac.hmac.HmacScheme;
import com.intel.icecp.node.security.crypto.mac.hmac.HmacSha1Scheme;
import com.intel.icecp.node.security.crypto.mac.hmac.HmacSha224Scheme;
import com.intel.icecp.node.security.crypto.mac.hmac.HmacSha256Scheme;
import com.intel.icecp.node.security.crypto.mac.hmac.HmacSha384Scheme;
import com.intel.icecp.node.security.crypto.mac.hmac.HmacSha512Scheme;
import javax.crypto.KeyGenerator;
import org.junit.Test;
import org.junit.Ignore;

/**
 * Test for {@link HmacScheme} subclasses
 * Note that only key of size 128 is allowed on system where the 
 * "Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy"
 * file is not installed. Therefore the field {@link AesSymmetricCipherTest#KEY_SIZES}
 * contains only 128.
 * 
 */
@Ignore
public class HmacSchemeTest {

    /** Random bytes string to sign*/
    private static final byte[] TO_SIGN = RandomBytesGenerator.getRandomBytes(100);
    /** Key sizes to consider */
    private static final int[] KEY_SIZES = {128};
    /** HMAC schemes to test */
    private static final MacScheme[] HMAC_SCHEMES = {new HmacSha1Scheme(), new HmacSha224Scheme(), 
        new HmacSha256Scheme(), new HmacSha384Scheme(), new HmacSha512Scheme()};
    
    
    /**
     * Utility method that creates a symmetric key of a given algorithm
     * 
     * @param keySize Size in bits of the key
     * @param algorithmType Algorithm to use
     * @return A symmetric key instance 
     * @throws Exception If creation fails
     */
    private SymmetricKey symmetricKeyGen(int keySize, String algorithmType) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(algorithmType);
        keyGen.init(keySize);
        return new SymmetricKey(keyGen.generateKey());
    }

    /**
     * Utility method that computes an HMAC on some bytes in {@link HmacSchemeTest#TO_SIGN}
     * 
     * @param mac Mac scheme to use
     * @param sk Secret key
     * @return Bytes corresponding to the MAC
     * @throws Exception In case of error
     */
    private byte[] compute(MacScheme mac, SymmetricKey sk) throws Exception {
        return mac.computeMac(TO_SIGN, sk);
    }
    
    /**
     * Utility method that verifies a given HMAC on {@link HmacSchemeTest#TO_SIGN} 
     * 
     * @param mac Mac scheme to use
     * @param sk Secret key
     * @param toVerify HMAC to verify
     * @throws Exception In case of failure
     */
    private void verify(MacScheme mac, SymmetricKey sk, byte[] toVerify) throws Exception {
        mac.verifyMac(toVerify, TO_SIGN, sk);
    }

    /**
     * Test for {@link MacScheme#computeMac(byte[], com.intel.icecp.core.security.crypto.key.SecretKey) }
     * 
     * @throws Exception 
     */
    @Test
    public void computeMacTest() throws Exception {
        for (MacScheme<? extends SecretKey> m : HMAC_SCHEMES) {
            for (int size : KEY_SIZES) {
                compute(m, symmetricKeyGen(size, m.id()));
            }
        }
    }
    
    /**
     * Test for {@link MacScheme#verifyMac(byte[], byte[], com.intel.icecp.core.security.crypto.key.SecretKey) }
     * 
     * @throws Exception 
     */
    @Test
    public void verifyMacTest() throws Exception {
        for (MacScheme<? extends SecretKey> m : HMAC_SCHEMES) {
            for (int size : KEY_SIZES) {
                SymmetricKey sk;
                sk = symmetricKeyGen(size, m.id());
                verify(m, sk, compute(m, symmetricKeyGen(size, m.id())));
            }
        }
    }

}
