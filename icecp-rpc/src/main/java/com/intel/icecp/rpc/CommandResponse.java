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
package com.intel.icecp.rpc;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.intel.icecp.core.Message;

/**
 * Represent a response to a {@link Command} invocation
 *
 * @see CommandRegistry
 */
public class CommandResponse implements Message {
    private static final int TIMEOUT_FUTURE_RESULT = 2000;
    public boolean err;
    public Object out;

    public CommandResponse(){    
    }
    
    private CommandResponse(Object out, boolean err) {
        this.out = out;
        this.err = err;
    }

    /**
     * This method returns a CommandResponse with out object being the simple class name of the exception 
     * concatenated with error message if it is not null 
     * @param error invocation exception information
     * @return CommandResponse command response from future call. e
     */
    public static CommandResponse fromError(Throwable error) {
        String errMsg = error.getMessage();
        String errForOut = errMsg != null ? error.getClass().getSimpleName() + " : " + errMsg
                : error.getClass().getSimpleName();

        return new CommandResponse(errForOut, true);
    }

    /**
     * This method get the target exception inside the InvocationTargetException and returns the corresponding
     * CommandRequest.
     * @param error invocation exception information
     * @return CommandResponse command response from future call.
     */
    public static CommandResponse fromError(InvocationTargetException error) {
        return fromError(error.getTargetException());
    }

    /**
     * @param response object from remote method call
     */
    public static CommandResponse fromValid(Object response) {
        return new CommandResponse(response, false);
    }

    /**
     * @param response object from remote method call
     * @param timeout timeout for this to wait on the future object
     * @param timeunit unit for timeout
     * @return CommandResponse command response from future call.
     */
    public static CommandResponse fromValidFuture(Future<?> response, long timeout, TimeUnit timeunit) {

        try {
            return new CommandResponse(response.get(timeout, timeunit), false);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return fromError(e);
        }
    }
}
