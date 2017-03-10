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

import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.core.security.keymanagement.IcecpKeyManager;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerException;
import com.intel.icecp.core.security.trust.TrustModel;
import com.intel.icecp.core.security.trust.exception.TrustModelException;
import java.net.URI;

/**
 * Simple {@link TrustModel} for symmetric signing that fetches the symmetric 
 * key used for both signing and verification from a given {@link IcecpKeyManager}.
 * Keys are trusted by default.
 *
 */
public class SimpleSymmetricSignatureTrustModel implements TrustModel<SymmetricKey, SymmetricKey>{

    /** Key manager */
    private final IcecpKeyManager keyManager;
    
    public SimpleSymmetricSignatureTrustModel(IcecpKeyManager keyManager) {
        this.keyManager = keyManager;
    }
    
    /**
     * Fetches the symmetric key identified by keyId from the key manager
     * 
     * @return The symmetric key for signing/verifying
     * @throws TrustModelException In case of null keyManager or key not found
     */
    private SymmetricKey fetchKey(URI keyId) throws TrustModelException {
        try {
            return keyManager.getSymmetricKey(keyId);
        } catch (KeyManagerException ex) {
            throw new TrustModelException("Unable to fetch symmetric key " + keyId, ex);
        }
    }
    
    /**
     * {@inheritDoc }
     */
    @Override
    public SymmetricKey fetchSigningKey(URI signingKeyId) throws TrustModelException {
        return fetchKey(signingKeyId);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public SymmetricKey fetchVerifyingKey(URI verifyingKeyId) throws TrustModelException {
        return fetchKey(verifyingKeyId);
    }
    
    
    
}
