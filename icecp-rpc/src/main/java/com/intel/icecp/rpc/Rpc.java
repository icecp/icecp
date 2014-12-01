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

package com.intel.icecp.rpc;

import java.net.URI;

import com.intel.icecp.core.management.Channels;

/**
 *
 */
public class Rpc {
    private Rpc(){};
    
    /**
     * factory method to create RpcClient instance
     *
     * @param channels the method name
     * @param uri uri for target node/module
     */
    public static RpcClient newClient(Channels channels, URI uri){
        return new RpcClientImpl(channels, uri);
    }
    
    /**
     * factory method to create RpcServer instance
     *
     * @param channels the method name
     * @param uri uri for target node/module
     */
    public static RpcServer newServer(Channels channels, URI uri){
        return new RpcServerImpl(channels, uri);
    }
}
