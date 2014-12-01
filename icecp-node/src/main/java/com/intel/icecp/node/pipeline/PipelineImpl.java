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
package com.intel.icecp.node.pipeline;

import com.intel.icecp.core.pipeline.*;
import com.intel.icecp.core.channels.Token;
import com.intel.icecp.node.pipeline.exception.EmptyPipelineException;
import com.intel.icecp.node.pipeline.exception.InvalidPipelineInputTypeException;
import com.intel.icecp.node.pipeline.exception.InvalidPipelineOutputTypeException;
import com.intel.icecp.core.pipeline.exception.OperationException;
import com.intel.icecp.node.pipeline.exception.PipelineExecutionError;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of a generic {@link Pipeline}.
 * <p>
 * NOTE THAT: A {@link com.intel.icecp.node.pipeline.PipelineImpl} pipeline may
 * not be invertible. For example, if we have class A and B s.t., B &lt; A, and
 * two operations {@code Op1<Object, B>} and {@code Op2<A, ...>}, the inverse
 * execution will fail since {@code
 * Op1.inverseOperation()} will try to cast an instance of type B into A
 * (throwing a ClassCastException).
 * <p>
 *
 * @param <I> Input type
 * @param <O> Output type
 */
public class PipelineImpl<I, O> implements Pipeline<I, O> {

    /**
     * {@link java.util.LinkedList} that holds the operations to be executed
     */
    private final List<Operation> pipeline = new LinkedList<>();

    /**
     * Pipeline input and output types
     */
    private final Token<I> inputType;
    private final Token<O> outputType;

    /**
     * Tell whether pipeline types have been checked
     */
    private boolean executable = false;
    private boolean invertible = false;

    /**
     * (Non-Generic) Takes input and output types, and an array of operations
     * that compose the pipeline. To use for non-generic types (e.g.,
     * {@literal String}.
     *
     * @param inputType Input (non-generic) type
     * @param outputType Output (non-generic) type
     * @param operations Operations to add to the pipeline
     */
    public PipelineImpl(Class<I> inputType, Class<O> outputType, List<Operation> operations) {
        this.inputType = Token.of(inputType);
        this.outputType = Token.of(outputType);
        this.append(operations);
    }

    /**
     * (Non-Generic) Takes input and output types; creates an empty pipeline. To
     * use for non-generic types (e.g., {@literal String}.
     *
     * @param inputType the type of the objects to input to the first operation
     * @param outputType the type of the objects resulting from the last
     * operation
     */
    public PipelineImpl(Class<I> inputType, Class<O> outputType) {
        this(inputType, outputType, null);
    }

    /**
     * (Generic) Takes input and output types, and an array of operations that
     * compose the pipeline. To use for generic types (e.g.,
     * {@literal List<String>}.
     *
     * @param inputType Input type
     * @param outputType Output type
     * @param operations Operations to add to the pipeline
     */
    public PipelineImpl(Token<I> inputType, Token<O> outputType, List<Operation> operations) {
        this.inputType = inputType;
        this.outputType = outputType;
        this.append(operations);
    }

    /**
     * (Generic) Takes input and output types; creates an empty pipeline. To use
     * for generic types (e.g., {@literal List<String>}.
     *
     * @param inputType Input type
     * @param outputType Output type
     */
    public PipelineImpl(Token<I> inputType, Token<O> outputType) {
        this(inputType, outputType, null);
    }

