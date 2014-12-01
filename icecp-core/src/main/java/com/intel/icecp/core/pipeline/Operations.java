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

import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.pipeline.exception.OperationException;

/**
 * Interface for operations creation, using a provider pattern; the interface
 * exposes methods to register and obtain {@link OperationProvider}, and to
 * build {@link Operation} from attributes.
 *
 */
public interface Operations {

    /**
     * Registers a provider, and returns {@literal true} if the operation
     * succeeded
     *
     * @param providerId Unique identifier for the provider
     * @param provider Operation provider
     * @return {@literal true} if the provider was registered; {@literal false}
     * otherwise
     */
    boolean register(String providerId, OperationProvider provider);

    /**
     * Unregisters a provider; returns {@literal true} if the operation
     * succeeded
     *
     * @param providerId Unique identifier for the provider to remove
     * @return {@literal true} if the provider was unregistered;
     * {@literal false} otherwise
     */
    boolean unregister(String providerId);

    /**
     * Returns the provider associated to the given id.
     *
     * @param providerId Provider id
     * @return The operation provider associated to the given id
     */
    OperationProvider get(String providerId);

    /**
     * Given a provider id, and a list of attributes, builds an operation
     *
     * @param <O> Operation type
     * @param providerId Unique identifier for the provider
     * @param attributes Attributes the operation provider may require for the
     * initialization of the operation
     * @return A new operation
     * @throws OperationException in case of creation error
     */
    <O extends Operation> O buildOperation(String providerId, Attributes attributes) throws OperationException;
}
