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
package com.intel.icecp.node.security.trust;

import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.security.TrustModels;
import com.intel.icecp.core.security.crypto.key.Key;
import com.intel.icecp.core.security.crypto.key.SecretKey;
import com.intel.icecp.core.security.keymanagement.IcecpKeyManager;
import com.intel.icecp.core.security.trust.TrustModel;
import com.intel.icecp.core.security.trust.TrustModelProvider;
import com.intel.icecp.core.security.trust.exception.TrustModelInstantiationError;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link TrustModels}; providers are maintained in a
 * {@link HashMap}.
 *
 */
public class TrustModelsImpl implements TrustModels {

    /**
     * Available providers
     */
    private final Map<String, TrustModelProvider> providers = new HashMap<>();

    /**
     * Key manager to pass in to trust model provider
     */
    private final IcecpKeyManager keyManager;

    public TrustModelsImpl(IcecpKeyManager keyManager) {
        this.keyManager = keyManager;
    }

    /**
     * {@inheritDoc }
     *
     */
    @Override
    public boolean register(String providerId, TrustModelProvider provider) {
        if (!providers.containsKey(providerId)) {
            providers.put(providerId, provider);
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
        // True if we removed the provider
        return providers.remove(providerId) != null;
    }

    /**
     * Simply returns the element, if present (note that {@link HashMap} returns
     * {@literal null} if it doesn't exists)
     * 
     * {@inheritDoc }
     *
     */
    @Override
    public TrustModelProvider get(String providerId) {
        return providers.get(providerId);
    }

    /**
     * Creates the trust model if the corresponding provider is present in
     * the list of available implementations. If not, throws an exception.
     * 
     * {@inheritDoc }
     *
     */
    @Override
    public <S extends SecretKey, V extends Key> TrustModel<S, V> createTrustModel(String providerId, Attributes attributes) throws TrustModelInstantiationError {
        if (providers.containsKey(providerId)) {
            return providers.get(providerId).build(keyManager, attributes);
        }
        throw new TrustModelInstantiationError("The given trust model provider " + providerId + " does not exist");
    }

}
