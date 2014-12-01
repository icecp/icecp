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

