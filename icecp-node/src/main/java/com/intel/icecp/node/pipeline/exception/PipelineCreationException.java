/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.icecp.node.pipeline.exception;

import com.intel.icecp.core.pipeline.exception.PipelineException;

/**
 * Error in creating a pipeline
 *
 */
public class PipelineCreationException extends PipelineException {

    public PipelineCreationException(String message) {
        super(message);
    }

    public PipelineCreationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    
    
}
