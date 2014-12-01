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