    /**
     * Method is synchronized so that {@link PipelineImpl#execute(java.lang.Object)
     * } and {@link PipelineImpl#append(java.util.List) } cannot execute at the
     * same time.
     *
     * {@inheritDoc }
     *
     */
    @SuppressWarnings("unchecked")
    @Override
    public synchronized O execute(I input) throws PipelineExecutionError, EmptyPipelineException, InvalidPipelineInputTypeException, InvalidPipelineOutputTypeException {
        // Checks whether operation types allow execution
        checkExecutable();
        Object in = input;
        try {
            // Just one element; return the result of the execution of the single operation
            if (pipeline.size() == 1) {
                return (O) pipeline.get(0).execute(in);
            } else {
                for (int i = 0; i < pipeline.size() - 1; i++) {
                    in = pipeline.get(i).execute(in);
                }
                return (O) pipeline.get(pipeline.size() - 1).execute(in);
            }
        } catch (OperationException | ClassCastException ex) {
            throw new PipelineExecutionError("Error while executing the pipeline.", ex);
        }
    }

    /**
     * Method is synchronized so that {@link PipelineImpl#executeInverse(java.lang.Object)
     * } and {@link PipelineImpl#append(java.util.List) } cannot execute at the
     * same time.
     *
     * {@inheritDoc }
     *
     */
    @SuppressWarnings("unchecked")
    @Override
    public synchronized I executeInverse(O input) throws PipelineExecutionError, EmptyPipelineException, InvalidPipelineInputTypeException, InvalidPipelineOutputTypeException {
        // Check whether operation types allow inversion
        checkInvertible();
        Object in = input;
        try {
            // Just one element, so we can return the result of the execution
            // of the single operation
            if (pipeline.size() == 1) {
                return (I) pipeline.get(0).executeInverse(in);
            } else {
                for (int i = pipeline.size() - 1; i > 0; i--) {
                    in = pipeline.get(i).executeInverse(in);
                }
                return (I) pipeline.get(0).executeInverse(in);
            }
        } catch (OperationException | ClassCastException ex) {
            throw new PipelineExecutionError("Error while inverting the pipeline.", ex);
        }
    }

    /**
     * Method is synchronized to avoid execution (or inversion) of the pipeline
     * while being modified.
     *
     * {@inheritDoc }
     *
     */
    @Override
    public final synchronized Pipeline append(List<? extends Operation> operations) {
        if (operations != null && !operations.isEmpty()) {
            this.pipeline.addAll(operations);
            // Since we add new operations, the pipeline can no
            // longer be considered executable or invertible
            executable = false;
            invertible = false;
        }
        return this;
    }

    /**
     * {@inheritDoc }
     *
     */
    @Override
    public final Pipeline append(Operation... operations) {
        return append(Arrays.asList(operations));
    }

    /**
     * Check whether this pipeline can be executed due to lack of operation or
     * mismatch input/output types
     *
     * @throws EmptyPipelineException if pipeline is empty
     * @throws InvalidPipelineInputTypeException if the input type of the first
     * operation cannot be assigned from the advertised input type of the
     * pipeline, or there are at least two adjacent operations in the pipeline
     * such that the output type of the previous operation cannot be assigned to
     * the input type of the next operation
     * @throws InvalidPipelineOutputTypeException if the output type of the last
     * operation cannot be assigned to the advertised output type of the
     * pipeline
     */
    @SuppressWarnings("unchecked")
    // this is where we check the type matching
    protected void checkExecutable() throws EmptyPipelineException, InvalidPipelineInputTypeException, InvalidPipelineOutputTypeException {

        // If we know already that the pipeline is executable, 
        // we can simply skip this check
        if (!executable) {

            if (this.pipeline.isEmpty()) {
                throw new EmptyPipelineException("Pipeline is empty.");
            }

            // Check input type
            if (!isInputTypeAssignableAt(0)) {
                String firstClass = this.pipeline.get(0).getInputType().toClass().getName();
                throw new InvalidPipelineInputTypeException("Operations not compatible with pipeline expected Input type. "
                        + "Input type of first DIRECT operation is " + firstClass + ", "
                        + "while pipeline expects input type " + this.inputType + "");
            }

            final int lastElementIndex = this.pipeline.size() - 1;
            // Check output type
            if (!isOutputTypeAssignableAt(lastElementIndex)) {
                throw new InvalidPipelineOutputTypeException("Operations not compatible with pipeline types. Output type of "
                        + "last DIRECT operation is " + this.pipeline.get(lastElementIndex).getOutputType().toClass().getName() + ", "
                        + "while pipeline expects output type " + this.outputType + "");
            }

            // Check adjacent operations
            for (int i = 0; i < lastElementIndex; i++) {
                Token<?> inpType = pipeline.get(i + 1).getInputType();
                Token<?> outType = pipeline.get(i).getOutputType();
                if (!inpType.isAssignableFrom(outType)) {
                    throw new InvalidPipelineInputTypeException("Incompatible operations in the pipeline: " + i + "th DIRECT operation produces "
                            + "an output " + "of type " + outType.toClass().getName() + ", while the next operation "
                            + "expects an input of type " + inpType.toClass().getName());
                }
            }
            // Pipeline execution type checking succeeded
            executable = true;
        }
    }

