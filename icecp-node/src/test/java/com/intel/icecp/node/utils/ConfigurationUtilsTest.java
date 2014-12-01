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

package com.intel.icecp.node.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 */
public class ConfigurationUtilsTest {

    @Test
    public void testIsSandboxEnabled() {
        assertTrue(ConfigurationUtils.isSandboxEnabled());
        System.setProperty("icecp.sandbox", "disabled");
        assertFalse(ConfigurationUtils.isSandboxEnabled());
    }

    @Test
    public void testGetPermissionsPath(){
        assertNotNull(ConfigurationUtils.getPermissionsPath());
        System.setProperty("icecp.permissions", "some-path");
        assertEquals("some-path", ConfigurationUtils.getPermissionsPath().toString());
    }

    @Test
    public void testGetConfigurationPath(){
        assertNotNull(ConfigurationUtils.getConfigurationPath());
        System.setProperty("icecp.configuration", "some-path");
        assertEquals("some-path", ConfigurationUtils.getConfigurationPath().toString());
    }
}