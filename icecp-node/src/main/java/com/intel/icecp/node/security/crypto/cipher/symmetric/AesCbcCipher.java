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
import com.intel.icecp.core.security.crypto.exception.cipher.CipherDecryptionError;
import com.intel.icecp.core.security.crypto.exception.cipher.CipherEncryptionError;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

/**
 * AES CBC implementation with PKCS5 padding and use of an IV. 
 * The first 16 bytes of the produced ciphertext correspond to the IV
 *
 */
public class AesCbcCipher implements Cipher<SymmetricKey, SymmetricKey> {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Number of bytes of the IV */
    private static final int IV_SIZE = 128 / 8;

    /**
     * Generates a secure random IV of {@link AesCbcCipher#IV_SIZE} bytes
     *
     * @return IV Bytes to use as Initial Vector.
     */
    private byte[] generateIV() {
        // Secure random instance to create the IV
        SecureRandom randomGenerator = new SecureRandom();
        randomGenerator.setSeed(randomGenerator.generateSeed(IV_SIZE));
        byte[] byteIV = new byte[IV_SIZE];
        randomGenerator.nextBytes(byteIV);
        return byteIV;
    }

    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public byte[] encrypt(byte[] plaintext, SymmetricKey key, Object... other) throws CipherEncryptionError {

        // Initialization vector
        byte[] iv = null;

        // Check the key for not null
        if (key == null || plaintext == null) {
            throw new CipherEncryptionError("Error during encryption: null key or data to encrypt");
        }

        // First we check whether the IV was given as input
        if (other != null && other.length >= 1) {
            // the first parameter in this case should be the IV
            try {
                iv = (byte[]) other[0];
            } catch (ClassCastException ex) {
                LOGGER.warn("Incorrect parameter type passed to encrypt ", ex); // Do nothing, continue
            }
        }

        // If we did not receive it as input, we create it
        if (iv == null) {
            iv = generateIV();
        }

        try {
            // Try to create an instance of the javax.crypto.Cipher with AES CBC
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(SecurityConstants.AES_CBC_ALGORITHM);
            // Initialize in encryption mode + IV
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key.getWrappedKey(), new IvParameterSpec(iv));
            byte[] encryptedData = cipher.doFinal(plaintext);
            // Now, we encode the ciphertext concatenating IV || encryptedData
            byte[] ciphertext = new byte[iv.length + encryptedData.length];
            // Copy IV into ciphertext
            System.arraycopy(iv, 0, ciphertext, 0, IV_SIZE);
            // Copy the encrypted data
            System.arraycopy(encryptedData, 0, ciphertext, IV_SIZE, encryptedData.length);
            // Return the ciphertext
            return ciphertext;

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new CipherEncryptionError("Error during encryption.", ex);
        }

    }

    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public byte[] decrypt(byte[] ciphertext, SymmetricKey key, Object... other) throws CipherDecryptionError {

        byte[] iv = null, encryptedBytes = null;

        // Check the key for not null
        if (key == null || ciphertext == null) {
            throw new CipherDecryptionError("Error during decryption: null data to decrypt or key");
        }

        // Option 1: IV is passed as parameter and cihertext is in dataToDecrypt
        if (other != null && other.length >= 1) {
            // the first parameter in this case should be the IV. If works, 
            // it means that dataToDecrypt is just encrypted data bytes
            try {
                // This may cause a ClassCastException
                iv = (byte[]) other[0];
                encryptedBytes = ciphertext;
            } catch (ClassCastException ex) {
                // Do nothing
                LOGGER.trace("Attempt to cast to invalid types, {}", ex);
            }
        }
        
        if (iv == null || encryptedBytes == null) {
            // Option 2: IV and ciphertext are included into ciphertext bytes
            // First IV_SIZE bytes are IV, the rest is encrypted data
            iv = new byte[IV_SIZE];
            System.arraycopy(ciphertext, 0, iv, 0, IV_SIZE);
            encryptedBytes = new byte[ciphertext.length - IV_SIZE];
            System.arraycopy(ciphertext, IV_SIZE, encryptedBytes, 0, encryptedBytes.length);
        }
        

        try {
            // Try to create an instance of the javax.crypto.Cipher with AES CBC
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(SecurityConstants.AES_CBC_ALGORITHM);
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key.getWrappedKey(), new IvParameterSpec(iv));
            return cipher.doFinal(encryptedBytes);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new CipherDecryptionError("Error during decryption", ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String id() {
        return SecurityConstants.AES_CBC_ALGORITHM;
    }

}
