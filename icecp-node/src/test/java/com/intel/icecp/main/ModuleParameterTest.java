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

package com.intel.icecp.main;

import org.junit.Test;

import java.net.URI;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class ModuleParameterTest {

    @Test
    public void testBuild() throws Exception {
        ModuleParameter option = ModuleParameter.build("a.jar");

        assertTrue(option.modulePath.toString().startsWith("file:"));
        assertTrue(option.modulePath.toString().endsWith("a.jar"));
        assertNotNull(option.configurationPath);
    }

    @Test
    public void testBuildWithScheme() throws Exception {
        ModuleParameter option = ModuleParameter.build("a.jar%ndn:/a/b/c");

        assertNotNull(option.modulePath);
        assertEquals("ndn:/a/b/c", option.configurationPath.toString());
    }

    @Test
    public void testUriConversionOnWindowsFileSystem() throws Exception {
        URI uri = ModuleParameter.toUri("C:/some-jar");

        assertEquals("file", uri.getScheme());
    }

    @Test
    public void testLocateConfiguration(){
        URI configurationUri = ModuleParameter.locateDefaultConfigurationUri(Paths.get("C:/path/to/jar").toUri());

        assertTrue(configurationUri.toString().endsWith("to/config.json"));
    }
}