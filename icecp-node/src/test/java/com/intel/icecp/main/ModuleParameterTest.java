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