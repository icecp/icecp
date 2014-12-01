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
