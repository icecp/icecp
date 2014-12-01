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
package com.intel.icecp.node.security.crypto.key;

import com.intel.icecp.core.security.crypto.exception.key.InvalidKeyTypeException;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.core.security.crypto.key.asymmetric.KeyPair;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

/**
 * Generator for symmetric and asymmetric keys
 *
 */
public class KeyProvider {

    /**
     * Generates and returns a public key pair
     *
     * @param algorithm Algorithm to use (e.g., AES)
     * @param keyLength Size of the key in bits
     * @return An instance of KeyPair containing the keys
     * @throws InvalidKeyTypeException In case of invalid type
     */
    public static KeyPair generateKeyPair(String algorithm, int keyLength) throws InvalidKeyTypeException {
        java.security.KeyPairGenerator keyPairGenerator;
        KeyPair pair = new KeyPair();
        try {
            // Generate the public key via java.security APIs
            keyPairGenerator = java.security.KeyPairGenerator.getInstance(algorithm);
            keyPairGenerator.initialize(keyLength, new SecureRandom());
            java.security.KeyPair keyPair = keyPairGenerator.generateKeyPair();

            /** Extract wrapped keys */
            pair.setPublicKey(new PublicKey(keyPair.getPublic()));
            pair.setPrivateKey(new PrivateKey(keyPair.getPrivate()));
            // And finally return the newly generated key pair
            return pair;
        } catch (NoSuchAlgorithmException ex) {
            throw new InvalidKeyTypeException("Unable to create key pair for algorithm " + algorithm + ".", ex);
        }
    }
    
    
    /**
     * Creates a symmetric key for a given algorithm and a given key size
     *
     * @param algorithm Algorithm to use for the key
     * @param size Key size in bits
     * @return An instance of {@link SymmetricKey}
     * @throws NoSuchAlgorithmException In case of unsupported algorithm
     */
    private static SymmetricKey createSymmetricKey(String algorithm, int size) throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
        keyGen.init(size);
        return new SymmetricKey(keyGen.generateKey());
    }

    /**
     * Creates a key from key bytes
     *
     * @param algorithm Algorithm to use for the key
     * @param key Key bytes
     * @return An instance of {@link SymmetricKey} from bytes
     */
    private static SymmetricKey createSymmetricKey(String algorithm, byte[] key) {
        return new SymmetricKey(new SecretKeySpec(key, 0, key.length, algorithm));
    }
    
    /**
     * Creates a new symmetric key, given the algorithm and the size in bits.
     *
     * @param algorithm Algorithm to use (e.g., AES)
     * @param bits Size of the key (in bits)
     * @return An instance of SymmetricKye subclass
     * @throws InvalidKeyTypeException In case of invalid key type
     */
    public static SymmetricKey generateSymmetricKey(String algorithm, int bits) throws InvalidKeyTypeException {
        // Do nothing, we fall back to software-only key generation
        try {
            // Create the symmetric key
            return createSymmetricKey(algorithm, bits);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidKeyTypeException("Unable to create key of type " + algorithm + " of size " + bits + ".", e);
        }
    }

    /**
     * Creates a new symmetric key for the given algorithm, an with a default
     * key size, specified in {@link SecurityConstants#DEFAULT_SYMM_KEY_SIZE}
     *
     * @param algorithm Algorithm to use (e.g., AES)
     * @return An instance of {@link SymmetricKey}
     * @throws InvalidKeyTypeException In case of invalid key type
     */
    public static SymmetricKey generateSymmetricKey(String algorithm) throws InvalidKeyTypeException {
        return generateSymmetricKey(algorithm, SecurityConstants.DEFAULT_SYMM_KEY_SIZE);
    }

    /**
     * Generated a SymmetricKey instance from existing one in bytes
     *
     * @param algorithm  Algorithm to use (e.g., AES)
     * @param keyBytes Bytes of an existing symmetric key
     * @return An instance of {@link SymmetricKey}
     */
    public static SymmetricKey generateSymmetricKey(String algorithm, byte[] keyBytes) {
        // Generator to use
        return createSymmetricKey(algorithm, keyBytes);
    }

}
