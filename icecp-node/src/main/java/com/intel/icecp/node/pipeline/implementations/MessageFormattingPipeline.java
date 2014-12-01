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
