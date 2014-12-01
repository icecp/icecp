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
package com.intel.icecp.node.security.crypto.cipher.symmetric;

import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.node.security.RandomBytesGenerator;
import javax.crypto.KeyGenerator;

/**
 * Abstract class use for testing symmetric encryption schemes.
 * Note that only key of size 128 is allowed on system where the 
 * "Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy"
 * file is not installed. Therefore the field {@link AesSymmetricCipherTest#KEY_SIZES}
 * contains only 128.
 *
 */
public abstract class AesSymmetricCipherTest {

    /** Different key size to consider */
    protected static final int[] KEY_SIZES = {128};
    
    /** Parameters */
    protected final byte[] textToEncrypt = RandomBytesGenerator.getRandomBytes(2048);
    
    /**
     * Utility method that creates a symmetric key of a given algorithm
     * 
     * @param keySize Size in bits of the key
     * @param algorithmType Algorithm to use (e.g., AES)
     * @return A symmetric key instance 
     * @throws Exception If creation fails
     */
    protected SymmetricKey symmetricKeyGen(int keySize, String algorithmType) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(algorithmType);
        keyGen.init(keySize);
        return new SymmetricKey(keyGen.generateKey());
    }
}
