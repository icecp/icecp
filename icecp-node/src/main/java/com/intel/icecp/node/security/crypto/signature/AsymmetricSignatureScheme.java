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
