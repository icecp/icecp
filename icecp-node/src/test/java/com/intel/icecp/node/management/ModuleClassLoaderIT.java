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
import com.intel.icecp.core.Node;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.attributes.IdAttribute;
import com.intel.icecp.node.AttributesFactory;
import com.intel.icecp.node.NodeFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Permissions;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 */
public class ModuleClassLoaderIT {

    private static final Logger LOGGER = LogManager.getLogger();
    private ModuleClassLoader instance;

    @Before
    public void beforeTest() throws IOException {
        byte[] jarBytes = retrieveExampleModuleByteCode();
        instance = new ModuleClassLoader(jarBytes, Collections.EMPTY_LIST, new Permissions());
    }

    @Ignore
    @Test
    public void findModulesTwice() {
        Collection<Class<? extends Module>> modules = instance.findModules(".+Module");
        assertEquals(1, modules.size());

        modules = instance.findModules(".+Module");
        assertEquals(1, modules.size()); // TODO fix this; classes already loaded so they are skipped in second call
    }

    /**
     * Relies on external JAR file; see {@link #retrieveExampleModuleByteCode()}. If JAR is not found this test will be
     * ignored
     *
     * @throws Exception JUnit will interpret any uncaught errors as test failures, hence the generic exception
     */
    @Test
    public void loadExampleJarFromFileSystem() throws Exception {
        Collection<Class<? extends Module>> modules = instance.findModules(".+Module");
        Module module = modules.stream().findFirst().get().newInstance();

        Node node = NodeFactory.buildMockNode();
        Attributes attributes = AttributesFactory.buildEmptyAttributes(node.channels(), URI.create("icecp:/a/b/c"));
        attributes.add(new IdAttribute(42));
        module.run(node, attributes);
    }

    /**
     * Relies on external JAR file; see {@link #retrieveExampleModuleByteCode()}. If JAR is not found this test will be
     * ignored
     *
     * @throws Exception JUnit will interpret any uncaught errors as test failures, hence the generic exception
     */
    @Test
    public void ensureModuleInterfaceInExampleJarIsSameAsSystemClass() throws Exception {
        Class<?> module = instance.loadClass("com.intel.icecp.core.Module");
        assertEquals(Module.class, module);

        Class<?> moduleImplementor = instance.loadClass("com.intel.icecp.module.example.ExampleModule");
        assertTrue(Module.class.isAssignableFrom(moduleImplementor));
    }

    /**
     * Relies on external JAR file; see {@link #retrieveExampleModuleByteCode()}. If JAR is not found this test will be
     * ignored
     *
     * @throws Exception JUnit will interpret any uncaught errors as test failures, hence the generic exception
     */
    @Test
    public void testInterfaceInAThreadPool() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        byte[] jarBytes = retrieveExampleModuleByteCode();

        Executors.newCachedThreadPool().submit(() -> {
            try {
                ModuleClassLoader other = new ModuleClassLoader(jarBytes, null, null);

                Class<?> moduleImplementor = other.loadClass("com.intel.icecp.module.example.ExampleModule");
                assertTrue(Module.class.isAssignableFrom(moduleImplementor));

                Class<?> module = other.loadClass("com.intel.icecp.core.Module");
                assertEquals(Module.class, module);

                LOGGER.info("Module interface loaded from local scope: {}", Module.class.hashCode());
                LOGGER.info("Module interface loaded from JAR scope: {}", module.hashCode());
                latch.countDown();
            } catch (Throwable e) {
                LOGGER.error("Failed to find class", e);
                fail("Failed to find module");
            }

            latch.countDown();
        });

        latch.await(30, TimeUnit.SECONDS);
    }

    /**
     * This method relies on an external JAR to be built; TODO build this automatically here. Build it with `cd example;
     * mvn package`. TODO add dependencies
     *
     * @return the JAR bytes
     * @throws IOException if the file bytes cannot be streamed
     */
    private byte[] retrieveExampleModuleByteCode() throws IOException {
        String jar = "example/target/icecp-module-example-1.0.0.jar"; // note that this relies on the example being built with
        assumeTrue(Paths.get(jar).toFile().exists());

        byte[] bytes = Files.readAllBytes(Paths.get(jar));
        LOGGER.info("Using example JAR {} with {} bytes.", jar, bytes.length);
        return bytes;
    }
}