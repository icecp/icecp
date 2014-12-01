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
package com.intel.icecp.core.security.trust;

import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.security.SecurityService;
import com.intel.icecp.core.security.crypto.key.Key;
import com.intel.icecp.core.security.crypto.key.SecretKey;
import com.intel.icecp.core.security.keymanagement.IcecpKeyManager;
import com.intel.icecp.core.security.trust.exception.TrustModelInstantiationError;

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
    <S extends SecretKey, V extends Key> TrustModel<S, V> build(IcecpKeyManager keyManager, Attributes attributes) throws TrustModelInstantiationError;

}
