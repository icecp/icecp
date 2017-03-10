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

import com.intel.icecp.core.Attribute;
import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.Module;
import com.intel.icecp.core.Node;
import com.intel.icecp.core.attributes.AttributeNotFoundException;
import com.intel.icecp.core.attributes.AttributeRegistrationException;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.attributes.CannotInstantiateAttributeException;
import com.intel.icecp.core.attributes.IdAttribute;
import com.intel.icecp.core.attributes.ModuleStateAttribute;
import com.intel.icecp.core.attributes.NameAttribute;
import com.intel.icecp.core.event.Event;
import com.intel.icecp.core.management.ConfigurationManager;
import com.intel.icecp.core.management.PermissionsManager;
import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.messages.ConfigurationMessage;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.metadata.formats.BytesFormat;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.misc.DependencyNotFoundException;
import com.intel.icecp.core.modules.ModuleInstance;
import com.intel.icecp.core.modules.ModuleLoadException;
import com.intel.icecp.core.modules.ModuleNotFoundException;
import com.intel.icecp.core.modules.ModuleProperty;
import com.intel.icecp.core.modules.Modules;
import com.intel.icecp.core.permissions.ModulePermission;
import com.intel.icecp.node.AttributesFactory;
import com.intel.icecp.node.channels.ndn.NdnChannelProvider;
import com.intel.icecp.node.utils.ChannelUtils;
import com.intel.icecp.node.utils.SecurityUtils;
import com.intel.icecp.request_response.impl.HashRequestor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.security.Permissions;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Manage and run modules. The module JAR loaded by this manager must contain a MANIFEST.MF file which includes: <ul>
 * <li>a ModuleMavenCoordinates attribute</li> <li>a Class-Path attribute containing a space delimited list of
 * dependency JARs</li> </ul> Refer to the ModuleClassLoader docs and the sample module for more info, including its
 * pom.xml file.
 * <p>
 * This node sub-system is configured in "modules.json"; it uses the following properties: <ul> <li>moduleClassFilter:
 * for limiting the search when finding Module implementations in a JAR</li> <li>retrieveUnderMs: the time (in
 * milliseconds) this class will wait for retrieval of dependencies</li> <li>dependencyChannels: an array of the
 * locations this class will for dependencies</li> </ul>
 * <p>
 * <p>
 * TODO: need to incorporate {@link Event}s into this TODO: need to remove ConfigurationManager and specify
 * configurations as parameters
 *
 */
public class ModulesImpl implements Modules {

    public static final String DEFAULT_MODULE_CLASS_FILTER = ".+Module";
    private static final List<String> DEFAULT_DEPENDENCY_CHANNELS = Collections.singletonList("file:~/.m2/repository"); // TODO this should actually do better checking to use the Maven localRepository setting
    private static final int DEFAULT_RETRIEVE_UNDER_MS = 60000;
    private static final Logger LOGGER = LogManager.getLogger();
    private final ConcurrentMap<Long, ModuleInstance> instances = new ConcurrentHashMap<>();
    private final Random randomGenerator = new SecureRandom();
    private final Node node;
    private final Configuration configuration;
    private final ExecutorService pool;
    private final PermissionsManager permissionsManager;

    /**
     * @param node the node on which to run modules
     * @param permissionsManager interface for retrieving module permissions
     * @param configurationManager interface for retrieving module configurations
     */
    public ModulesImpl(Node node, PermissionsManager permissionsManager, ConfigurationManager configurationManager) {
        this.node = node;
        this.permissionsManager = permissionsManager;
        this.pool = Executors.newCachedThreadPool();

        //	read the configuration file, TODO use attributes for this
        this.configuration = configurationManager.get("modules");
        try {
            configuration.load();
        } catch (ChannelIOException ex) {
            LOGGER.error("Error reading module manager configuration file: ", ex);
            throw new Error(ex);
        }

        // set seed
        randomGenerator.setSeed(System.nanoTime());
    }

