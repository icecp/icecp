/* *****************************************************************************
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
 * *******************************************************************************
 */
package com.intel.icecp.core.security;

import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.security.crypto.key.Key;
import com.intel.icecp.core.security.crypto.key.SecretKey;
import com.intel.icecp.core.security.trust.TrustModel;
import com.intel.icecp.core.security.trust.TrustModelProvider;
import com.intel.icecp.core.security.trust.exception.TrustModelInstantiationError;

/**
 * Interface for creating trust models, registering {@link TrustModelProvider}.
 * The expected usage is the following:
 * <p>
 * <pre>
 * <code>
 *  TrustModels trustModels = new SomeTrustModelsImpl();
 *  trustModels.register(SecurityConstants. ..., new SpecificTrustModelProvider());
 *  trustModels.createTrustModel(SecurityConstants. ..., ...);
 * </code>
 * </pre>
 *
 */
public interface TrustModels {

    /**
     * Registers a provider under a given id; this provider can be used to later
     * create a trust model
     *
     * @param providerId unique provider identifier
     * @param provider Provider to use to instantiate a specific trust model
     * @return {@literal True} if the provider is added successfully;
     * {@literal false} otherwise
     */
    boolean register(String providerId, TrustModelProvider provider);

    /**
     * Unregisters a provider matching a trust model id
     *
     * @param providerId Provider identifier
     * @return {@literal True} if the provider is added successfully;
     * {@literal false} otherwise
     */
    boolean unregister(String providerId);

    /**
     * Returns a registered provider, if exists; null otherwise
     *
     * @param providerId the unique identifier for the provider
     * @return A provider registered under the given ID, or null if no such
     * provider exists
     */
    TrustModelProvider get(String providerId);

    /**
     * Creates and returns a trust model instance, based on the given attributes
     *
     * @param <S> Type of the signing key
     * @param <V> Type of the verification key
     * @param providerId Unique trust model identifier
     * @param attributes Attributes to use for creation and initialization
     * @return The created trust model
     * @throws TrustModelInstantiationError If the trust model cannot be created
     */
    <S extends SecretKey, V extends Key> TrustModel<S, V> createTrustModel(String providerId, Attributes attributes) throws TrustModelInstantiationError;

}
