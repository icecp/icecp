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

import com.intel.icecp.common.TestModule;
import com.intel.icecp.core.Attribute;
import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Module;
import com.intel.icecp.core.Node;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.management.ConfigurationManager;
import com.intel.icecp.core.management.ModulePermissions;
import com.intel.icecp.core.management.PermissionsManager;
import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.messages.ConfigurationMessage;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.mock.MockChannels;
import com.intel.icecp.core.modules.ModuleInstance;
import com.intel.icecp.core.modules.ModuleLoadException;
import com.intel.icecp.core.modules.ModuleNotFoundException;
import com.intel.icecp.node.NodeFactory;
import com.intel.icecp.node.management.modules.C;
import com.intel.icecp.node.management.modules.DModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 */
public class ModulesImplTest {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String EXAMPLE_MODULE_DIR_NAME = "example";
    private static byte[] exampleJarBytes;
    private final PermissionsManager permissionsManager = mock(PermissionsManager.class);
    private final ConfigurationManager configurationManager = mock(ConfigurationManager.class);
    private ModulesImpl instance;
    private Permissions permissions;
    private Node node;

    @BeforeClass
    public static void beforeClass() {
        try {
            File jarFile = createJarFile();
            if (jarFile != null) {
                Path jarFilePath = jarFile.toPath();
                LOGGER.info("Find example JAR file {}", jarFilePath);
                exampleJarBytes = Files.readAllBytes(jarFilePath);
            }
        } catch (IOException ex) {
            LOGGER.warn("Skipping " + ModulesImplTest.class.getName() + " tests");
            assumeTrue(false); // if we can't load the resource, skip the tests
        }
    }

    private static File createJarFile() throws IOException {
        String examplePomFileName = EXAMPLE_MODULE_DIR_NAME + "/pom.xml";
        try (Reader pomFileReader = new FileReader(examplePomFileName)) {
            MavenXpp3Reader pomReader = new MavenXpp3Reader();
            Model pomModel = pomReader.read(pomFileReader);
            // figure out the jar file version:
            String jarVersion = pomModel.getVersion();
            // module example jar file name
            String jarFileName = "icecp-module-example-" + jarVersion + ".jar";
            Runtime.getRuntime().exec("mvn clean install -f " + examplePomFileName);
            return new File(EXAMPLE_MODULE_DIR_NAME + "/target/" + jarFileName);
        } catch (XmlPullParserException e) {
            LOGGER.error("Failed to parse example module pom file {} and exeception: {}", examplePomFileName, e);
            throw new IOException("Failed to parse example module pom file " + examplePomFileName);
        }
    }

    /**
     * Given a key, value, and configuration channel, publish a ConfigurationMessage to this channel
     *
     * @param key Config message key
     * @param value Config message value
     * @param configChannel Channel on which to publish
     * @throws com.intel.icecp.core.misc.ChannelIOException
     */
    static void publishConfigItem(final String key, final String value, final Channel<ConfigurationMessage> configChannel) throws com.intel.icecp.core.misc.ChannelIOException {
        ConfigurationMessage configurationMessage = new ConfigurationMessage();
        configurationMessage.put(key, value);
        configChannel.publish(configurationMessage);
    }

    /**
     * Open a channel (by uri, using a Channels provider) and publish a BytesMessage to it.
     *
     * @param channels channel provider
     * @param uri      URI of channel to open
     * @param bytes    bytes to put into a BytesMessage and publish
     * @return the opened channel
     * @throws ChannelIOException if we cannot open the channel or cannot publish to it
     */
    static Channel<BytesMessage> openAndPublishBytesMessage(final Channels channels,
                                                            final URI uri,
                                                            final byte[] bytes)
            throws ChannelIOException {
        Channel<BytesMessage> jarChannel = null;
        try {
            jarChannel = channels.openChannel(uri, BytesMessage.class, Persistence.DEFAULT);
        } catch (ChannelLifetimeException e) {
            throw new ChannelIOException("Encountered exception opening channel", e);
        }
        jarChannel.publish(new BytesMessage(bytes));
        return jarChannel;
    }

