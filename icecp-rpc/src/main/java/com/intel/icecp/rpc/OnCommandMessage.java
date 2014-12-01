/*
 * ******************************************************************************
 *
 *  INTEL CONFIDENTIAL
 *
 *  Copyright 2013 - 2016 Intel Corporation All Rights Reserved.
 *
 *  The source code contained or described herein and all documents related to the
 *  source code ("Material") are owned by Intel Corporation or its suppliers or
 *  licensors. Title to the Material remains with Intel Corporation or its
 *  suppliers and licensors. The Material contains trade secrets and proprietary
 *  and confidential information of Intel or its suppliers and licensors. The
 *  Material is protected by worldwide copyright and trade secret laws and treaty
 *  provisions. No part of the Material may be used, copied, reproduced, modified,
 *  published, uploaded, posted, transmitted, distributed, or disclosed in any way
 *  without Intel's prior express written permission.
 *
 *  No license under any patent, copyright, trade secret or other intellectual
 *  property right is granted to or conferred upon you by disclosure or delivery of
 *  the Materials, either expressly, by implication, inducement, estoppel or
 *  otherwise. Any license under such intellectual property rights must be express
 *  and approved by Intel in writing.
 *
 *  Unless otherwise agreed by Intel in writing, you may not remove or alter this
 *  notice or any other notice embedded in Materials by Intel or Intel's suppliers
 *  or licensors in any way.
 *
 * *********************************************************************
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
