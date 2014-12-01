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
