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
import com.intel.icecp.core.security.crypto.exception.mac.MacError;
import com.intel.icecp.core.security.crypto.exception.mac.UnsupportedMacAlgorithmException;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.core.security.crypto.mac.MacScheme;
import com.intel.icecp.core.security.trust.TrustModel;
import com.intel.icecp.core.security.trust.exception.TrustModelException;
import com.intel.icecp.node.messages.security.SignedMessage;
import com.intel.icecp.node.utils.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Operation that takes as input an instance of {@link InputStream} and returns a {@link SignedMessage}; "signature" is
 * performed using a MAC scheme.
 *
 */
public class MacSigningOperation extends Operation<Message, SignedMessage> {

    /**
     * Trust mode to use
     */
    protected final TrustModel<SymmetricKey, SymmetricKey> trustModel;

    /**
     * Format to use to encode the message to sign
     */
    protected final Format format;

    /**
     * ID of the key to use for signing
     */
    protected final URI keyId;
    
    /**
     * MAC scheme to use for symmetric signing
     */
    protected final String macScheme;

    public MacSigningOperation(TrustModel<SymmetricKey, SymmetricKey> trustModel, URI keyId, String macScheme, Format format) {
        super(Message.class, SignedMessage.class);
        this.trustModel = trustModel;
        this.format = format;
        this.macScheme = macScheme;
        this.keyId = keyId;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public SignedMessage execute(Message inputMessage) throws OperationException {
        if (inputMessage == null) {
            throw new OperationException("MAC generation failed: null input");
        }
        try {
            // Encode the message to get its bytes
            byte[] inputMessageBytes = StreamUtils.readAll(format.encode(inputMessage));
            // Retrieve the symmetric key via the trust model
            SymmetricKey macSymmetricKey = trustModel.fetchSigningKey(keyId);
            // Load the MAC scheme via its provider
            MacScheme signScheme = CryptoProvider.getMacScheme(macScheme, false);
            // Compose the signed message
            return new SignedMessage(inputMessage, signScheme.computeMac(inputMessageBytes, macSymmetricKey));
        } catch (FormatEncodingException | IllegalArgumentException | IOException | UnsupportedMacAlgorithmException | MacError | TrustModelException ex) {
            throw new OperationException("MAC generation failed.", ex);
        }
    }

    /**
     * We verify the MAC "signature", and if valid return back the message that was signed
     * <p>
     * {@inheritDoc }
     */
    @Override
    public Message executeInverse(SignedMessage input) throws OperationException {
        try {
            // Retrieve the symmetric key via the trust model
            SymmetricKey macSymmetricKey = trustModel.fetchVerifyingKey(keyId);
            MacScheme signScheme = CryptoProvider.getMacScheme(macScheme, false);
            // Verify the MAC (throws an Exception if not verified)
            signScheme.verifyMac(input.signature.signatureValue, StreamUtils.readAll(format.encode(input.message)), macSymmetricKey);
            // All OK, return the inner message
            return input.message;
        } catch (FormatEncodingException | IOException | TrustModelException | UnsupportedMacAlgorithmException | MacError ex) {
            throw new OperationException("MAC verification failed.", ex);
        }
    }
}