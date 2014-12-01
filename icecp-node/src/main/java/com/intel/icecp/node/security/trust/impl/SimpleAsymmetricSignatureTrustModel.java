/* *****************************************************************************
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
 * *******************************************************************************
 */
package com.intel.icecp.node.security.trust.impl;

import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import com.intel.icecp.core.security.keymanagement.IcecpKeyManager;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerException;
import com.intel.icecp.core.security.trust.TrustModel;
import com.intel.icecp.core.security.trust.exception.TrustModelException;
import java.net.URI;

/**
 * Simple trust model class intended for testing/quick prototyping:
 * <ol>
 * <li> Signing key is an instance of {@link PrivateKey}, and is assumed to be 
 * fetched from the default {@link IcecpKeyManager}; </li>
 * <li> Verification key is an instance of {@link PublicKey}, and is assumed to be 
 * fetched from the default {@link IcecpKeyManager}; </li>
 * <li> Verification key is searched only in the supplied {@link IcecpKeyManager}, 
 * and is assumed to be trusted. </li>
 * </ol>
 * 
 */
public class SimpleAsymmetricSignatureTrustModel implements TrustModel <PrivateKey, PublicKey>{

    /** Key manager instance*/
    private final IcecpKeyManager keyManager;

    
    public SimpleAsymmetricSignatureTrustModel(IcecpKeyManager keyManager) {
        this.keyManager = keyManager;
    }
   
    /**
     * {@inheritDoc }
     */
    @Override
    public PrivateKey fetchSigningKey(URI signingKeyId) throws TrustModelException {
        try {
            // Retrieve the secret (private) key from the key manager
            return keyManager.getPrivateKey(signingKeyId);
        } catch (KeyManagerException ex) {
            throw new TrustModelException("Unable to find private key with ID " + signingKeyId.toString(), ex);
        }
                
    }
    
    /**
     * {@inheritDoc }
     */
    @Override
    public PublicKey fetchVerifyingKey(URI verifyingKeyId) throws TrustModelException {
        try {
            // Retrieve the public key from the key manager
            return keyManager.getPublicKey(verifyingKeyId);
        } catch (KeyManagerException ex) {
            throw new TrustModelException("Unable to find public key with ID " + verifyingKeyId.toString(), ex);
        }
    }

}
