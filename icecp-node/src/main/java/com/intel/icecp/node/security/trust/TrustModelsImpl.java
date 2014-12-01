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
import com.intel.icecp.core.security.TrustModels;
import com.intel.icecp.core.security.crypto.key.Key;
import com.intel.icecp.core.security.crypto.key.SecretKey;
import com.intel.icecp.core.security.trust.TrustModel;
import com.intel.icecp.core.security.trust.TrustModelProvider;
import com.intel.icecp.core.security.trust.exception.TrustModelInstantiationError;
import java.util.HashMap;
import java.util.Map;
import com.intel.icecp.core.security.keymanagement.KeyManager;

/**
 * Implementation of {@link TrustModels}; providers are maintained in a
 * {@link HashMap}.
 *
 */
public class TrustModelsImpl implements TrustModels {

    /**
     * Available providers
     */
    private final Map<String, TrustModelProvider> providers = new HashMap<>();

    /**
     * Key manager to pass in to trust model provider
     */
    private final KeyManager keyManager;

    public TrustModelsImpl(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    /**
     * {@inheritDoc }
     *
     */
    @Override
    public boolean register(String providerId, TrustModelProvider provider) {
        if (!providers.containsKey(providerId)) {
            providers.put(providerId, provider);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc }
     *
     */
    @Override
    public boolean unregister(String providerId) {
        // True if we removed the provider
        return providers.remove(providerId) != null;
    }

    /**
     * Simply returns the element, if present (note that {@link HashMap} returns
     * {@literal null} if it doesn't exists)
     * 
     * {@inheritDoc }
     *
     */
    @Override
    public TrustModelProvider get(String providerId) {
        return providers.get(providerId);
    }

    /**
     * Creates the trust model if the corresponding provider is present in
     * the list of available implementations. If not, throws an exception.
     * 
     * {@inheritDoc }
     *
     */
    @Override
    public <S extends SecretKey, V extends Key> TrustModel<S, V> createTrustModel(String providerId, Attributes attributes) throws TrustModelInstantiationError {
        if (providers.containsKey(providerId)) {
            return providers.get(providerId).build(keyManager, attributes);
        }
        throw new TrustModelInstantiationError("The given trust model provider " + providerId + " does not exist");
    }

    
    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public KeyManager keyManager() {
        return keyManager;
    }

    
    
    
}
