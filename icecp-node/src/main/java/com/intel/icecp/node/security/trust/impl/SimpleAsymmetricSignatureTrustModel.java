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
