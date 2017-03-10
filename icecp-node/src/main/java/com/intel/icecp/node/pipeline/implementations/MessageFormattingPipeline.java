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
package com.intel.icecp.node.pipeline.implementations;

import com.intel.icecp.core.channels.Token;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.pipeline.Operation;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.node.pipeline.PipelineImpl;
import com.intel.icecp.node.pipeline.operations.FormattingOperation;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Specific instantiation of a pipeline with fixed output type {@link InputStream}
 * and the input is a subclass of {@link Message}
 *
 * 
 * @param <M> subclass or implementation of a message
 * @deprecated This class will be replaced by some SPI mechanism 
 */
@Deprecated
public class MessageFormattingPipeline<M extends Message> extends PipelineImpl<M, InputStream> {

    public MessageFormattingPipeline(Class<M> messageType, List<Operation> operations) {
        super(messageType, InputStream.class, operations);
    }

    public MessageFormattingPipeline(Class<M> messageType, Operation... operations) {
        super(messageType, InputStream.class, Arrays.asList(operations));
    }

    public MessageFormattingPipeline(Token<M> messageType, List<Operation> operations) {
        super(messageType, Token.of(InputStream.class), operations);
    }

    public MessageFormattingPipeline(Token<M> messageType, Operation... operations) {
        super(messageType, Token.of(InputStream.class), Arrays.asList(operations));
    }


    /**
     * (Non-generic) Returns a pipeline containing only a {@link FormattingOperation}
     * Use for non-generic types such as {@literal String}.
     *
     * @param <S>
     * @param messageType
     * @param format
     * @return
     */
    public static <S extends Message> Pipeline<S, InputStream> create(Class<S> messageType, Format<S> format) {
        return createImpl(Token.of(messageType), format);
    }

    /**
     * (Generic) Returns a pipeline containing only a {@link FormattingOperation}. 
     * Use for generic types such as {@literal List<String>}.
     *
     * @param <S>
     * @param messageType
     * @param format
     * @return
     */
    public static <S extends Message> Pipeline<S, InputStream> create(Token<S> messageType, Format<S> format) {
        return createImpl(messageType, format);
    }

    private static <S extends Message> Pipeline<S, InputStream> createImpl(Token<S> messageType, Format<S> format) {
        return new MessageFormattingPipeline(messageType, new FormattingOperation(format));
    }
}
