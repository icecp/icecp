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

package com.intel.icecp.node.channels.ndn;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.core.pipeline.exception.PipelineException;
import net.named_data.jndn.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.Function;

/**
 * Handler for deserializing messages once they are completed in the CompletableFuture
 *
 */
public class MessageDeserializer<T extends Message> implements Function<Data, T> {

    private static final Logger LOGGER = LogManager.getLogger();
    private final Pipeline<T, InputStream> pipeline;

    /**
     * @param pipeline for deserializing bytes
     */
    public MessageDeserializer(Pipeline<T, InputStream> pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * When a data packet is received, deserialize it with the given pipeline; note that the NDN client retrieving
     * segments must concatenate them for this method to work
     *
     * @param data a packet containing all of the message content
     * @return the message
     */
    @Override
    public T apply(Data data) {
        LOGGER.debug("Deserializing message: {}", data.getName());
        ByteArrayInputStream stream = new ByteArrayInputStream(data.getContent().getImmutableArray());
        try {
            return pipeline.executeInverse(stream);
        } catch (PipelineException e) {
            LOGGER.error("Failed to deserialize message: {}", data.getName(), e);
            throw new RuntimeException(e);
        }
    }
}
