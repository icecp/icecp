/*
 * Copyright (c) 2017 Intel Corporation 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
