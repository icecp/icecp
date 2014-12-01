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
package com.intel.icecp.node.security.crypto.signature;

import com.intel.icecp.core.security.crypto.signature.SignatureScheme;
import com.intel.icecp.core.security.crypto.exception.siganture.SignatureError;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;

/**
 * Generic asymmetric signature scheme; it uses a {@link PrivateKey} as a signing key,
 * and a {@link PublicKey} as a verification key.
 *
 */
public abstract class AsymmetricSignatureScheme implements SignatureScheme<PrivateKey, PublicKey> {

    /**
     * {@inheritDoc }
     */
    @Override
    public byte[] sign(byte[] dataToSign, PrivateKey key) throws SignatureError {
        try {
            Signature signatureScheme = Signature.getInstance(this.id());
            // Pass the key to the scheme
            signatureScheme.initSign(key.getKey());
            // Pass the data and produce the signature
            signatureScheme.update(dataToSign);
            return signatureScheme.sign();
        } catch (NoSuchAlgorithmException | ClassCastException | InvalidKeyException | java.security.SignatureException ex) {
            throw new SignatureError("Unable to compute the signature of the given message.", ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void verify(byte[] signature, byte[] data, PublicKey key) throws SignatureError {
        try {
            Signature signatureScheme = Signature.getInstance(this.id());
            // Pass the public key to the scheme
            signatureScheme.initVerify(key.getPublicKey());
            // Pass the data and verify the signature
            signatureScheme.update(data);
            signatureScheme.verify(signature);
        } catch (NoSuchAlgorithmException | ClassCastException | InvalidKeyException | java.security.SignatureException ex) {
            throw new SignatureError("Unable to verify the signature of the given message", ex);
        }
    }

}
