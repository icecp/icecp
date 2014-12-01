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

import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.security.crypto.exception.key.InvalidKeyTypeException;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import com.intel.icecp.core.security.keymanagement.IcecpKeyManager;
import com.intel.icecp.core.security.trust.TrustModel;
import com.intel.icecp.core.security.trust.TrustModelProvider;
import com.intel.icecp.core.security.trust.exception.TrustModelInstantiationError;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Mock trust model provider
 *
 */
public class MockTrustModelProvider implements TrustModelProvider {

    private static final Logger LOGGER = LogManager.getLogger();
    
    /** ID */
    public static final String MOCK_TRUST_MODEL_ID = "mockTrustModel";

    /**
     * {@inheritDoc }
     *
     */
    @Override
    public TrustModel<PrivateKey, PublicKey> build(IcecpKeyManager keyManager, Attributes attributes) throws TrustModelInstantiationError {
        try {
            return new MockTrustModel();
        } catch (InvalidKeyTypeException ex) {
           LOGGER.warn("Unable to instantiate {}.", MockTrustModel.class.getName(), ex);
        }
        return null;
    }

    /**
     * {@inheritDoc }
     *
     */
    @Override
    public String id() {
        return MOCK_TRUST_MODEL_ID;
    }

}
