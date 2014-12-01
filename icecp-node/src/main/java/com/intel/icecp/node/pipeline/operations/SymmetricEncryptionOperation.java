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
package com.intel.icecp.node.pipeline.operations;

import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.pipeline.Operation;
import com.intel.icecp.core.pipeline.exception.OperationException;
import com.intel.icecp.core.security.CryptoProvider;
import com.intel.icecp.core.security.crypto.exception.cipher.CipherDecryptionError;
import com.intel.icecp.core.security.crypto.exception.cipher.CipherEncryptionError;
import com.intel.icecp.core.security.crypto.exception.cipher.UnsupportedCipherException;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerException;
import com.intel.icecp.node.utils.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import com.intel.icecp.core.security.keymanagement.KeyManager;

/**
 * Operation that takes as input an InputStream and produces as output a encrypted
 * message encoded as a {@link BytesMessage}
 *
 */
public class SymmetricEncryptionOperation extends Operation<InputStream, BytesMessage> {

    /** ID of key */
    private final URI keyID;
    /** ID of the algorithm to use */
    private final String algorithm;
    /** Key manager to use*/
    private final KeyManager keyManager;

    public SymmetricEncryptionOperation(URI keyID, String algorithm, KeyManager keyManager) {
        super(InputStream.class, BytesMessage.class);
        this.keyID = keyID;
        this.algorithm = algorithm;
        this.keyManager = keyManager;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public BytesMessage execute(InputStream input) throws OperationException {
        try {
            byte[] bytes = StreamUtils.readAll(input);
            // Retrieve the key from the key manager (ASSUMPTION: the key is already there)
            SymmetricKey key = keyManager.getSymmetricKey(keyID);
            // Create the encrypted message
            return new BytesMessage(CryptoProvider.getCipher(algorithm, false).encrypt(bytes, key));
        } catch (NullPointerException | KeyManagerException | IOException | UnsupportedCipherException | CipherEncryptionError | SecurityException | IllegalArgumentException ex) {
            throw new OperationException("SymmetricEncryptionOperation encryption failed.", ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public InputStream executeInverse(BytesMessage input) throws OperationException {
        try {
            SymmetricKey key = keyManager.getSymmetricKey(keyID);
            // Decrypt the bytes
            byte[] decBytes = CryptoProvider.getCipher(algorithm, false).decrypt(input.getBytes(), key);
            // Returns the decrypted data.
            return new ByteArrayInputStream(decBytes);
        } catch (NullPointerException | CipherDecryptionError | UnsupportedCipherException | KeyManagerException ex) {
            throw new OperationException("SymmetricEncryptionOperation decryption failed.", ex);
        }
    }

}
