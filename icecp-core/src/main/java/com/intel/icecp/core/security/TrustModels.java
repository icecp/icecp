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
