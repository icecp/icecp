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
package com.intel.icecp.core.security.keymanagement;

import com.intel.icecp.core.security.keymanagement.exception.KeyManagerException;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import java.net.URI;
import java.security.cert.Certificate;

/**
 * Key management interface for ICECP It provides APIs to retrieve and store keys
 * and certificates, and to verify certificates validity;
 */
public interface KeyManager {

    /**
     * Retrieves the public key corresponding to a given identifier
     *
     * @param keyId Unique string identifier for the public key
     * @return The public key, if available
     * @throws KeyManagerException In case of error doing key lookup
     */
    PublicKey getPublicKey(URI keyId) throws KeyManagerException;

    /**
     * Retrieves the private key corresponding to a given identifier
     *
     * @param keyId Unique string identifier of the private key
     * @return The public key, if available
     * @throws KeyManagerException In case of error doing key lookup
     */
    PrivateKey getPrivateKey(URI keyId) throws KeyManagerException;

    /**
     * Retrieves the symmetric key corresponding to a given name
     *
     * @param keyId Unique string identifier of the symmetric key
     * @return The public key, if available
     * @throws KeyManagerException In case of error doing key lookup
     */
    SymmetricKey getSymmetricKey(URI keyId) throws KeyManagerException;

    /**
     * Stores a given symmetric key under the given identifier.
     *
     * @param keyId The identifier to use 
     * @param k Symmetric key to store
     * @throws KeyManagerException In case of error
     */
    void addSymmetricKey(URI keyId, SymmetricKey k) throws KeyManagerException;

    /**
     * Deletes the symmetric key with a given identifier.
     *
     * @param keyId Key identifier
     * @throws KeyManagerException In case of error
     */
    void deleteSymmetricKey(URI keyId) throws KeyManagerException;

    /**
     * Retrieves the certificate with a given id; looks it up from the given
     * channel if necessary
     *
     * @param certificateID Unique certificate identifier 
     * @return The certificate corresponding to the given identifier
     * @throws KeyManagerException In case of error (e.g., certificate not found, certificate not valid, etc.)
     */
    Certificate getCertificate(URI certificateID) throws KeyManagerException;
    
    /**
     * Verifies the given certificates chain (bytes)
     *
     * @param certificate Encoded certificate chain
     * @return The first certificate of the given key chain
     * @throws KeyManagerException In case of error in verifying the certificate chain
     */
    Certificate verifyCertificateChain(byte[] certificate) throws KeyManagerException;

}
