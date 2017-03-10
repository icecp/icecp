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
