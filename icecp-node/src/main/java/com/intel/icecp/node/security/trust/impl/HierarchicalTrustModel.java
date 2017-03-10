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
import java.security.cert.Certificate;

/**
 * Implementation of a basic hierarchical {@link TrustModel} that:
 * <ol>
 * <li> Retrieves the signing key from a given {@link IcecpKeyManager} </li>
 * <li> Retrieves the verification key from the local key manager only if its 
 * associated certificate is verified against key manager trust base. </li>
 * </ol>
 * 
 */
public class HierarchicalTrustModel implements TrustModel<PrivateKey, PublicKey> {

    /** Key Manager instance*/
    private final IcecpKeyManager keyManager;
    
    
    public HierarchicalTrustModel(IcecpKeyManager keyManager) {
        this.keyManager = keyManager;
    }

    
    /**
     * {@inheritDoc }
     */
    @Override
    public PrivateKey fetchSigningKey(URI signingKeyId) throws TrustModelException {
        try {
            return keyManager.getPrivateKey(signingKeyId);
        } catch(KeyManagerException ex) {
            throw new TrustModelException("Unable to fetch signing key " + signingKeyId + ".", ex);
        }
    }

    /**
     * In this case, the ID of the verification key is a certificate ID
     * 
     * {@inheritDoc }
     */
    @Override
    public PublicKey fetchVerifyingKey(URI certificateId) throws TrustModelException {
        try {
            // Try to fetch and verify the certificate
            Certificate cert = keyManager.getCertificate(certificateId);
            // If the certificate has been fetched and verified, we can return the 
            // corresponding public key wrapped into a PublicKey class
            return new PublicKey(cert.getPublicKey());
        } catch(KeyManagerException ex) {
            throw new TrustModelException("Trust verification for certificate " + certificateId + " failed.", ex);
        }
    }
    
    
    
}
