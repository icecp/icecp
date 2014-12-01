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

package com.intel.icecp.node.management;

import com.intel.icecp.core.Module;
import com.intel.icecp.node.management.modules.A;
import com.intel.icecp.node.management.modules.B;
import com.intel.icecp.node.management.modules.C;
import com.intel.icecp.node.management.modules.DModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 */
public class ModuleClassLoaderTest {

    private static final Logger LOGGER = LogManager.getLogger();
    private ModuleClassLoader instance;

    @Before
    public void beforeTest() throws IOException {
        byte[] jarBytes = JarUtils.buildJar(A.class, B.class, C.class, DModule.class);
        instance = new ModuleClassLoader(jarBytes, null, null);
    }

    @Test
    @Ignore
    public void findModules() {
        Collection<Class<? extends Module>> modules = instance.findModules(".*");
        assertEquals(2, modules.size()); // TODO fix this; cannot load modules because system class loader has already done so in @Before
    }

    @Test
    @Ignore
    public void findModulesWithFilter() {
        Collection<Class<? extends Module>> modules = instance.findModules(".+Module");
        assertEquals(1, modules.size()); // TODO fix this; cannot load modules because system class loader has already done so in @Before
    }

    @Test
    public void checkImplementsLogic() {
        assertFalse(Module.class.isAssignableFrom(A.class));
        assertTrue(Module.class.isAssignableFrom(C.class));
        assertTrue(Module.class.isAssignableFrom(DModule.class));
    }

    @Test
    public void checkLoadedInterfaceLogic() throws Exception {
        String className = "com.intel.icecp.core.Module";

        URLClassLoader classLoader = new URLClassLoader(new URL[]{}, this.getClass().getClassLoader());
        Class<?> module0 = classLoader.loadClass(className);
        assertNotNull(module0);

        Class<?> module1 = instance.loadClass(className);
        assertNotNull(module1);
        assertEquals(module0, module1);

        ModuleClassLoader other = new ModuleClassLoader(JarUtils.buildJar(C.class), null, null);
        Class<?> module2 = other.loadClass(className);
        assertEquals(module1, module2);
    }

    @Test
    public void testSystemClassLoadingCheck() throws Exception {
        assertTrue(instance.isClassLoadedInSystem(this.getClass().getName()));
        assertFalse(instance.isClassLoadedInSystem("some.nonexistent.class.Foo"));
    }

    @Test
    public void testLoadedInterfaces() throws Exception {
        Class<?>[] cs = instance.loadClass("com.intel.icecp.node.management.modules.DModule").getInterfaces();
        assertEquals(1, cs.length);
        assertEquals(Module.class, cs[0]);
    }

    @Test
    public void testInterfaceInJarAgainstLocal() throws Exception {
        ModuleClassLoader other = new ModuleClassLoader(JarUtils.buildJar(C.class, Module.class), null, null);
        Class<?> module = other.loadClass("com.intel.icecp.core.Module");
        assertEquals(Module.class, module);

        Class<?> c = other.loadClass("com.intel.icecp.node.management.modules.C");
        assertTrue(Module.class.isAssignableFrom(c));
    }

    @Test
    public void packageNameOfClass() {
        assertEquals("a.b.c", ModuleClassLoader.packageNameOfClassname("a.b.c.D"));
        assertNull(ModuleClassLoader.packageNameOfClassname("NoPackageClass"));
        assertNull(ModuleClassLoader.packageNameOfClassname(null));
    }

}