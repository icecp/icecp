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
