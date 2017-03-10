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
package com.intel.icecp.core.pipeline;

import com.intel.icecp.core.channels.Token;
import com.intel.icecp.core.pipeline.exception.OperationException;

/**
 * Operation to be executed on an input of type {@literal I}, returning an output of type
 * {@literal O}. The operation may be invertible
 *
 * @param <I>
 * @param <O>
 */
public abstract class Operation<I, O> {
    
    /** Input and output type as instance of {@link Token}*/
    protected Token<I> inputType;
    protected Token<O> outputType;

    /**
     * Constructor to use in case of generic types (e.g., {@literal List<String>})
     * 
     * @param inputType
     * @param outputType 
     */
    protected Operation(Token<I> inputType, Token<O> outputType) {
        this.inputType = inputType;
        this.outputType = outputType;
    }
    
    /**
     * Constructor to use in case of non-generic types (e.g., {@literal String})
     * 
     * @param inputType
     * @param outputType 
     */
    protected Operation(Class<I> inputType, Class<O> outputType) {
        this.inputType = Token.of(inputType);
        this.outputType = Token.of(outputType);
    }
    
    /**
     * Performs a specific operation given an input of type {@literal I}
     *
     * @param input of type {@literal O}
     * @return The result of the execution of this operation
     * @throws com.intel.icecp.core.pipeline.exception.OperationException
     */
    public abstract O execute(I input) throws OperationException;

    /**
     * Performs a specific reverse operation (if possible) given an input
     *
     * @param input
     * @return
     * @throws com.intel.icecp.core.pipeline.exception.OperationException
     */
    public abstract I executeInverse(O input) throws OperationException;

    /**
     * Return the input type for this operation
     *
     * @return
     */
    public Token<I> getInputType() {
        return inputType;
    }

    /**
     * Returns the output type for this operation
     *
     * @return
     */
    public Token<O> getOutputType() {
        return outputType;
    }
}