    /**
     * @param node the node containing the module
     * @param moduleId the module ID
     * @return the canonical URI for accessing a module instance and its attributes
     */
    public static URI buildUri(Node node, long moduleId) throws ModuleLoadException {
        assert moduleId >= 0;
        return ChannelUtils.join(node.getDefaultUri(), "modules", Long.toString(moduleId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModuleInstance get(long id) throws ModuleNotFoundException {
        ModuleInstance instance = instances.get(id);
        if (instance == null) {
            throw new ModuleNotFoundException("Failed to find module ID: " + id);
        }
        SecurityUtils.checkPermission(new ModulePermission(instance.name(), "access"));

        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ModuleInstance> getAll() {
        SecurityUtils.checkPermission(new ModulePermission("*", "access"));
        return instances.values();
    }

    /**
     * Retrieve a module JAR from the provided URI; helper method for {@link #load(Channel, String, Channel)}.
     *
     * @param moduleUri the location of the module to retrieve
     * @param configurationUri the location of the configuration to retrieve
     * @return a future that resolves once the module is loaded and assigned an ID
     * @throws ChannelLifetimeException if it fails to open the channel for retrieving the module JAR bytes
     */
    public CompletableFuture<Collection<Long>> load(URI moduleUri, URI configurationUri) throws ChannelLifetimeException {
        return load(moduleUri, DEFAULT_MODULE_CLASS_FILTER, configurationUri);
    }

    /**
     * Retrieve a module JAR from the provided URI; helper method for {@link #load(Channel, String, Channel)}.
     *
     * @param moduleUri the location of the module to retrieve
     * @param moduleNameRegex a regex pattern of modules to match
     * @param configurationUri the location of the configuration to retrieve
     * @return a future that resolves once the module is loaded and assigned an ID
     * @throws ChannelLifetimeException if it fails to open the channel for retrieving the module JAR bytes
     */
    private CompletableFuture<Collection<Long>> load(URI moduleUri, String moduleNameRegex, URI configurationUri) throws ChannelLifetimeException {
        Channel<BytesMessage> moduleChannel = node.openChannel(moduleUri, BytesMessage.class, new Persistence(), new BytesFormat());
        Channel<ConfigurationMessage> configurationChannel = node.openChannel(configurationUri, ConfigurationMessage.class, new Persistence(), new JsonFormat<>(ConfigurationMessage.class));
        return load(moduleChannel, moduleNameRegex, configurationChannel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Collection<Long>> load(Channel<BytesMessage> channel, String moduleNameRegex, Channel<ConfigurationMessage> configurationChannel) {
        assert channel.isOpen();
        assert configurationChannel.isOpen();

        CompletableFuture<Collection<Class<? extends Module>>> modules =
                retrieveLatestFrom(channel).thenApplyAsync(jarBytes -> classLoadModulesOrReturnEmpty(jarBytes.getBytes(), moduleNameRegex, permissionsManager), pool);
        CompletableFuture<ConfigurationMessage> singleConfig =
                retrieveLatestFrom(configurationChannel);
        CompletableFuture<Collection<ModuleInstance>> loadedModules =
                CompletableFuture.allOf(modules, singleConfig).thenApplyAsync(m -> instantiateModules(modules.getNow(null), singleConfig.getNow(null)), pool);
        return loadedModules.thenApplyAsync(moduleInstances -> moduleInstances.stream().filter(mi -> mi != null).map(ModuleInstance::id).collect(Collectors.toList()), pool);
    }

    <T extends Message> CompletableFuture<T> retrieveLatestFrom(Channel<T> channel) {
        try {
            return channel.latest();
        } catch (ChannelIOException e) {
            CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    Collection<ModuleInstance> instantiateModules(Collection<Class<? extends Module>> classes, ConfigurationMessage config) {
        return classes.stream().map(moduleClass -> {
            long id = createModuleId();
            try {
                Attributes attributes = assembleAttributes(moduleClass, id, config);
                return loadFromClass(moduleClass, attributes);
            } catch (ModuleLoadException e) {
                LOGGER.error("Failed to load module: " + moduleClass, e);
                return null;
            }
        }).collect(Collectors.toList());
    }

    /**
     * Load a module from an in-memory class; at this stage, permissions must have been applied by the class loader.
     * This is a node-level operation protected by {@code ModulePermission("start")}. This method should be maintained
     * as it may be exposed in the {@link Modules} API in the future.
     *
     * @param moduleClass the class of the module to load
     * @param attributes the configuration for the module to load
     * @return the new instance ID of the loaded class
     * @throws ModuleLoadException if the module fails to load
     */
    ModuleInstance loadFromClass(Class<? extends Module> moduleClass, Attributes attributes) throws ModuleLoadException {
        LOGGER.debug("Loading new module: {}", moduleClass);
        String moduleName = retrieveModuleName(attributes);
        SecurityUtils.checkPermission(new ModulePermission(moduleName, "start"));

        ModuleInstance moduleInstance = ModuleInstance.create(moduleClass, attributes);
        instances.put(moduleInstance.id(), moduleInstance);
        moduleInstance.state(Module.State.LOADED);
        LOGGER.debug("Loaded new module: {}", moduleInstance);
        return moduleInstance;
    }

    String retrieveModuleName(Attributes attributes) throws ModuleLoadException {
        try {
            return attributes.get(NameAttribute.class);
        } catch (AttributeNotFoundException e) {
            throw new ModuleLoadException("Expected 'name' attribute could not be found in module attributes", e);
        }
    }

    /**
     * Helper class for {@link #loadFromClass(Class, Attributes)}; it allows an in-memory module to be loaded without
     * any bootstrap configuration applied. This method should be maintained as it may be exposed in the {@link Modules}
     * API in the future.
     *
     * @param moduleClass the class of the module to load
     * @return the new instance ID of the loaded class
     * @throws ModuleLoadException if the module fails to load
     */
    public ModuleInstance loadFromClass(Class<? extends Module> moduleClass) throws ModuleLoadException {
        Attributes attributes = assembleAttributes(moduleClass, createModuleId(), new ConfigurationMessage());
        return loadFromClass(moduleClass, attributes);
    }

    Attributes assembleAttributes(Class<? extends Module> m, long id, ConfigurationMessage singleConfig) throws ModuleLoadException {
        try {
            Attributes a = AttributesFactory.buildAttributesFromMap(node.channels(), buildUri(node, id), singleConfig, retrieveAttributesFromAnnotation(m));
            a.add(new NameAttribute(retrieveModuleNameFromAnnotation(m)));
            a.add(new IdAttribute(id));
            a.add(new ModuleStateAttribute());
            return a;
        } catch (AttributeRegistrationException | CannotInstantiateAttributeException e) {
            throw new ModuleLoadException("Unable to create module attributes for: " + m, e);
        }
    }

    Collection<Class<? extends Module>> classLoadModulesOrReturnEmpty(byte[] jarBytes, String moduleFilterRegex, PermissionsManager permissionsManager) {
        try {
            return classLoadModules(jarBytes, moduleFilterRegex, permissionsManager);
        } catch (ModuleLoadException e) {
            LOGGER.error("Failed to find any modules in JAR", e);
            return Collections.emptyList();
        }
    }

    /**
     * Examine a JAR and discover all modules found inside of it. This method will not load modules for use with {@link
     * #start(long)}; for that operation, see {@link #loadFromClass(Class, Attributes)}.
     *
     * @param jarBytes the bytes of the JAR
     * @param moduleFilterRegex a filter to limit the search for modules to file names matching this pattern
     * @param permissionsManager for retrieving permissions for a module
     * @return a promise to return the list of class-loaded modules
     */
    Collection<Class<? extends Module>> classLoadModules(byte[] jarBytes, String moduleFilterRegex, PermissionsManager permissionsManager) throws ModuleLoadException {
        SecurityUtils.checkPermission(new ModulePermission("*", "start"));

        try {
            // load classes without permissions to retrieve names from annotations; DO NOT ALLOW USERS TO TOUCH THESE CLASSES
            ModuleClassLoader unprotectedLoader = new ModuleClassLoader(jarBytes, null, null);
            Collection<Class<? extends Module>> unprotectedModuleClasses = unprotectedLoader.findModules(moduleFilterRegex);

            // skip further execution if no modules found
            if (unprotectedModuleClasses.isEmpty()) {
                LOGGER.warn("No module classes found in JAR, aborting");
                return Collections.emptyList();
            }

            // grab dependencies
            List<IcecpJarFileInfo> dependencies = retrieveDependenciesFromMaven(jarBytes);
            LOGGER.debug("Found {} dependencies in JAR", dependencies.size());

            // load each module with its own class loader and permission set
            Collection<Class<? extends Module>> loadedModules = new ArrayList<>();
            for (Class<? extends Module> unprotectedModuleClass : unprotectedModuleClasses) {
                String moduleName = retrieveModuleNameFromAnnotation(unprotectedModuleClass);
                Permissions permissions = permissionsManager.retrievePermissions(moduleName).getPermissions();
                ModuleClassLoader moduleClassloader = new ModuleClassLoader(jarBytes, dependencies, permissions);

                @SuppressWarnings("unchecked")
                Class<? extends Module> moduleClass = (Class<? extends Module>) moduleClassloader.loadClass(unprotectedModuleClass.getName());
                loadedModules.add(moduleClass);
            }

            return loadedModules;
        } catch (Throwable ex) {
            throw new ModuleLoadException("Failed to load module from JAR bytes", ex);
        }
    }

    /**
     * Retrieve the module name from the {@link ModuleProperty} annotated on the class
     *
     * @param moduleClass the module class
     * @param <T> ensure the class extends {@link Module}
     * @return the module name
     * @throws ModuleLoadException if the module does not have the {@link ModuleProperty} annotation
     */
    <T extends Module> String retrieveModuleNameFromAnnotation(Class<T> moduleClass) throws ModuleLoadException {
        ModuleProperty moduleProperty = moduleClass.getAnnotation(ModuleProperty.class);
        if (moduleProperty == null) {
            throw new ModuleLoadException("No ModuleProperty found on " + moduleClass.getName() + ", unable to determine assigned name.");
        }
        return moduleProperty.name();
    }

    <T extends Module> Collection<Class<? extends Attribute>> retrieveAttributesFromAnnotation(Class<T> moduleClass) throws ModuleLoadException {
        ModuleProperty moduleProperty = moduleClass.getAnnotation(ModuleProperty.class);
        if (moduleProperty == null) {
            throw new ModuleLoadException("No ModuleProperty found on " + moduleClass.getName() + ", unable to determine expected attributes.");
        }
        return Arrays.asList(moduleProperty.attributes());
    }


    /**
     * @return a module ID unique to this {@link Modules} instance
     */
    private long createModuleId() {
        long id;
        do {
            id = Math.abs(randomGenerator.nextLong());
        } while (instances.containsKey(id));
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Long> start(long id) {
        CompletableFuture<Long> future = new CompletableFuture<>();
        ModuleInstance instance;

        try {
            instance = get(id);
        } catch (ModuleNotFoundException ex) {
            LOGGER.error("Failed to find a loaded module with ID: {}", id);
            future.completeExceptionally(ex);
            return future;
        }

        SecurityUtils.checkPermission(new ModulePermission(instance.name(), "start"));
        LOGGER.info("Starting module: {}", instance.name());

        RunnableModuleWrapper wrapper = new RunnableModuleWrapper(node, instance, future);
        pool.submit(wrapper);
        return future;
    }

    /**
     * Stop the module (assumes the module implements the stop() contract correctly). Any thread with access to the
     * {@link ModulePermission#ModulePermission(String, String)}. Investigated ways to force stop but the only sure
     * mechanisms involve running modules in separate processes; attempted with Future.cancel() but this only works if
     * the module respects InterruptedExceptions (and happens to sleep). And {@link Thread#stop()} is deprecated.
     */
    @Override
    public CompletableFuture<Void> stop(long id, Module.StopReason reason) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        ModuleInstance instance;

        try {
            instance = get(id);
        } catch (ModuleNotFoundException ex) {
            LOGGER.error("Failed to find a loaded module with ID: {}", id);
            future.completeExceptionally(ex);
            return future;
        }

        SecurityUtils.checkPermission(new ModulePermission(instance.name(), "stop"));
        LOGGER.info("Stopping module: {}", instance);

        pool.submit(() -> {
            /**
             * Unlike RunnableModuleWrapper, the context class loader does not need to be set here; in fact, it
             * should not be because the module may have chained its own loader to ModuleClassLoader;
             * also, we avoid setting the thread name in case the module has set its own.
             */

            try {
                instance.module().stop(reason);
                instances.remove(id);
                future.complete(null);
            } catch (Exception e) {
                LOGGER.error("Module {} failed to shut down correctly.", instance, e);
            }
        });

        return future;
    }

    /**
     * Stop all modules
     */
    @Override
    public CompletableFuture<Void> stopAll(Module.StopReason reason) {
        List<CompletableFuture> futures = instances.keySet().stream().map(moduleId -> stop(moduleId, reason)).collect(Collectors.toList());

        CompletableFuture[] futuresArray = futures.toArray(new CompletableFuture[futures.size()]);
        return CompletableFuture.allOf(futuresArray);
    }

    /**
     * Collect all JAR bytes for dependencies in a module JAR; pull the dependency list from the specified jar file;
     * locate each dependency and copy it into the {@code List<byte[]>}. See {@link
     * #retrieveDependencyFromAvailableChannels(com.intel.icecp.node.management.MavenProject)}
     * <p>
     * Algorithm for finding the dependencies: look through the list of channels given in the "
     *
     * @param bytes the bytes for the base JAR
     * @return a list of the JAR files found
     */
    private List<IcecpJarFileInfo> retrieveDependenciesFromMaven(byte[] bytes) {
        MavenProject[] projects = JarUtils.parseDependencies(bytes);
        List<IcecpJarFileInfo> responses = new ArrayList<>();
        for (MavenProject project : projects) {
            try {
                BytesMessage message = retrieveDependencyFromAvailableChannels(project);
                responses.add(new IcecpJarFileInfo(project, message.getBytes()));
            } catch (DependencyNotFoundException e) {
                LOGGER.error("Failed to find dependency: {}", project, e);
            }
        }
        return responses;
    }

    /**
     * Use the list of available dependency channels given in the "dependencyChannels" configuration property to search
     * for the dependency bytes; the search is performed from left to right
     *
     * @param dependency the dependency to look for
     * @return the message containing all the bytes of the dependency JAR
     * @throws DependencyNotFoundException if the JAR is not found in any dependency
     */
    private BytesMessage retrieveDependencyFromAvailableChannels(MavenProject dependency) throws DependencyNotFoundException {
        int retrieveUnderMs = configuration.getOrDefault(DEFAULT_RETRIEVE_UNDER_MS, "retrieveUnderMs");
        Persistence persistence = new Persistence(retrieveUnderMs);
        List<String> dependencyLocations = configuration.getOrDefault(DEFAULT_DEPENDENCY_CHANNELS, "dependencyChannels");

        for (String channelName : dependencyLocations) {
            URI uri = URI.create(convertHomeDirectory(channelName));
            LOGGER.debug("Attempting to retrieve dependencies from channel {}", uri);

            try {
                // TODO Decide how to create these URIs.  Do we to use Maven or not
                // TODO why is NDN special?
                BytesMessage bytesMessage;
                if (channelName.startsWith(NdnChannelProvider.SCHEME)) {
                    HashRequestor<BytesMessage, BytesMessage> hashRequestor = new HashRequestor<>(node.channels(), BytesMessage.class, BytesMessage.class);
                    BytesMessage msgLookupJar = new BytesMessage(dependency.toMavenCoordinate().getBytes());
                    CompletableFuture<BytesMessage> future = hashRequestor.request(uri, msgLookupJar);
                    bytesMessage = future.get();
                    hashRequestor.stop();
                } else {
                    uri = URI.create(convertHomeDirectory(channelName) + "/" + dependency.toUrlFragment());
                    Channel<BytesMessage> channel = node.openChannel(uri, BytesMessage.class, persistence);
                    bytesMessage = channel.latest().get(retrieveUnderMs, TimeUnit.MILLISECONDS);
                    channel.close(); // need to close the channels, otherwise they stay around.
                }

                return bytesMessage;
            } catch (Exception ex) {
                LOGGER.error("Failed to retrieve {} from {}", dependency, uri, ex);
            }
        }

        throw new DependencyNotFoundException();
    }

    /**
     * If the location contains a "~", then substitute that for the "users home directory". Support Linux and Windows
     * properties. Always convert backslashes to forward slashes.
     *
     * @param location the URI of a place to retrieve dependencies
     * @return the possibly converted location with home directory included
     */
    String convertHomeDirectory(String location) {
        if (location.startsWith("file:")) {
            String userHome = System.getProperty("user.home");
            if (userHome.startsWith("/")) {
                return location.replace("~", userHome);
            } else {
                return location.replace("~", "/" + userHome.replace("\\", "/"));
            }
        }
        return location.replace("\\", "/");
    }
}
