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
package com.intel.icecp.core.security.crypto.mac;

import com.intel.icecp.core.security.SecurityService;
import com.intel.icecp.core.security.crypto.key.SecretKey;
import com.intel.icecp.core.security.crypto.exception.mac.MacError;

/**
 * Interface for a generic Message Authentication Code (MAC)
 *
 * @param <S> MAC key type
 */
public interface MacScheme<S extends SecretKey> extends SecurityService<String> {

    /**
     * Given data byte and a secret key, produces a MAC
     *
     * @param data Data on which compute a MAC
     * @param key MAC key
     * @return MAC bytes
     * @throws MacError In case of error in computing the MAC
     */
    byte[] computeMac(byte[] data, S key) throws MacError;

    /**
     * Given MAC, the data, and a secret key, returns true iif the MAC is
     * verified
     *
     * @param macBytes MAC bytes
     * @param data Data on which verify the MAC
     * @param key Key to use for verification
     * @throws MacError In case of error in MAC verification
     */
    void verifyMac(byte[] macBytes, byte[] data, S key) throws MacError;

}
