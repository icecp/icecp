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

import com.intel.icecp.core.pipeline.exception.PipelineException;
import java.util.List;

/**
 * Pipeline of sequential operations, which takes as input an instance of type
 * {@literal I}, and outputs an instance of type {@literal O}
 *
 * @param <I> Input type
 * @param <O> Output type
 */
public interface Pipeline<I, O> {

    /**
     * Takes an input of type {@literal I}, and returns the result of the
     * pipeline execution (of type {@literal O})
     *
     * @param input the input object to be converted
     * @return the result of the pipeline execution
     * @throws PipelineException if the method encounters errors in processing
     * the pipeline
     */
    O execute(I input) throws PipelineException;

    /**
     * Takes an input of type {@literal O}, and returns the result of the
     * inverse pipeline execution (of type {@literal I})
     *
     * @param input the input object to be converted
     * @return the result of the pipeline execution
     * @throws PipelineException if the method encounters errors in processing
     * the pipeline
     */
    I executeInverse(O input) throws PipelineException;

    /**
     * Appends a list of operations to the pipeline
     *
     * @param operations the operations to add to the pipeline
     * @return a reference to {@link Pipeline}, which may be either this
     * pipeline, or another instance; this is intended for subsequent calls of
     * the method, or to return a new instance in case of an immutable pipeline
     * implementation
     */
    Pipeline append(List<? extends Operation> operations);

    /**
     * Appends an array of operations to the pipeline
     *
     * @param operations the operations to add to the pipeline
     * @return a reference to {@link Pipeline}, which may be either this
     * pipeline, or another instance; this is intended for subsequent calls of
     * the method, or to return a new instance in case of an immutable pipeline
     * implementation
     */
    Pipeline append(Operation... operations);
}
