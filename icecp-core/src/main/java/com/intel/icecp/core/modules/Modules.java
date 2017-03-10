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
package com.intel.icecp.core.modules;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Module;
import com.intel.icecp.core.Module.StopReason;
import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.messages.ConfigurationMessage;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 */
public interface Modules {

    /**
     * Retrieve a module from the list of loaded modules. Access is checked with ModulePermission("module-name").
     *
     * @param id the ID of the module instance to retrieve
     * @return the {@link Module} instance
     * @throws ModuleNotFoundException if the module has not yet been loaded
     */
    ModuleInstance get(long id) throws ModuleNotFoundException;

    /**
     * @return all module instances currently loaded on the system
     */
    Collection<ModuleInstance> getAll();

    /**
     * Retrieve an (assumed) JAR file and load only the modules matching the regular expression passed as
     * moduleNameRegex.
     * <p>
     * If module implementors need to run multiple instances of their module, call this method multiple times.
     *
     * @param moduleChannel a document channel returning the bytes of a JAR file
     * @param moduleNameRegex a unique name identifying the module inside the JAR, matches the name specified on the
     * module with {@link ModuleProperty}
     * @param configurationChannel a channel for retrieving configuration to load with the module
     * @return a future that resolves to a collection of all loaded module IDs
     */
    CompletableFuture<Collection<Long>> load(Channel<BytesMessage> moduleChannel, String moduleNameRegex, Channel<ConfigurationMessage> configurationChannel);

    /**
     * Start the module in the execution pool; any thread with access to the ModulePermission("module-name") can start
     * and stop the module.
     *
     * @param id the ID of the module instance to start
     * @return a future that completes once the module is started, the result will contain the passed ID
     */
    CompletableFuture<Long> start(long id);

    /**
     * Stop the module (assumes the module implements the stop() contract correctly). Any thread with access to the
     * ModulePermission("module-name") can start and stop the module. TODO is there a way to force this? Tried with
     * Future.cancel() but this only works if the module respects InterruptedExceptions (and sleeps).
     *
     * @param id the ID of the module instance to start
     * @param reason a {@link StopReason} describing why to stop the module
     * @return a future that completes once the module is stopped
     */
    CompletableFuture<Void> stop(long id, Module.StopReason reason);

    /**
     * Stop all modules
     *
     * @param reason a {@link StopReason} describing why to stop the modules
     * @return a future that completes once all modules are stopped
     */
    CompletableFuture<Void> stopAll(Module.StopReason reason);
}
