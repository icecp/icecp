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