    @Before
    public void beforeTest() throws Exception {
        node = NodeFactory.buildMockNode();

        // configuration manager should return a mock configuration
        when(configurationManager.get(any())).thenReturn(mock(Configuration.class));

        // permissions manager should return a ModulePermissions that returns _permissions_ when getPermissions is called
        ModulePermissions modulePermissions = mock(ModulePermissions.class);
        permissions = new Permissions();
        when(modulePermissions.getPermissions()).thenReturn(permissions);
        when(permissionsManager.retrievePermissions(any())).thenReturn(modulePermissions);

        instance = new ModulesImpl(node, permissionsManager, configurationManager);
    }

    @Test(expected = Error.class)
    public void testNoModulesConfigThrowsError() throws ChannelIOException {
        Configuration mockConfig = mock(Configuration.class);
        Mockito.doThrow(new ChannelIOException("oops")).when(mockConfig).load();
        when(configurationManager.get("modules")).thenReturn(mockConfig);
        new ModulesImpl(node, permissionsManager, configurationManager);
    }

    @Test
    public void testModulesConfigIsLoaded() throws ChannelIOException {
        Configuration mockConfig = mock(Configuration.class);
        when(configurationManager.get("modules")).thenReturn(mockConfig);
        new ModulesImpl(node, permissionsManager, configurationManager);
        verify(mockConfig, times(1)).load();
    }

    @Test(expected = ModuleNotFoundException.class)
    public void testNotFoundModule() throws Exception {
        instance.get(1);
    }

    @Test
    public void testLoadingModule() throws Exception {
        ModuleInstance moduleInstance = instance.loadFromClass(TestModule.class);

        assertEquals("test-module", moduleInstance.name());
        assertEquals(TestModule.class, moduleInstance.module().getClass());
        assertEquals(Module.State.LOADED, moduleInstance.state());
    }

    @Test
    public void testStartingMultipleModules() throws Exception {
        long id1 = instance.loadFromClass(TestModule.class).id();
        long id2 = instance.loadFromClass(TestModule.class).id();
        assertEquals(2, instance.getAll().size());

        CompletableFuture.allOf(instance.start(id1), instance.start(id2)).get(2, TimeUnit.SECONDS);

        instance.stopAll(Module.StopReason.USER_DIRECTED).get(2, TimeUnit.SECONDS);
        assertEquals(0, instance.getAll().size());
    }

    @Test
    public void testHomeDirectoryConversion() {
        String homeDirectory = instance.convertHomeDirectory("file:~/empty");
        assertTrue(homeDirectory.matches("file:.{2,}empty"));
    }

    @Test
    public void testHomeDirectoryConversionPassThrough() {
        String homeDirectory = instance.convertHomeDirectory("other:~/empty");
        assertEquals("other:~/empty", homeDirectory);
    }

    @Test
    public void instantiateExampleFilesystemModule() throws Exception {
        List<ModuleInstance> moduleInstances = new ArrayList<>(instance.instantiateModules(Arrays.asList(C.class, DModule.class), new ConfigurationMessage()));
        assertEquals(2, moduleInstances.size());

        Attributes attributesC = moduleInstances.get(0).attributes();
        assertEquals(3, attributesC.size());
        assertTrue(attributesC.has("id"));
        assertTrue(attributesC.has("name"));
        assertTrue(attributesC.has("state"));

        Attributes attributesD = moduleInstances.get(1).attributes();
        assertEquals(4, attributesD.size());
        assertTrue(attributesD.has("id"));
        assertTrue(attributesD.has("name"));
        assertTrue(attributesD.has("state"));
        assertTrue(attributesD.has("test"));
    }

    @Test
    public void testStartingAndStoppingModule() throws Exception {
        long id = instance.loadFromClass(TestModule.class).id();

        CompletableFuture<Module.State> stateFuture = new CompletableFuture<>();
        instance.get(id).attributes().observe("state", (name, oldValue, newValue) ->
                stateFuture.complete((Module.State) newValue));

        CompletableFuture<Long> started = instance.start(id);
        started.get(2, TimeUnit.SECONDS);

        assertEquals(Module.State.RUNNING, stateFuture.get(2, TimeUnit.SECONDS));

        CompletableFuture<Void> stopped = instance.stop(id, Module.StopReason.USER_DIRECTED);
        stopped.get(2, TimeUnit.SECONDS);
    }

    @Test
    public void retrieveModuleName() throws Exception {
        assertEquals("test-module", instance.retrieveModuleNameFromAnnotation(TestModule.class));
    }

