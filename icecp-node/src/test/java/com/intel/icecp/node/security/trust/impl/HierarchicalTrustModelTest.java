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

import com.intel.icecp.core.mock.MockKeyManager;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.trust.exception.TrustModelException;
import java.net.URI;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for {@link HierarchicalTrustModel}
 *
 */
public class HierarchicalTrustModelTest {
    
    private static HierarchicalTrustModel trustModel;
    
    /**
     * Initialization
     * 
     * @throws Exception 
     */
    @BeforeClass
    public static void init() throws Exception {
        trustModel = new HierarchicalTrustModel(new MockKeyManager().init());
    }
    
    /**
     * Test for {@link HierarchicalTrustModel#fetchSigningKey(java.net.URI)  }
     * should return a valid {@link PrivateKey}
     * 
     */
    @Test
    public void fetchSigningKeyCorrectTest() throws Exception {
        trustModel.fetchSigningKey(MockKeyManager.DEFAULT_ID_PRV_KEY);
    }
    
    /**
     * Test for {@link HierarchicalTrustModel#fetchSigningKey(java.net.URI)  }
     * should throw a {@link TrustModelException} exception
     * 
     */
    @Test(expected = TrustModelException.class)
    public void fetchSigningKeyFailureTest() throws Exception {
        trustModel.fetchSigningKey(URI.create("NON_EXISTING_KEY"));
    }
    
    /**
     * Test for {@link HierarchicalTrustModel#fetchVerifyingKey(java.net.URI) }
     * should return a valid {@link PrivateKey}
     * 
     */
    @Test
    public void fetchVerifyingKeyCorrectTest() throws Exception {
        trustModel.fetchVerifyingKey(MockKeyManager.DEFAULT_CERTIFICATE);
    }
    
    /**
     * Test for {@link HierarchicalTrustModel#fetchVerifyingKey(java.net.URI) }
     * should throw a {@link TrustModelException} exception
     * 
     */
    @Test(expected = TrustModelException.class)
    public void fetchVerifyingKeyFailureTest() throws Exception {
        trustModel.fetchVerifyingKey(URI.create("NON_EXISTING_CERT"));
    }

}
