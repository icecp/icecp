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
package com.intel.icecp.node.security;

/**
 * Utility class that should not be constructed.
 * Contains constants for security components
 *
 */
public class SecurityConstants {

    // ***** Hash algorithms
    public static final String SHA1 = "SHA-1";
    public static final String MD5 = "MD5";
    public static final String SHA256 = "SHA-256";
    public static final String SHA384 = "SHA-384";
    public static final String SHA512 = "SHA-512";

    // ***** Symmetric key types
    public static final String AES = "AES";
    // Default values
    public static final int DEFAULT_SYMM_KEY_SIZE = 128;

    // ***** Symm Encryption algorithms
    public static final String AES_ECB_ALGORITHM = "AES/ECB/PKCS5Padding";
    public static final String AES_CBC_ALGORITHM = "AES/CBC/PKCS5Padding";

    // ***** Asymm Encryption algorithms
    public static final String RSA_ALGORITHM = "RSA";
    public static final String EC_ALGORITHM = "EC";
    public static final String CPABE_ALGORITHM = "CP-ABE";

    // ***** Signature algorihms
    public static final String SHA1withDSA = "SHA1withDSA";
    public static final String SHA1withRSA = "SHA1withRSA";
    public static final String SHA256withRSA = "SHA256withRSA";
    public static final String SHA1withECDSA = "SHA1withECDSA";

    // ***** Message Auth Code (MAC)
    public static final String HmacSHA1 = "HmacSHA1";
    public static final String HmacSHA224 = "HmacSHA224";
    public static final String HmacSHA256 = "HmacSHA256";
    public static final String HmacSHA384 = "HmacSHA384";
    public static final String HmacSHA512 = "HmacSHA512";

    // ***** Key Manager 
    public static final String KEY_STORE_BASED_MANAGER = "keystorebasedmanager";

    // ***** TPM
    // IV always used in tpm_sealdata command from tpm_tools
    protected static final byte[] TPM_TOOLS_IV_SEALING = "IBM SEALIBM SEAL".getBytes();

    public static final String TPM_1_2_SERVICE = "TPM1.2ServiceTrousers";
    public static final String TPM_1_2_SERVICE_PROPERTIES = "tpm1.2_tss.properties";
    public static final String DEFAULT_TPM_SERVICE = TPM_1_2_SERVICE;

    // ***** TRUST MODEL
    
    public static final String HYERARCHICAL_TRUST_MODEL = "HIERARCHICAL";
    public static final String SIMPLE_ASYMMETRIC_TRUST_MODEL = "SIMPLE_ASYMMETRIC";
    public static final String SIMPLE_SYMMETRIC_TRUST_MODEL = "SIMPLE_SYMMETRIC";
    public static final String DEFAULT_TRUST_MODEL = "SIMPLE";

    // ***** CHANNEL SECURITY
    public static final String DEFAULT_CHANNEL_SECURITY_MANAGER = "DefaultChannelSecuirtyManager";

    private SecurityConstants() {

    }
    
    /**
     * Password to use when unsealing the sealed data generated with 
     * {@literal tpm_sealdata} (for compatibility with IBM TPM tools)
     * 
     * @return Bytes of the "password" string
     */
    public static byte[] getTpmToolsSealSecret() {
        return "password".getBytes();
    }    
    
    /**
     * Returns a default password as a char array.
     * To use for testing purposes only
     * 
     * @return Default password char array
     */
    public static char[] getKeyStoreDefaultPassword() { 
        return new char[]{'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};
    }
    
}
