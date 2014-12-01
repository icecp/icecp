/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.icecp.node.pipeline;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.channels.Token;
import com.intel.icecp.core.pipeline.Operation;
import com.intel.icecp.core.pipeline.Operations;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.core.pipeline.exception.OperationException;
import com.intel.icecp.node.pipeline.exception.PipelineCreationException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Factory class for {@link Pipeline}
 *
 * @param <I> Input type
 * @param <O> Output type
 */
public class PipelineFactory<I extends Message, O extends InputStream> {

    private static final Logger LOG = LogManager.getLogger();

    /**
     * Contains all the available providers
     */
    private final Operations opertionProviders;

    /**
     * Input and output types
     */
    private final Token<I> inputType;
    private final Token<O> outputType;

    public PipelineFactory(Operations opertions, Class<I> input, Class<O> output) {
        this.opertionProviders = opertions;
        this.inputType = Token.of(input);
        this.outputType = Token.of(output);
    }

    /**
     * Creates a pipeline based on the input attributes
     *
     * @param attributes To use for pipeline instantiation
     * @return A pipeline
     * @throws PipelineCreationException In case of pipeline instantiation error
     */
    public Pipeline buildPipeline(Attributes attributes) throws PipelineCreationException {

        List<Operation> operations = new ArrayList<>();
        attributes.keySet().stream().forEach(attributeId -> {
            try {
                operations.add(opertionProviders.buildOperation(attributeId, attributes));
            } catch (OperationException ex) {
                // May not be an error, i.e., in case of a non-operation-specific attribute
                LOG.warn("Operation creation error", ex);
            }
        });
        if (operations.isEmpty()) {
            throw new PipelineCreationException("Error creating a pipeline: empty operations list");
        }
        // We do not do pipeline consistency check here; it will be done
        // at runtime.
        return new PipelineImpl(inputType, outputType, operations);

    }

}
