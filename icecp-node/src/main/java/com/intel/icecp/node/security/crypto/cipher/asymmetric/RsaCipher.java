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
package com.intel.icecp.node.security.crypto.cipher.asymmetric;

import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.core.security.crypto.cipher.Cipher;
import com.intel.icecp.core.security.crypto.exception.cipher.CipherDecryptionError;
import com.intel.icecp.core.security.crypto.exception.cipher.CipherEncryptionError;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Thin wrapper around RSA crypto APIs provided by {@link javax.crypto.Cipher}
 *
 */
public class RsaCipher implements Cipher<PublicKey, PrivateKey> {

    /**
     * {@inheritDoc }
     */
    @Override
    public byte[] encrypt(byte[] dataToEncrypt, PublicKey key, Object... other) throws CipherEncryptionError {
        try {
            // Obtain a RSA cipher instance from javax.crypto
            final javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(SecurityConstants.RSA_ALGORITHM);
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key.getPublicKey());
            // Finally return the ciphertext
            return cipher.doFinal(dataToEncrypt);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException ex) {
            throw new CipherEncryptionError("Error during encryption.", ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public byte[] decrypt(byte[] dataToDecrypt, PrivateKey key, Object... other) throws CipherDecryptionError {
        try {
            // Obtain a RSA cipher instance from javax.crypto
            final javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(SecurityConstants.RSA_ALGORITHM);
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key.getKey());
            // Finally return
            return cipher.doFinal(dataToDecrypt);

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException ex) {
            throw new CipherDecryptionError("Error during decryption.", ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String id() {
        return SecurityConstants.RSA_ALGORITHM;
    }

}
