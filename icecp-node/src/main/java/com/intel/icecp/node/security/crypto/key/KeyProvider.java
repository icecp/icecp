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
