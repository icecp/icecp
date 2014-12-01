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
package com.intel.icecp.core.security.keymanagement;

import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.security.SecurityService;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerException;

/**
 * Provider class that handles key manager specific initializations.
 *
 */
public interface IcecpKeyManagerProvider extends SecurityService<String> {
    
    /**
     * Returns a key manager
     * 
     * @param keyChannels Channels that may be used to remotely fetch keys
     * @param attributes Attributes to use for initialization
     * @return An instance of a key manager
     * @throws KeyManagerException If the key manager cannot be created
     */
    IcecpKeyManager get(Channels keyChannels, Attributes attributes) throws KeyManagerException;
    
    /**
     * Returns a key manager
     * 
     * @param keyChannels Channels that may be used to remotely fetch keys
     * @param configuration Configuration parameters
     * @return An instance of a key manager
     * @throws KeyManagerException If the key manager cannot be created
     */
    IcecpKeyManager get(Channels keyChannels, Configuration configuration) throws KeyManagerException;
    
}
