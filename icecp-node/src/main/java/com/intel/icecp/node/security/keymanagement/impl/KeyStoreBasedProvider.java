/*
 * File name: KeyStoreBasedProvider.java
 * 
 * Purpose: 
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */
package com.intel.icecp.node.security.keymanagement.impl;

import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.security.keymanagement.IcecpKeyManager;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerException;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.core.security.keymanagement.IcecpKeyManagerProvider;

/**
 * Provider class loaded via SPI, which is used to build, initialize, and 
 * manage a key manager of type {@link KeyStoreBasedManager}.
 *
 */
public class KeyStoreBasedProvider implements IcecpKeyManagerProvider {

    /** Key manager instance */
    private KeyStoreBasedManager keyManager = null;
    
    /**
     * {@inheritDoc }
     * 
     * TODO: Move to an attribute-only initialization
     * 
     */
    @Override
    public IcecpKeyManager get(Channels keyChannels, Configuration configuration) throws KeyManagerException {
        if (keyManager == null) {
            keyManager = new KeyStoreBasedManager(keyChannels, configuration);
            // load the key manager; this operation may fail
            keyManager.load();
        }
        return keyManager;
    }
    
    /**
     * {@inheritDoc }
     * 
     * TODO: Not yet supported as {@link KeyStoreBasedManager} initialization
     * only expects {@link Configuration}
     */
    @Override
    public IcecpKeyManager get(Channels keyChannels, Attributes attributes) throws KeyManagerException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    

    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public String id() {
        return SecurityConstants.KEY_STORE_BASED_MANAGER;
    }

    
    
}