    /**
     * Check whether this pipeline can be inverted inversely due to lack of
     * operation or mismatch input/output types
     *
     * @throws EmptyPipelineException if the pipeline is empty
     * @throws InvalidPipelineInputTypeException if the output type of the last
     * operation cannot be assigned from the advertised output type of the
     * pipeline, or there are at least two adjacent operations in the pipeline
     * such that the input type of the next (in original order) operation cannot
     * be assigned to the output type of the previous (in original order)
     * operation
     * @throws InvalidPipelineOutputTypeException if the input type of the last
     * operation cannot be assigned to the advertised input type of the pipeline
     */
    @SuppressWarnings("unchecked")
    // this is where we check the type matching
    protected void checkInvertible() throws EmptyPipelineException, InvalidPipelineInputTypeException, InvalidPipelineOutputTypeException {

        // If we know already that the pipeline is invertible, 
        // we can simply skip this check
        if (!invertible) {

            if (this.pipeline.isEmpty()) {
                throw new EmptyPipelineException("Pipeline is empty.");
            }

            final int lastElementIndex = this.pipeline.size() - 1;
            // Check input type (for inverse operation)
            if (!isOutputTypeAssignableAt(lastElementIndex)) {
                throw new InvalidPipelineInputTypeException("Operations not compatible with INVERSE pipeline types. "
                        + "Input type of the first INVERSE operation is " + this.pipeline.get(lastElementIndex).getOutputType().toClass().getName()
                        + ", while pipeline expects input type " + this.outputType + "");
            }

            // Check output type (for inverse operation)
            if (!isInputTypeAssignableAt(0)) {
                throw new InvalidPipelineOutputTypeException("Operations not compatible with INVERSE pipeline types. "
                        + "Output type of the last INVERSE operation is " + this.pipeline.get(0).getInputType().toClass().getName()
                        + ", while pipeline expects output type " + this.inputType + "");
            }

            // Check adjacent operations
            for (int i = lastElementIndex; i > 0; i--) {
                // The input of the ith operation must be assignable with the output
                // of the operation i-1, so that executeInverse can be performed
                if (!pipeline.get(i - 1).getOutputType().isAssignableFrom(pipeline.get(i).getInputType())) {
                    throw new InvalidPipelineInputTypeException(
                            "Incompatible operations in the INVERSE pipeline: " + i + "th INVERSE operation produces an output "
                            + "of type " + pipeline.get(i).getInputType().toClass().getName() + ", while the next operation "
                            + "expects an input of type " + pipeline.get(i - 1).getOutputType().toClass().getName());
                }
            }
            // Inverse pipeline type checking succeeded
            invertible = true;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isOutputTypeAssignableAt(int index) {
        return this.pipeline.get(index).getOutputType().isAssignableFrom(this.outputType);
    }

    @SuppressWarnings("unchecked")
    private boolean isInputTypeAssignableAt(int index) {
        return this.pipeline.get(index).getInputType().isAssignableFrom(this.inputType);
    }
}
