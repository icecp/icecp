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
package com.intel.icecp.core.security.trust;

import com.intel.icecp.core.security.crypto.key.Key;
import com.intel.icecp.core.security.crypto.key.SecretKey;
import com.intel.icecp.core.security.trust.exception.TrustModelException;
import java.net.URI;

/**
 * Abstraction of a generic Trust Model, which is meant to fetch signing and verifying keys.
 * Specific implementation should implement verification key fetching and trust verification logic.
 * 
 * @param <S>   Type of the signing key
 * @param <K>   Type of the verifying key
 */
public interface TrustModel<S extends SecretKey, K extends Key> {
    
    /**
     * Fetches and returns a secret key (symmetric or asymmetric) that can be used for
     * asymmetric and symmetric signature (we refer to MAC as signatures, even if
     * this is not the proper name for this operation)
     * 
     * @param signingKeyId Unique ID of the signing key
     * @return Returns an instance of a subclass of {@link SecretKey} representing the key to use for signature
     * @throws TrustModelException if the signing key is not found
     */
    S fetchSigningKey(URI signingKeyId) throws TrustModelException;
    
    
    /**
     * Fetches the verification key, and returns it to the caller if sufficiently trusted.
     * Key retrieval and trust evaluation depend on the specific trust model
     * 
     * @param verifyingKeyId Unique ID of the verification key
     * @return Returns an instance of a subclass of {@link Key}, representing the verification key
     * @throws TrustModelException If the verification key is not found or untrusted
     */
    K fetchVerifyingKey(URI verifyingKeyId) throws TrustModelException;
    
    
}
