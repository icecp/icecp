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
