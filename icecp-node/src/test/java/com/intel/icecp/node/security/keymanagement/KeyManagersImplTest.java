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
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.core.security.keymanagement.IcecpKeyManager;
import com.intel.icecp.core.security.keymanagement.IcecpKeyManagerProvider;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerException;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerNotSupportedException;
import com.intel.icecp.node.AttributesFactory;
import java.net.URI;
import java.security.cert.Certificate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for class {@link KeyManagersImpl}
 *
 */
public class KeyManagersImplTest {

    /**
     * Key manager instance
     */
    private KeyManagersImpl keyManager;

    /**
     * Mock key manager
     */
    class MockKeyManager implements IcecpKeyManager {

        @Override
        public PublicKey getPublicKey(URI keyId) throws KeyManagerException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public PrivateKey getPrivateKey(URI keyId) throws KeyManagerException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public SymmetricKey getSymmetricKey(URI keyId) throws KeyManagerException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void addSymmetricKey(URI keyId, SymmetricKey k) throws KeyManagerException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void deleteSymmetricKey(URI keyId) throws KeyManagerException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Certificate getCertificate(URI certificateID) throws KeyManagerException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Certificate verifyCertificateChain(byte[] certificate) throws KeyManagerException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    /**
     * Mock key manager provider
     */
    class MockKeyManagerProvider implements IcecpKeyManagerProvider {


        @Override
        public IcecpKeyManager get(Channels channels, Configuration configuration) throws KeyManagerException {
            return new MockKeyManager();
        }
        
        @Override
        public IcecpKeyManager get(Channels channels, Attributes attributes) throws KeyManagerException {
            return new MockKeyManager();
        }

        @Override
        public String id() {
            return this.getClass().getCanonicalName();
        }

    }
    /** Provider ID */
    private static final String KEY_MANAGER_PROVIDER_ID = MockKeyManagerProvider.class.getCanonicalName();

    /**
     * Initialization code
     */
    @Before
    public void init() {
        keyManager = new KeyManagersImpl(new MockFileOnlyChannels());
    }

    /**
     * Test for {@link KeyManagersImpl#register(java.lang.String, com.intel.icecp.core.security.keymanagement.IcecpKeyManagerProvider, com.intel.icecp.core.attributes.Attributes)
     * }
     * Should work correctly
     *
     */
    @Test
    public void registerTestAttributes() {
        // Create empty attributes
        keyManager.register(KEY_MANAGER_PROVIDER_ID, new MockKeyManagerProvider());
        Assert.assertTrue(keyManager.hasKeyManager(KEY_MANAGER_PROVIDER_ID));
    }

    /**
     * Test for {@link KeyManagersImpl#unregister(java.lang.String) }
     * This test passes iif register works
     *
     */
    @Test
    public void unregisterTest() {
        keyManager.register(KEY_MANAGER_PROVIDER_ID, new MockKeyManagerProvider());
        keyManager.unregister(KEY_MANAGER_PROVIDER_ID);
        Assert.assertFalse(keyManager.hasKeyManager(KEY_MANAGER_PROVIDER_ID));
    }

    /**
     * Test for {@link KeyManagersImpl#get(java.lang.String) }
     * Should return an instance of {@link MockKeyManager}
     * This test passes iif register works
     *
     * @throws Exception
     */
    @Test
    public void getCorrectTest() throws Exception {
        Attributes attributes = AttributesFactory.buildEmptyAttributes(null, null);
        keyManager.register(KEY_MANAGER_PROVIDER_ID, new MockKeyManagerProvider());
        Assert.assertTrue(keyManager.get(KEY_MANAGER_PROVIDER_ID, attributes).getClass().equals(MockKeyManager.class));
    }

    /**
     * Test for {@link KeyManagersImpl#get(java.lang.String) }
     * Should fail
     *
     * @throws KeyManagerNotSupportedException
     */
    @Test(expected = KeyManagerNotSupportedException.class)
    public void getNonExistingTest() throws KeyManagerNotSupportedException {
        Attributes attributes = AttributesFactory.buildEmptyAttributes(null, null);
        keyManager.register(KEY_MANAGER_PROVIDER_ID, new MockKeyManagerProvider());
        keyManager.get("NON_EXISTING_PROVIDER", attributes);
    }
}
