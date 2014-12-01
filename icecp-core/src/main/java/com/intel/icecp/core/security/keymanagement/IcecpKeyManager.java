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
 *
 */
public interface IcecpKeyManager {

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
