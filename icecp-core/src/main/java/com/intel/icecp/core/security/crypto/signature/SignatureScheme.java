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
package com.intel.icecp.core.security.crypto.signature;

import com.intel.icecp.core.security.crypto.key.SecretKey;
import com.intel.icecp.core.security.SecurityService;
import com.intel.icecp.core.security.crypto.exception.siganture.SignatureError;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;

/**
 * Interface for a generic signature scheme
 *
 * @param <S> Signing key type
 * @param <V> Verification key type
 */
public interface SignatureScheme<S extends SecretKey, V extends PublicKey> extends SecurityService<String> {

    /**
     * Returns the signature of a given data, using a given algorithm and key.
     *
     * @param dataToSign Bytes to sign
     * @param key Key to use for signing
     * @return The signature bytes
     * @throws SignatureError In case of error during the signing process
     */
    byte[] sign(byte[] dataToSign, S key) throws SignatureError;

    /**
     * Verifies the signature, given a public key and the algorithm; throws and
     * exception if not verified
     *
     * @param signature Signature bytes
     * @param data Data to use for signature verification
     * @param key Verification key
     * @throws SignatureError In case of error in verifying the signature
     */
    void verify(byte[] signature, byte[] data, V key) throws SignatureError;

}
