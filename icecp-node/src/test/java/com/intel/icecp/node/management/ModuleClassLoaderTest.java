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