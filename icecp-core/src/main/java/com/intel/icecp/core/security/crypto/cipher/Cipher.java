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
package com.intel.icecp.core.security.crypto.cipher;

import com.intel.icecp.core.security.SecurityService;
import com.intel.icecp.core.security.crypto.key.Key;
import com.intel.icecp.core.security.crypto.exception.cipher.CipherDecryptionError;
import com.intel.icecp.core.security.crypto.exception.cipher.CipherEncryptionError;

/**
 * Generic interface of a cipher
 *
 * @param <E> Encryption key type
 * @param <D> Decryption key type
 */
public interface Cipher<E extends Key, D extends Key> extends SecurityService<String> {

    /**
     * Encrypts the bytes in input with a given key
     *
     * @param dataToEncrypt Bytes to encrypt
     * @param key Key to use for encryption
     * @param other Other input parameters (if needed by the specific cipher)
     *
     * @return the encrypted bytes, or null in case of any error
     * @throws CipherEncryptionError In case of encryption error
     */
    byte[] encrypt(byte[] dataToEncrypt, E key, Object... other) throws CipherEncryptionError;

    /**
     * Decrypts the given encrypted bytes
     *
     * @param dataToDecrypt Bytes to encrypt
     * @param key Key to use for decryption
     * @param other Other input parameters (if needed by the specific cipher)
     *
     * @return the decrypted bytes, or null in case of any error
     * @throws CipherDecryptionError In case of decryption error
     */
    byte[] decrypt(byte[] dataToDecrypt, D key, Object... other) throws CipherDecryptionError;

}
