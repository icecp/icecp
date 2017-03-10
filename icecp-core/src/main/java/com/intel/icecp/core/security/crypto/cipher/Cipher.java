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
package com.intel.icecp.core.security.crypto.cipher;

import com.intel.icecp.core.security.SecurityService;
import com.intel.icecp.core.security.crypto.key.Key;
import com.intel.icecp.core.security.crypto.exception.cipher.CipherDecryptionError;
import com.intel.icecp.core.security.crypto.exception.cipher.CipherEncryptionError;

/**
 * Generic interface of a cipher
 *
 * @param <E> Encryption key type
 * @param <D> Decryption key type
 */
public interface Cipher<E extends Key, D extends Key> extends SecurityService<String> {

    /**
     * Encrypts the bytes in input with a given key
     *
     * @param dataToEncrypt Bytes to encrypt
     * @param key Key to use for encryption
     * @param other Other input parameters (if needed by the specific cipher)
     *
     * @return the encrypted bytes, or null in case of any error
     * @throws CipherEncryptionError In case of encryption error
     */
    byte[] encrypt(byte[] dataToEncrypt, E key, Object... other) throws CipherEncryptionError;

    /**
     * Decrypts the given encrypted bytes
     *
     * @param dataToDecrypt Bytes to encrypt
     * @param key Key to use for decryption
     * @param other Other input parameters (if needed by the specific cipher)
     *
     * @return the decrypted bytes, or null in case of any error
     * @throws CipherDecryptionError In case of decryption error
     */
    byte[] decrypt(byte[] dataToDecrypt, D key, Object... other) throws CipherDecryptionError;

}
