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

import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import com.intel.icecp.core.security.trust.TrustModel;
import com.intel.icecp.core.security.trust.TrustModelProvider;
import com.intel.icecp.core.security.trust.exception.TrustModelInstantiationError;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.core.security.keymanagement.KeyManager;

/**
 * Provider class for {@link HierarchicalTrustModel}
 *
 */
public class HierarchicalTrustModelProvider implements TrustModelProvider {

    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public TrustModel<PrivateKey, PublicKey> build(KeyManager keyManager, Attributes attributes) throws TrustModelInstantiationError {
        return new HierarchicalTrustModel(keyManager);
    }

    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public String id() {
        return SecurityConstants.HYERARCHICAL_TRUST_MODEL;
    }
    
}
