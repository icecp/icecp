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

import com.intel.icecp.core.Node;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.pipeline.exception.OperationCreationException;

/**
 * Provider interface for {@link Operation}
 *
 */
@FunctionalInterface
public interface OperationProvider {

    /**
     * Given a provider id, and a list of attributes, builds an operation
     *
     * @param <O> Operation type
     * @param node Reference to node
     * @param attributes Attributes the operation provider may require to
     * initialize the operation
     * @return A new operation
     * @throws com.intel.icecp.core.pipeline.exception.OperationCreationException In
     * case is not possible to create the operation
     */
    <O extends Operation> O buildOperation(Node node, Attributes attributes) throws OperationCreationException;

}
