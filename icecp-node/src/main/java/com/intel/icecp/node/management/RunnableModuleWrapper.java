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
import com.intel.icecp.core.modules.ModuleInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

/**
 * Helper class for running module instances in the pool
 *
 */
class RunnableModuleWrapper implements Runnable {

    private static final Logger logger = LogManager.getLogger();

    public final Node node;
    public final ModuleInstance instance;
    public final CompletableFuture<Long> future;

    /**
     * @param node the node running the module
     * @param instance the wrapper object holding a module and its metadata
     * @param future to be completed with the module ID when the module starts running
     */
    RunnableModuleWrapper(Node node, ModuleInstance instance, CompletableFuture<Long> future) {
        this.node = node;
        this.instance = instance;
        this.future = future;
    }

    /**
     * Wrap the module execution inside some helpful constructs: thread renaming, try-catch, status channel publish
     */
    @Override
    public void run() {
        // This is required to support ServiceLoaders that use their default ExtensionLoaders. We set our threads
        // context loader to the ModuleClassLoader so that the ServiceLoader providers can be found.
        // a.k.a.: assert(instance.module.getClass().getClassLoader().getClass().equals(ModuleClassLoader.class));
        Thread.currentThread().setContextClassLoader(instance.module().getClass().getClassLoader());

        String oldName = Thread.currentThread().getName();
        Thread.currentThread().setName("module-" + instance);

        // alert the node that the module is now running
        future.complete(instance.id());

        // run the module
        try {
            instance.module().run(node, instance.attributes());
        } catch (Exception t) {
            logger.error("Module {} exited with an exception", instance, t);
            instance.state(Module.State.ERROR);
        }

        Thread.currentThread().setName(oldName);
    }
}

