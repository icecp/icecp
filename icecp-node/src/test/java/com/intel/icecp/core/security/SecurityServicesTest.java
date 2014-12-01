/* *****************************************************************************
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
 * *******************************************************************************
 */
package com.intel.icecp.core.security;

import com.intel.icecp.core.channels.Token;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link SecurityServices}
 *
 */
public class SecurityServicesTest {
    
    /** Security service provider instance */
    private final SecurityServices<String, MockSecurityServiceInterface> serviceProvider = 
            new SecurityServices<>(Token.of(MockSecurityServiceInterface.class));
    
    private final Class[] classes = {MockSecurityServiceImpl.class};
    
    @Before
    public void init() throws Exception {
         SecurityServicesTestUtils.createConfigurationFile(classes, MockSecurityServiceInterface.class);
    }
    
   /**
    * Test for {@link SecurityServices#get(java.lang.String, boolean) }
    * Checks for not null service loading
     * @throws java.lang.Exception
    */
   @Test
   public void getTest() throws Exception {
       // Try to laod the mock service implementation. The outcome should not be null
       Assert.assertNotNull(serviceProvider.get(MockSecurityServiceImpl.class.getName(), false));
   }
    
}
