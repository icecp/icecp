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
package com.intel.icecp.node.security.tpm.data;

import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.node.security.crypto.utils.CryptoUtils;

/**
 * Representation of sealed data object
 *
 * NOTE THAT, the choice of the format to use, is based on the output of the
 * command tpm_sealdata from IBM tpm_tools (is not a standard representation...)
 *
 */
public class SealedData {

    // Added for compatibility with tpm_tools
    private static final String BEGIN_TSS_HEADER = "-----BEGIN TSS-----";
    private static final String BEGIN_TSS_KEY_HEADER = "-----TSS KEY-----";
    private static final String BEGIN_ENC_KEY_HEADER = "-----ENC KEY-----";
    private static final String BEGIN_ENC_DAT_HEADER = "-----ENC DAT-----";
    private static final String END_TSS_HEADER = "-----END TSS-----";
    private static final String SYMMETRIC_KEY_ALGO_TAG = "Symmetric Key: ";

    // Bytes of the sealed symmetric key
    public byte[] sealedSymmKey;

    // Sealing key specs to be used to load the key inside the TPM
    public byte[] sealingKeySpecs;

    // The actual encrypted date, ecrypte with the symm ket sealed by the TPM
    public byte[] encryptedData;

    // Encryption algorithm
    public String encryptionAlgorithm;

    public SealedData(byte[] sealedSymmKey, byte[] sealingKeySpecs, byte[] encryptedData, String encryptionAlgorithm) {
        this.sealedSymmKey = sealedSymmKey;
        this.sealingKeySpecs = sealingKeySpecs;
        this.encryptedData = encryptedData;
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    /**
     * Decodes sealed data in the form:
     *
     * -----BEGIN TSS----- -----TSS KEY----- ... -----ENC KEY----- ... -----ENC
     * DAT----- ... -----END TSS-----
     *
     * @param sealedData
     * @return a SealedData object, or null
     */
    public static SealedData decode(byte[] sealedData) {
        try {

            int offset = 0;
            String[] lines = new String(sealedData).split("\n");

            if (lines == null || lines.length == 0) {
                return null;
            }

            // Iignore line 0 and 1, since they are respectively BEGIN_TSS_HEADER and BEGIN_TSS_KEY_HEADER
            offset = offset + 2;

            // read the key specs blob
            StringBuilder keySpecs = new StringBuilder();
            while (!lines[offset].equals(BEGIN_ENC_KEY_HEADER) && lines.length > offset) {
                keySpecs.append(lines[offset]);
                offset++;
            }

            // Jump the line BEGIN_ENC_KEY_HEADER
            offset++;
            if (offset >= lines.length) {
                return null;
            }

            // Now we can read the algorithm type
            if (!lines[offset].startsWith(SYMMETRIC_KEY_ALGO_TAG)) {
                return null;
            }

            String algoType;
            if (lines[offset].contains("AES")) {
                if (lines[offset].contains("CBC")) {
                    algoType = SecurityConstants.AES_CBC_ALGORITHM;
                } else {
                    algoType = SecurityConstants.AES_ECB_ALGORITHM;
                }
            } else {
                // Try AES CBC if we can not read
                algoType = SecurityConstants.AES_CBC_ALGORITHM;
            }

            offset++;
            if (offset >= lines.length) {
                return null;
            }
            // Now read the key
            // read the key specs blob
            StringBuilder sealedSymmKey = new StringBuilder();
            while (!lines[offset].equals(BEGIN_ENC_DAT_HEADER) && lines.length > offset) {
                sealedSymmKey.append(lines[offset]);
                offset++;
            }

            // Jump to BEGIN_ENC_DAT_HEADER
            offset++;
            if (offset >= lines.length) {
                return null;
            }

            // lastly we read the enc data
            StringBuilder encData = new StringBuilder();
            while (!lines[offset].equals(END_TSS_HEADER) && lines.length > offset) {
                encData.append(lines[offset]);
                offset++;
            }

            if (offset >= lines.length) {
                return null;
            }

            // All OK, return the decoded object
            return new SealedData(
                    CryptoUtils.base64Decode(sealedSymmKey.toString().getBytes()),
                    CryptoUtils.base64Decode(keySpecs.toString().getBytes()),
                    CryptoUtils.base64Decode(encData.toString().getBytes()),
                    algoType);

        } catch (Exception ex) {
//			ex.printStackTrace();
        }
        return null;
    }

    /**
     * Encodes sealed data in the form:
     *
     * -----BEGIN TSS----- -----TSS KEY----- ... -----ENC KEY----- ... -----ENC
     * DAT----- ... -----END TSS-----
     *
     * @param sealedData
     * @return
     */
    public static byte[] encode(SealedData sealedData) {

        StringBuilder output = new StringBuilder();

        output.append(BEGIN_TSS_HEADER).append("\n")
                .append(BEGIN_TSS_KEY_HEADER).append("\n")
                .append(new String(CryptoUtils.base64Encode(sealedData.sealingKeySpecs)).replaceAll("(.{65})", "$1\n")).append("\n")
                .append(BEGIN_ENC_KEY_HEADER).append("\n")
                .append(SYMMETRIC_KEY_ALGO_TAG).append(sealedData.encryptionAlgorithm).append("\n")
                .append(new String(CryptoUtils.base64Encode(sealedData.sealedSymmKey)).replaceAll("(.{65})", "$1\n")).append("\n")
                .append(BEGIN_ENC_DAT_HEADER).append("\n")
                .append(new String(CryptoUtils.base64Encode(sealedData.encryptedData)).replaceAll("(.{65})", "$1\n")).append("\n")
                .append(END_TSS_HEADER);

        return output.toString().getBytes();

    }

}
