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
