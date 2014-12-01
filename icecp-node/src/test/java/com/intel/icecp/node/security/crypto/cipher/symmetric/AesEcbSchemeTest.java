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
package com.intel.icecp.node.security.crypto.cipher.symmetric;

import com.intel.icecp.core.security.crypto.cipher.Cipher;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.node.security.SecurityConstants;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link AesEcbCipher}.
 * 
 */
public class AesEcbSchemeTest extends AesSymmetricCipherTest {
    
    /** AES-ECB cipher */
    protected final Cipher aesEcb = new AesEcbCipher();
    
    /**
     * Test for {@link AesEcbCipher#decrypt(byte..., com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey, java.lang.Object...) }
     * 
     * @throws Exception 
     */
    @Test
    public void aesEcbEncTest() throws Exception {
        for (int size : KEY_SIZES) {
            Assert.assertNotNull(aesEcb.encrypt(textToEncrypt, symmetricKeyGen(size, SecurityConstants.AES)));
        }
    }
        
    /**
     * Test for {@link AesEcbCipher#encrypt(byte..., com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey, java.lang.Object...) }
     * 
     * @throws Exception 
     */
    @Test
    public void aesEcbDecTest() throws Exception {
        for (int size : KEY_SIZES) {
            SymmetricKey sk = symmetricKeyGen(size,SecurityConstants.AES);
            Assert.assertArrayEquals(aesEcb.decrypt(aesEcb.encrypt(textToEncrypt, sk), sk), textToEncrypt);
        }
    }
}
