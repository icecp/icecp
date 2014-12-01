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
