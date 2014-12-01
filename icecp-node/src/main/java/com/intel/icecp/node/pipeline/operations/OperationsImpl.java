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
package com.intel.icecp.node.pipeline.operations;

import com.intel.icecp.core.Node;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.pipeline.Operation;
import com.intel.icecp.core.pipeline.OperationProvider;
import com.intel.icecp.core.pipeline.Operations;
import com.intel.icecp.core.pipeline.exception.OperationCreationException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link Operations} that keeps instances of
 * {@link OperationProvider} in a {@link HashMap}
 *
 */
public class OperationsImpl implements Operations {

    /**
     * Providers holder
     */
    private final Map<String, OperationProvider> operationProviders = new HashMap<>();
    
    private final Node node;
    
    public OperationsImpl(Node node) {
        this.node = node;
    }

    /**
     * Adds only if not yet present
     *
     * {@inheritDoc }
     *
     */
    @Override
    public boolean register(String providerId, OperationProvider provider) {
        if (!operationProviders.containsKey(providerId)) {
            operationProviders.put(providerId, provider);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc }
     *
     */
    @Override
    public boolean unregister(String providerId) {
        return operationProviders.remove(providerId) != null;
    }

    /**
     * {@inheritDoc }
     *
     */
    @Override
    public OperationProvider get(String providerId) {
        return operationProviders.get(providerId);
    }

    /**
     * {@inheritDoc }
     *
     */
    @Override
    public <O extends Operation> O buildOperation(String operationId, Attributes attributes) throws OperationCreationException {
        if (operationProviders.containsKey(operationId)) {
            return operationProviders.get(operationId).buildOperation(node, attributes);
        }
        throw new OperationCreationException("Operation provider for " + operationId + " does not exist");
    }

}
