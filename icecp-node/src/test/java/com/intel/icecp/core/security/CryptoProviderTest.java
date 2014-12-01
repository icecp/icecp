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
package com.intel.icecp.core.security;

import com.intel.icecp.core.security.crypto.cipher.Cipher;
import com.intel.icecp.core.security.crypto.mac.MacScheme;
import com.intel.icecp.core.security.crypto.signature.SignatureScheme;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.node.security.crypto.cipher.asymmetric.RsaCipher;
import com.intel.icecp.node.security.crypto.cipher.symmetric.AesCbcCipher;
import com.intel.icecp.node.security.crypto.cipher.symmetric.AesEcbCipher;
import com.intel.icecp.node.security.crypto.mac.hmac.HmacSha1Scheme;
import com.intel.icecp.node.security.crypto.mac.hmac.HmacSha224Scheme;
import com.intel.icecp.node.security.crypto.mac.hmac.HmacSha256Scheme;
import com.intel.icecp.node.security.crypto.mac.hmac.HmacSha384Scheme;
import com.intel.icecp.node.security.crypto.mac.hmac.HmacSha512Scheme;
import com.intel.icecp.node.security.crypto.signature.dsa.Sha1withDsaScheme;
import com.intel.icecp.node.security.crypto.signature.ecdsa.Sha1WithEcdsaScheme;
import com.intel.icecp.node.security.crypto.signature.rsa.Sha1withRsaScheme;
import com.intel.icecp.node.security.crypto.signature.rsa.Sha256withRsaScheme;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link CryptoProvider} class
 *
 */
public class CryptoProviderTest {

    /** Available crypto services */
    private final Class[] signatureTypes = {Sha1withDsaScheme.class, Sha1withRsaScheme.class, Sha256withRsaScheme.class, Sha1WithEcdsaScheme.class};
    private final Class[] hmacTypes = {HmacSha1Scheme.class, HmacSha224Scheme.class, HmacSha256Scheme.class, HmacSha384Scheme.class, HmacSha512Scheme.class};
    private final Class[] cipherTypes = {AesCbcCipher.class, AesEcbCipher.class, RsaCipher.class};
    
    /**
     * Initializes service files with the supported service instantiations, if needed
     * 
     * @throws Exception 
     */
    @Before
    public void init() throws Exception {
        SecurityServicesTestUtils.createConfigurationFile(signatureTypes, SignatureScheme.class);
        SecurityServicesTestUtils.createConfigurationFile(hmacTypes, MacScheme.class);
        SecurityServicesTestUtils.createConfigurationFile(cipherTypes, Cipher.class);
    }
    
    /**
     * Test for {@link CryptoProvider#getSignatureScheme(java.lang.String, boolean) }
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getSignatureSchemeTest() throws Exception {
        Assert.assertNotNull(CryptoProvider.getSignatureScheme(SecurityConstants.SHA1withDSA, false));
        Assert.assertNotNull(CryptoProvider.getSignatureScheme(SecurityConstants.SHA1withRSA, false));
        Assert.assertNotNull(CryptoProvider.getSignatureScheme(SecurityConstants.SHA1withECDSA,false));
        Assert.assertNotNull(CryptoProvider.getSignatureScheme(SecurityConstants.SHA256withRSA, false));
    }
    
    /**
     * Test for {@link CryptoProvider#getMacScheme(java.lang.String, boolean) }
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getMacSchemeTest() throws Exception {
        Assert.assertNotNull(CryptoProvider.getMacScheme(SecurityConstants.HmacSHA1, false));
        Assert.assertNotNull(CryptoProvider.getMacScheme(SecurityConstants.HmacSHA224, false));
        Assert.assertNotNull(CryptoProvider.getMacScheme(SecurityConstants.HmacSHA256, false));
        Assert.assertNotNull(CryptoProvider.getMacScheme(SecurityConstants.HmacSHA384, false));
        Assert.assertNotNull(CryptoProvider.getMacScheme(SecurityConstants.HmacSHA512, false));
    }
    
    /**
     * Test for {@link CryptoProvider#getCipher(java.lang.String, boolean) }
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getCipherTest() throws Exception {
        Assert.assertNotNull(CryptoProvider.getCipher(SecurityConstants.AES_ECB_ALGORITHM, false));
        Assert.assertNotNull(CryptoProvider.getCipher(SecurityConstants.AES_CBC_ALGORITHM, false));
        Assert.assertNotNull(CryptoProvider.getCipher(SecurityConstants.RSA_ALGORITHM, false));
    }

}
