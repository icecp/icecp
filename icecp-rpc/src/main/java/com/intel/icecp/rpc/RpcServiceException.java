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

/**
 */
public class RpcServiceException extends Exception {
    /**
     * RPC service related exception with {@code Throwable t} constructor
     *
     * @param t Throwable
     */
    public RpcServiceException(Throwable t) {
        super(t);
    }

    /**
     * RPC service related exception with {@code String error} message constructor
     *
     * @param error message for the exception
     */
    public RpcServiceException(String error) {
        super(error); }
}
