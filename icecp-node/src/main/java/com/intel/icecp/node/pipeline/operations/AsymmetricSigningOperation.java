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

import com.intel.icecp.core.Message;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.metadata.formats.FormatEncodingException;
import com.intel.icecp.core.pipeline.Operation;
import com.intel.icecp.core.pipeline.exception.OperationException;
import com.intel.icecp.core.security.CryptoProvider;
import com.intel.icecp.core.security.crypto.exception.siganture.SignatureError;
import com.intel.icecp.core.security.crypto.exception.siganture.UnsupportedSignatureAlgorithmException;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import com.intel.icecp.core.security.crypto.signature.SignatureScheme;
import com.intel.icecp.core.security.trust.TrustModel;
import com.intel.icecp.core.security.trust.exception.TrustModelException;
import com.intel.icecp.node.messages.security.SignedMessage;
import com.intel.icecp.node.utils.StreamUtils;

import java.io.IOException;
import java.net.URI;

/**
 * Operation that takes a specific trust model (of type {@link TrustModel}) and signs a message, i.e., constructs a
 * {@link SignedMessage} (execute method) or verifies a message signature (executeInverse method)
 *
 */
public class AsymmetricSigningOperation extends Operation<Message, SignedMessage> {

    /** Trust model to use to fetch and verify trust in keys */
    private final TrustModel<PrivateKey, PublicKey> trustModel;

    /** Format to use to encode the message before signing it */
    private final Format format;

    private final URI signingKeyId;
    
    private final URI verifyingKeyId;
    
    private final String algorithmId;
    
    public AsymmetricSigningOperation(TrustModel<PrivateKey, PublicKey> trustModel, URI signingKeyId, URI verifyingKeyId, Format format, String algorithmId) {
        super(Message.class, SignedMessage.class);
        this.trustModel = trustModel;
        this.format = format;
        this.algorithmId = algorithmId;
        this.signingKeyId = signingKeyId;
        this.verifyingKeyId = verifyingKeyId;
        
    }

    /**
     * Fetch the signing key "from" the given trust model, then sign the message (bytes)
     * and return a {@link SignedMessage}
     * 
     * {@inheritDoc }
     */
    @Override
    public SignedMessage execute(Message inputMessage) throws OperationException {
        if (inputMessage == null) {
            throw new OperationException("AsymmetricSigningOperation singing operation failed: null input");
        }
        try {
            // Read the bytes to sign 
            byte[] inputMessageBytes = StreamUtils.readAll(format.encode(inputMessage));
            // PrivateKey is subclass of SecretKey
            PrivateKey privateKey = trustModel.fetchSigningKey(signingKeyId);
            // Retrieve the signature scheme to use from the key
            SignatureScheme signScheme = CryptoProvider.getSignatureScheme(algorithmId, false);
            // Compose the SignedMessage passing in the inputMessage, and the signature of its bytes
            return new SignedMessage(inputMessage, signScheme.sign(inputMessageBytes, privateKey));
        } catch (FormatEncodingException | IOException | UnsupportedSignatureAlgorithmException | SignatureError | TrustModelException ex) {
            throw new OperationException("AsymmetricSigningOperation singing operation failed.", ex);
        }
    }

    /**
     * Fetch the verification key from the given trust model, then verify the signature
     * and return the inner {@link Message} from the input {@link SignedMessage}.
     * 
     * {@inheritDoc }
     */
    @Override
    public Message executeInverse(SignedMessage input) throws OperationException {
        // We verify the signature, and if valid return back the message that was signed
        try {
            // Retrieve the symmetric key to use; Note that this may be a blocking operation!
            PublicKey publicKey = trustModel.fetchVerifyingKey(verifyingKeyId);
            SignatureScheme signScheme = CryptoProvider.getSignatureScheme(algorithmId, false);
            // Get the bytes to use for signature verification
            byte[] messageBytes = StreamUtils.readAll(format.encode(input.message));
            // Verify the signature (throws an Exception if not verified)
            signScheme.verify(input.signature.signatureValue, messageBytes, publicKey);
            // All OK, return the inner message
            return input.message;
        } catch (FormatEncodingException | IOException | UnsupportedSignatureAlgorithmException | SignatureError | TrustModelException ex) {
            throw new OperationException("AsymmetricSigningOperation signature verification failed.", ex);
        }
    }

}