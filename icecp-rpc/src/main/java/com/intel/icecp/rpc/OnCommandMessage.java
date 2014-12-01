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

import com.intel.icecp.core.Module;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Interface used to process commands received by the module
 *
 * @param <M> Module supporting this interface
 */
@FunctionalInterface
public interface OnCommandMessage<M extends Module, T> {
    /**
     * Callback this interface implements when a message is received
     *
     * @param context of the Module in which the message was received
     * @return Optional response message, if a response was requested
     */
    T onCommandMessage(M context) throws Exception;

    /**
     * Default implementation of executing the onCommandMessage asynchronously
     * in a pool
     *
     * @param executorService ExecutorService for submit
     * @param context         Context for onCommandMessage
     * @return Future to test for completion.
     */
    default Future<T> onCommandMessageAsync(ExecutorService executorService, M context) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executorService.submit((Runnable) () -> {
            try {
                future.complete(onCommandMessage(context));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
