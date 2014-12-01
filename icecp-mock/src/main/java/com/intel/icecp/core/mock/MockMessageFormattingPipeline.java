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
package com.intel.icecp.core.mock;

import com.intel.icecp.core.channels.Token;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.pipeline.Operation;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.core.pipeline.exception.OperationException;
import com.intel.icecp.core.pipeline.exception.PipelineException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Specific instantiation of a {@link Pipeline} where the output type is fixed
 * ({@link InputStream}), and the input is a subclass of {@link Message} Note
 * that this implementation neither peforms type checking on operations, nor
 * deals with concurrent access.
 *
 * @param <T> subclass or implementation of a message
 */
class MockMessageFormattingPipeline<T extends Message> implements Pipeline<T, InputStream> {
    
    /** Input/output types */
    Token<T> inputType;
    Token<T> outputType = Token.of(InputStream.class);
    
    /** Operations list */
    List<Operation> pipeline = new LinkedList<>();

    /**
     * Exception class for this pipeline implementation
     */
    public static class MockMessageFormattingPipelineException extends PipelineException {
    
        public MockMessageFormattingPipelineException(String message, Throwable cause) {
            super(message, cause);
        }
    
    }
    
    
    
    public MockMessageFormattingPipeline(Token<T> messageType, List<Operation> operations) {
        this.inputType = messageType;
        this.pipeline.addAll(operations);
    }

    private MockMessageFormattingPipeline(Token<T> messageType, Operation... operations) {
        this(messageType, Arrays.asList(operations));
    }

    public MockMessageFormattingPipeline(Class<T> messageType, List<Operation> operations) {
        this(Token.of(messageType), operations);
    }

    public MockMessageFormattingPipeline(Class<T> messageType, Operation... operations) {
        this(Token.of(messageType), operations);
    }

    /**
     * (Non-generic) Returns an instance of
     * {@link MockMessageFormattingPipeline} containing only a
     * {@link MockFormattingOperation}. Use for non-generic types such as
     * String.
     *
     * @param <S> the type of message allowed as input
     * @param messageType the type of message allowed as input
     * @param format the serializer for the given message type
     * @return the creted pipeline
     */
    public static <S extends Message> Pipeline<S, InputStream> create(Class<S> messageType, Format<S> format) {
        return createImpl(Token.of(messageType), format);
    }

    /**
     * (Generic) Returns an instance of {@link MockMessageFormattingPipeline}
     * containing only a {@link MockFormattingOperation}. Use for generic types
     * such as {@literal List<String>}.
     *
     * @param <S> the type of message allowed as input
     * @param messageType the type of message allowed as input
     * @param format the serializer for the given message type
     * @return the creted pipeline
     */
    public static <S extends Message> Pipeline<S, InputStream> create(Token<S> messageType, Format<S> format) {
        return createImpl(messageType, format);
    }
    
    
    /**
     * Returns an instance of {@link MockMessageFormattingPipeline}
     * containing only a {@link MockFormattingOperation}.
     *
     * @param <S> the type of message allowed as input
     * @param messageType the type of message allowed as input
     * @param format the serializer for the given message type
     * @return the creted pipeline
     */
    private static <S extends Message> Pipeline<S, InputStream> createImpl(Token<S> messageType, Format<S> format) {
        return new MockMessageFormattingPipeline<>(messageType, new MockFormattingOperation(format));
    }
    
    /**
     * {@inheritDoc }
     * 
     */
    @SuppressWarnings("unchecked")
    @Override
    public InputStream execute(T input) throws PipelineException {
        Object in = input;
        try {
            // Just one element; return the result of the execution of the single operation
            if (pipeline.size() == 1) {
                return (InputStream) pipeline.get(0).execute(in);
            } else {
                for (int i = 0; i < pipeline.size() - 1; i++) {
                    in = pipeline.get(i).execute(in);
                }
                return (InputStream) pipeline.get(pipeline.size() - 1).execute(in);
            }
        } catch (OperationException | ClassCastException ex) {
            throw new MockMessageFormattingPipelineException("Error while executing the pipeline.", ex);
        }
    }
    
    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public T executeInverse(InputStream input) throws PipelineException {
        Object in = input;
        try {
            // Just one element, so we can return the result of the execution
            // of the single operation
            if (pipeline.size() == 1) {
                return (T) pipeline.get(0).executeInverse(in);
            } else {
                for (int i = pipeline.size() - 1; i > 0; i--) {
                    in = pipeline.get(i).executeInverse(in);
                }
                return (T) pipeline.get(0).executeInverse(in);
            }
        } catch (OperationException | ClassCastException ex) {
            throw new MockMessageFormattingPipelineException("Error while inverting the pipeline.", ex);
        }
    }
    
    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public Pipeline append(List<? extends Operation> operations) {
        if (operations != null) {
            this.pipeline.addAll(operations);
        }
        return this;
    }
    
    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public Pipeline append(Operation... operations) {
        if (operations != null) {
            return append(Arrays.asList(operations));
        }
        return this;
    }
}
