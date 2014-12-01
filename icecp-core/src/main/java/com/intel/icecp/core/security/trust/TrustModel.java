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
