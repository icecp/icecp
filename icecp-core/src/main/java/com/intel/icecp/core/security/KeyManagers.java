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
