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
package com.intel.icecp.core.security;

import com.intel.icecp.core.security.keymanagement.IcecpKeyManagerProvider;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.management.ConfigurationManager;
import com.intel.icecp.core.security.keymanagement.IcecpKeyManager;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerNotSupportedException;

/**
 * Interface to create and use key managers ({@link IcecpKeyManager}), starting from
 * their registered {@link IcecpKeyManagerProvider}s.
 * In principle, this is similar to (but way simpler than) 
 * {@link com.intel.icecp.core.management.Channels}.
 * 
 * Should be used as follows:
 * 
 * <p>
 * <pre><code>
 * KeyManagers managers = ...; // Some implementation
 * ...
 * managers.register(SOME_KEY_MANAGER_PROVIDER, provider);
 * ...
 * IcecpKeyManager km = managers.get(SOME_KEY_MANAGER_PROVIDER, attributes);
 * ...
 * </code></pre>
 *
 */
public interface KeyManagers {
    
    /**
     * Register a new key manager provider, which could be later used to
     * build the key manager (uses {@link ConfigurationManager})
     * 
     * @param keyManager String that uniquely identifies a key manager
     * @param provider Reference to a key manager provider
     * @return {@literal true} if the provider has been registered; {@literal false} otherwise
     */
    boolean register(String keyManager, IcecpKeyManagerProvider provider);
    
    /**
     * Tells whether the given key manager has been registered
     * 
     * @param keyManager Key manager to check
     * @return {@literal True} if the given key manager is registered; 
     * {@literal False} otherwise
     */
    boolean hasKeyManager(String keyManager);
    
    
    /**
     * Unregisters a key manager provider
     * 
     * @param keyManager 
     */
    void unregister(String keyManager);
    
    /**
     * Retrieves a key manager with given id.
     * 
     * @param keyManager Key manager ID
     * @param attributes Attributes containing options for the key manager
     * @return An instance of key manager
     * @throws KeyManagerNotSupportedException If the key manager cannot be instantiated
     */
    IcecpKeyManager get(String keyManager, Attributes attributes) throws KeyManagerNotSupportedException;
    
    
    /**
     * Retrieves a key manager with given id.
     * 
     * @param keyManager Key manager ID
     * @param configuration Configuration containing options for the key manager
     * @return An instance of key manager
     * @throws KeyManagerNotSupportedException If the key manager cannot be instantiated
     */
    IcecpKeyManager get(String keyManager, ConfigurationManager configuration) throws KeyManagerNotSupportedException;
    
}
