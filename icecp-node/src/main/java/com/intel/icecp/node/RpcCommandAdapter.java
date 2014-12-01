/*
 * ******************************************************************************
 *
 * INTEL CONFIDENTIAL
 *
 * Copyright 2016 Intel Corporation All Rights Reserved.
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
package com.intel.icecp.node;

import com.intel.icecp.core.Module;
import com.intel.icecp.core.Node;
import com.intel.icecp.rpc.Command;
import com.intel.icecp.rpc.RpcServer;

import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Wrapper class to invoke command messages. This would be used by a client
 * through the RpcServer call which will have these messages registered as
 * commands.
 *
 * Created by Natalie Gaston natalie.gaston@intel.com on 6/7/2016.
 */
public class RpcCommandAdapter {
    private static final Logger logger = LogManager.getLogger();
    
    private Node context;
    
    public RpcCommandAdapter(Node context){
        this.context = context;
    }

    public CompletableFuture<Collection<Long>> loadAndStartModules(String moduleUriStr, String configurationUriStr){
        CompletableFuture<Collection<Long>> ids = null;

        try {
            URI moduleUri = URI.create(moduleUriStr);
            URI configurationUri = URI.create(configurationUriStr);
            ids = context.loadAndStartModules(moduleUri, configurationUri);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to load and start with invalid URI found: moduleUri = " + moduleUriStr
                    + " configurationUri = " + configurationUriStr);
        }
        return ids;
    }

    public Object stopModule(Long moduleId) {
        return context.modules().stop(moduleId, Module.StopReason.USER_DIRECTED);
    }

    public Object stopAllModules() {
        return context.modules().stopAll(Module.StopReason.USER_DIRECTED);
    }

    /**
     * Adds commands to the RpcServer command registry.
     */
    void addCommands(RpcServer rpcServer) {
        RpcCommandAdapter adapter = new RpcCommandAdapter(context);
        Arrays.asList(adapter.getClass().getDeclaredMethods()).stream()
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .map(m -> new Command(m.getName(), adapter, m))
                .forEach(rpcServer.registry()::add);
    }
}