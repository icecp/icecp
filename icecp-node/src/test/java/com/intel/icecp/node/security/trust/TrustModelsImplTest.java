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
