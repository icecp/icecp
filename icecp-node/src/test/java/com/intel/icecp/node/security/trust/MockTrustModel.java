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
package com.intel.icecp.node.security.trust;

import com.intel.icecp.core.security.crypto.exception.key.InvalidKeyTypeException;
import com.intel.icecp.core.security.crypto.key.asymmetric.KeyPair;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import com.intel.icecp.core.security.trust.TrustModel;
import com.intel.icecp.core.security.trust.exception.TrustModelException;
import com.intel.icecp.node.security.crypto.key.KeyProvider;
import java.net.URI;

/**
 * Mock trust model that holds a key pair
 *
 */
public class MockTrustModel implements TrustModel<PrivateKey, PublicKey> {

    /**
     * Private and public key
     */
    private final PrivateKey sk;
    private final PublicKey pk;

    public MockTrustModel() throws InvalidKeyTypeException {
        KeyPair kp = KeyProvider.generateKeyPair("RSA", 1024);
        sk = kp.getPrivateKey();
        pk = kp.getPublicKey();
    }

    /**
     * {@inheritDoc }
     *
     */
    @Override
    public PrivateKey fetchSigningKey(URI signingKeyId) throws TrustModelException {
        return sk;
    }

    /**
     * {@inheritDoc }
     *
     */
    @Override
    public PublicKey fetchVerifyingKey(URI verifyingKeyId) throws TrustModelException {
        return pk;
    }

}
