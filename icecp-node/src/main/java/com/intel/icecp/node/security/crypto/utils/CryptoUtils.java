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
package com.intel.icecp.node.security.crypto.utils;

import com.intel.icecp.core.security.crypto.key.Key;
import com.intel.icecp.core.security.crypto.exception.hash.HashError;
import com.intel.icecp.core.security.crypto.exception.key.InvalidKeyTypeException;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Collection of utility methods, used by crypto package classes
 *
 */
public class CryptoUtils {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Computes an hash of the given input, using the specified algorithm.
     *
     * @param input
     * @param algorithm
     * @return The hash value, if the algorithm is supported
     * @throws HashError if something went wrong
     */
    public static byte[] hash(byte[] input, String algorithm) throws HashError {
        try {
            MessageDigest m = MessageDigest.getInstance(algorithm);
            m.update(input);
            return m.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new HashError("Unable to compute the hash of the given input.", ex);
        }
    }

    /**
     * Converts a given byte array into a HEX String representation
     *
     * @param bytes bytes to convert into HEX string
     * @return The corresponding HEX string
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Converts a HEX string into bytes
     *
     * @param hexString HEX String to convert into bytes
     * @return The corresponding bytes
     */
    public static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Base64 encoding using {@link Base64#getEncoder() } encoder
     *
     * @param data Data to encode
     * @return The Base64 encoded String
     * @throws IllegalArgumentException In case of error while encoding the given bytes
     */
    public static byte[] base64Encode(byte[] data) throws IllegalArgumentException {
        return Base64.getEncoder().encode(data);
    }

    /**
     * Base64 decoding using {@link Base64#getDecoder() } decoder
     *
     * @param data Data to decode
     * @return The decoded bytes
     * @throws IllegalArgumentException In case of error in decoding the given bytes
     */
    public static byte[] base64Decode(byte[] data) throws IllegalArgumentException {
        return Base64.getDecoder().decode(data);
    }

    /**
     * Compares the given two byte arrays
     *
     * @param first 
     * @param second
     * @return True iif first == true
     */
    public static boolean compareBytes(byte[] first, byte[] second) {
        return MessageDigest.isEqual(first, second);
    }

    /**
     * Given a key, returns a suitable signing algorithm (if exists).
     *
     * @param key
     * @return
     * @throws InvalidKeyTypeException
     */
    public static String getPublicKeyAlgorithmFromKey(Key key) throws InvalidKeyTypeException {
        String keyType;
        if (key instanceof PrivateKey) {
            PrivateKey k = (PrivateKey) key;
            keyType = k.getKey().getAlgorithm();
        } else if (key instanceof PublicKey) {
            PublicKey k = (PublicKey) key;
            keyType = k.getPublicKey().getAlgorithm();
        } else {
            // All other types of keys are not valid asymmetric keys
            throw new InvalidKeyTypeException("Invalid key type " + key.getClass().getName());
        }

        System.out.println("********" + keyType);
        
        if (SecurityConstants.SHA1withDSA.contains(keyType)) {
            return SecurityConstants.SHA1withDSA;
        } else if (SecurityConstants.SHA1withRSA.contains(keyType)) {
            return SecurityConstants.SHA1withRSA;
        } else if (SecurityConstants.SHA256withRSA.contains(keyType)) {
            return SecurityConstants.SHA256withRSA;
        }

        throw new InvalidKeyTypeException("Invalid key type " + keyType);
    }

}
