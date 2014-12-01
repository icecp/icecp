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

import com.intel.icecp.core.security.trust.exception.TrustModelInstantiationError;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link TrustModelsImpl}
 *
 */
public class TrustModelsImplTest {
    
    private TrustModelsImpl trustModels;
    
    /**
     * Initializations
     * 
     */
    @Before
    public void initialize() {
        trustModels = new TrustModelsImpl(null);
    }

    
    /**
     * Test for {@link TrustModelsImpl#register(java.lang.String, com.intel.icecp.core.security.trust.TrustModelProvider) }
     * This works as far as {@link TrustModelsImpl#get(java.lang.String) } works
     * 
     */
    @Test
    public void registerTest() {
        boolean res = trustModels.register(MockTrustModelProvider.MOCK_TRUST_MODEL_ID, new MockTrustModelProvider());
        Assert.assertNotNull(trustModels.get(MockTrustModelProvider.MOCK_TRUST_MODEL_ID));
        Assert.assertTrue(res);
    }
    
    /**
     * Test for {@link TrustModelsImpl#createTrustModel(java.lang.String, com.intel.icecp.core.attributes.Attributes) }
     * This should work
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void createTrustModelCorrectTest() throws Exception {
        trustModels.register(MockTrustModelProvider.MOCK_TRUST_MODEL_ID, new MockTrustModelProvider());
        trustModels.createTrustModel(MockTrustModelProvider.MOCK_TRUST_MODEL_ID, null);
    }
    
    
    /**
     * Test for {@link TrustModelsImpl#createTrustModel(java.lang.String, com.intel.icecp.core.attributes.Attributes) }
     * This should fail
     * 
     * @throws Exception 
     */
    @Test(expected = TrustModelInstantiationError.class)
    public void createTrustModelFailureTest() throws Exception {
        trustModels.createTrustModel(MockTrustModelProvider.MOCK_TRUST_MODEL_ID, null);
    }
    
}
