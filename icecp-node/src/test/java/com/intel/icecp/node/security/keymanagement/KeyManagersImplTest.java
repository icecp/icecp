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
