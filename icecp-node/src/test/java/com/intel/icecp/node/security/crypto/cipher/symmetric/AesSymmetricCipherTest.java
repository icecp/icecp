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

import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.node.security.RandomBytesGenerator;
import javax.crypto.KeyGenerator;

/**
 * Abstract class use for testing symmetric encryption schemes.
 * Note that only key of size 128 is allowed on system where the 
 * "Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy"
 * file is not installed. Therefore the field {@link AesSymmetricCipherTest#KEY_SIZES}
 * contains only 128.
 *
 */
public abstract class AesSymmetricCipherTest {

    /** Different key size to consider */
    protected static final int[] KEY_SIZES = {128};
    
    /** Parameters */
    protected final byte[] textToEncrypt = RandomBytesGenerator.getRandomBytes(2048);
    
    /**
     * Utility method that creates a symmetric key of a given algorithm
     * 
     * @param keySize Size in bits of the key
     * @param algorithmType Algorithm to use (e.g., AES)
     * @return A symmetric key instance 
     * @throws Exception If creation fails
     */
    protected SymmetricKey symmetricKeyGen(int keySize, String algorithmType) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(algorithmType);
        keyGen.init(keySize);
        return new SymmetricKey(keyGen.generateKey());
    }
}
