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
