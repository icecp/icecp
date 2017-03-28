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
package com.intel.icecp.core.security.trust;

import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.security.SecurityService;
import com.intel.icecp.core.security.crypto.key.Key;
import com.intel.icecp.core.security.crypto.key.SecretKey;
import com.intel.icecp.core.security.trust.exception.TrustModelInstantiationError;
import com.intel.icecp.core.security.keymanagement.KeyManager;

/**
 * Interface for a trust model provider, which can be loaded via Java SPI, (as
 * it extends {@link SecurityService}, and exposes a method to instantiate a
 * {@link TrustModel}. This is something similar to what is done for
 * {@link com.intel.icecp.core.channels.ChannelProvider}
 *
 */
public interface TrustModelProvider extends SecurityService<String> {

    /**
     * Instantiates a {@link TrustModel} based on the given input attributes
     *
     * @param <S> Signing key type
     * @param <V> Verification key type
     * @param keyManager Key manager to use to fetch the necessary key material
     * (either locally, or from channels)
     * @param attributes Attributes to use during trust model instantiation
     * @return An instance of the requested trust model
     * @throws TrustModelInstantiationError In case of errors while creating the
     * trust model instance
     */
    <S extends SecretKey, V extends Key> TrustModel<S, V> build(KeyManager keyManager, Attributes attributes) throws TrustModelInstantiationError;

}
