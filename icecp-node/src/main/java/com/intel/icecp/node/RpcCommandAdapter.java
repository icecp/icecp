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