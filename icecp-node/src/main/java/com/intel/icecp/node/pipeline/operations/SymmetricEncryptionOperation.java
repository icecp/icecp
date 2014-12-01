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
package com.intel.icecp.node.pipeline.operations;

import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.pipeline.Operation;
import com.intel.icecp.core.pipeline.exception.OperationException;
import com.intel.icecp.core.security.CryptoProvider;
import com.intel.icecp.core.security.crypto.exception.cipher.CipherDecryptionError;
import com.intel.icecp.core.security.crypto.exception.cipher.CipherEncryptionError;
import com.intel.icecp.core.security.crypto.exception.cipher.UnsupportedCipherException;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.core.security.keymanagement.IcecpKeyManager;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerException;
import com.intel.icecp.node.utils.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

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
    private final IcecpKeyManager keyManager;

    public SymmetricEncryptionOperation(URI keyID, String algorithm, IcecpKeyManager keyManager) {
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
        } catch (KeyManagerException | IOException | UnsupportedCipherException | CipherEncryptionError | SecurityException | IllegalArgumentException ex) {
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
        } catch (CipherDecryptionError | UnsupportedCipherException | KeyManagerException ex) {
            throw new OperationException("SymmetricEncryptionOperation decryption failed.", ex);
        }
    }

}