    @Test(expected=NullPointerException.class)
    public void testNullConstructorThrowsNPE() {
        new ModulesImpl(null, null, null);
    }

    @Test(expected=AssertionError.class)
    public void testBuildUriNullNodeNegativeIdViolatesAssertion() throws ModuleLoadException {
        ModulesImpl.buildUri(null, -70L);
    }

    @Test(expected=NullPointerException.class)
    public void testBuildUriNullNodeZeroIdThrowsNullPointerException() throws ModuleLoadException {
        ModulesImpl.buildUri(null, 0L);
    }

    @Test
    public void testModuleClassIsProtectedByPermissionsFromExampleInFilesystem() throws Exception {
        when(configurationManager.get(any())).thenReturn(mock(Configuration.class));
        ModulePermissions modulePermissions = mock(ModulePermissions.class);
        Permissions permissions = new Permissions();
        when(modulePermissions.getPermissions()).thenReturn(permissions);
        when(permissionsManager.retrievePermissions(any())).thenReturn(modulePermissions);
        instance = new ModulesImpl(NodeFactory.buildMockNode(), permissionsManager, configurationManager);

        Collection<Class<? extends Module>> modules = instance.classLoadModules(exampleJarBytes,
                ModulesImpl.DEFAULT_MODULE_CLASS_FILTER, permissionsManager);
        assertEquals(1, modules.size());
    }

    @Test
    public void testExampleModuleReceivesConfigurationAttribute() throws Exception {
        final String CONFIG_KEY = "example";
        final String CONFIG_VALUE = "hello world";
        final URI MODULE_OUTPUT_URI = new URI("icecp:/example/module/output");
        final URI JAR_URI = new URI("mock:/location/of/jar");
        final URI CONFIG_URI = new URI("mock:/location/of/configuration");

        MockChannels mockChannels = new MockChannels();
        Channel<BytesMessage> jarChannel = openAndPublishBytesMessage(mockChannels, JAR_URI, exampleJarBytes);

        // create channel for configuration and publish configuration to it
        Channel<ConfigurationMessage> configChannel = mockChannels.openChannel(CONFIG_URI,
                ConfigurationMessage.class, Persistence.DEFAULT);
        publishConfigItem(CONFIG_KEY, CONFIG_VALUE, configChannel);

        // load and start module
        long moduleId = instance.load(jarChannel, ModulesImpl.DEFAULT_MODULE_CLASS_FILTER, configChannel).
                get().stream().findFirst().get();
        instance.start(moduleId).get();

        // open a channel to receive notification from example module about the config it's reading
        Channel<BytesMessage> channel = node.channels().
                openChannel(MODULE_OUTPUT_URI, BytesMessage.class, Persistence.DEFAULT);
        await().until(() -> instance.retrieveLatestFrom(channel).isDone());

        // check whether the config contains our configuration
        assertTrue(new String(instance.retrieveLatestFrom(channel).get().getBytes(), "UTF-8").
                contains("'" + CONFIG_KEY + "' provided: " + CONFIG_VALUE));
    }

    @Test
    public void testLoadFilesystemModuleWithLoadChannelRegexConfiguration() throws Exception {
        MockChannels mockChannels = new MockChannels();
        Channel<BytesMessage> jarChannel = openAndPublishBytesMessage(mockChannels, new URI("mock:/location/of/jar"), exampleJarBytes);
        Channel<ConfigurationMessage> configChannel = mockChannels.openChannel(new URI("mock:/location/of/configuration"),
                ConfigurationMessage.class, Persistence.DEFAULT);
        ConfigurationMessage configurationMessage = new ConfigurationMessage();
        configurationMessage.put("test1", "test2");
        configChannel.publish(configurationMessage);

        CompletableFuture<Collection<Long>> futureLongs = instance.load(jarChannel, ModulesImpl.DEFAULT_MODULE_CLASS_FILTER, configChannel);
        Collection<Long> longs = futureLongs.get();

        assertTrue(longs.size() > 0);

        instance.start(longs.stream().findFirst().get()).get();
    }

    @Test
    public void retrieveModuleAttributes() throws Exception {
        Collection<Class<? extends Attribute>> expectedAttributes = instance.retrieveAttributesFromAnnotation(TestModule.class);
        assertEquals(1, expectedAttributes.size());
        assertEquals(TestModule.TestAttribute.class, expectedAttributes.stream().findFirst().get());
    }

}