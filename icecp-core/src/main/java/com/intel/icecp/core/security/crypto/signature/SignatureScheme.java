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
package com.intel.icecp.core.security.crypto.signature;

import com.intel.icecp.core.security.crypto.key.SecretKey;
import com.intel.icecp.core.security.SecurityService;
import com.intel.icecp.core.security.crypto.exception.siganture.SignatureError;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;

/**
 * Interface for a generic signature scheme
 *
 * @param <S> Signing key type
 * @param <V> Verification key type
 */
public interface SignatureScheme<S extends SecretKey, V extends PublicKey> extends SecurityService<String> {

    /**
     * Returns the signature of a given data, using a given algorithm and key.
     *
     * @param dataToSign Bytes to sign
     * @param key Key to use for signing
     * @return The signature bytes
     * @throws SignatureError In case of error during the signing process
     */
    byte[] sign(byte[] dataToSign, S key) throws SignatureError;

    /**
     * Verifies the signature, given a public key and the algorithm; throws and
     * exception if not verified
     *
     * @param signature Signature bytes
     * @param data Data to use for signature verification
     * @param key Verification key
     * @throws SignatureError In case of error in verifying the signature
     */
    void verify(byte[] signature, byte[] data, V key) throws SignatureError;

}
