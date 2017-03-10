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
package com.intel.icecp.node.security.keymanagement;

import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.management.ConfigurationManager;
import com.intel.icecp.core.security.KeyManagers;
import com.intel.icecp.core.security.keymanagement.IcecpKeyManager;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerException;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerNotSupportedException;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.intel.icecp.core.security.keymanagement.IcecpKeyManagerProvider;

/**
 * Implementation of {@link KeyManagers}, which keeps each provider inside a 
 * {@link Map}.
 *
 */
public class KeyManagersImpl implements KeyManagers {

    
    private static final Logger LOGGER = LogManager.getLogger();
    
    /** Holds all registered providers */
    private final Map<String, IcecpKeyManagerProvider> providers = new HashMap<>();
    
    private final Channels channels;

    public KeyManagersImpl(Channels channels) {
        this.channels = channels;
    }
    
    
    /**  
     * TODO: version using {@code Configuration}; to be removed
     * 
     * {@inheritDoc }
     * 
     */
    @Override
    public boolean register(String keyManager, IcecpKeyManagerProvider provider) {
        // We do not want to replace an existing one (first unregister, 
        // than register a new one)
        if (!providers.containsKey(keyManager)) {
            providers.put(keyManager, provider);
            // Pass the configuration to this key manager provider
            return true;
        }
        return false;
    }
    

    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public void unregister(String keyManager) {
        if (providers.containsKey(keyManager)) {
            providers.remove(keyManager);
            LOGGER.info("Unregistered key manager provider for {}.", keyManager);
        }
    }
    
    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public IcecpKeyManager get(String keyManager, Attributes attributes) throws KeyManagerNotSupportedException {
        if (providers.containsKey(keyManager)) {
            try {
                return providers.get(keyManager).get(channels, attributes);
            } catch (KeyManagerException ex) {
                // Log but not handle
                LOGGER.warn("Unable to build key manager", ex);
            }
        }
        throw new KeyManagerNotSupportedException("Unsupported key manager " + keyManager);
    }

    
    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public IcecpKeyManager get(String keyManager, ConfigurationManager configuration) throws KeyManagerNotSupportedException {
        if (providers.containsKey(keyManager)) {
            try {
                return providers.get(keyManager).get(channels, configuration.get(providers.get(keyManager).id()));
            } catch (KeyManagerException ex) {
                // Log but not handle
                LOGGER.warn("Unable to build key manager", ex);
            }
        }
        throw new KeyManagerNotSupportedException("Unsupported key manager " + keyManager);
    }


    
    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public boolean hasKeyManager(String keyManager) {
        return providers.containsKey(keyManager);
    }
    
    
}
