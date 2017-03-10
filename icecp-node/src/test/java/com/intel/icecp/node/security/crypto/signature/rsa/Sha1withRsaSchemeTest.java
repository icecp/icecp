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
package com.intel.icecp.node.security.crypto.signature.rsa;

import com.intel.icecp.core.security.crypto.signature.SignatureScheme;
import com.intel.icecp.node.security.crypto.signature.AsymmetricSignatureSchemeTest;
import org.junit.Test;

/**
 * Test for {@link Sha1withRsaScheme} 
 *
 */
public class Sha1withRsaSchemeTest extends AsymmetricSignatureSchemeTest {
    
    /** Implementations to test*/
    private static final SignatureScheme[] rsaSignatureSchemes = {new Sha1withRsaScheme()};

    private static final int[] KEY_SIZES = {1024};
    
    /**
     * Test for {@link Sha1withRsaScheme#sign(byte[], com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey) }
     * 
     * @throws Exception 
     */
    @Test
    public void sha1RsaSignTest() throws Exception {
        signTest("RSA", rsaSignatureSchemes, KEY_SIZES);
    }
    
    /**
     * Test for {@link Sha1withRsaScheme#verify(byte[], byte[], com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey)  }
     * 
     * @throws Exception 
     */
    @Test
    public void sha1RsaVerifyTest() throws Exception {
        verifyTest("RSA", rsaSignatureSchemes, KEY_SIZES);
    }
}
