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
