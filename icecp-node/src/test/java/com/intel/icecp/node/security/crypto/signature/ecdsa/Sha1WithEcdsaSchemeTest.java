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
package com.intel.icecp.node.security.crypto.signature.ecdsa;

import com.intel.icecp.core.security.crypto.signature.SignatureScheme;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.node.security.crypto.signature.AsymmetricSignatureSchemeTest;
import org.junit.Test;

/**
 * Test for {@link Sha1WithEcdsaScheme}
 *
 */
public class Sha1WithEcdsaSchemeTest extends AsymmetricSignatureSchemeTest {
    
    private static final SignatureScheme[] SCHEMES = {new Sha1WithEcdsaScheme()};
    // Different key sizes: 160 bits ==> 80 bits, 
    private static final int[] KEY_SIZES = {160, 192, 256};
    
    /**
     * {@inheritDoc }
     */
    @Test
    public void Sha1WithEcdsaSignTest() throws Exception {
        signTest(SecurityConstants.EC_ALGORITHM, SCHEMES, KEY_SIZES);
    }
    
    /**
     * {@inheritDoc }
     */
    @Test
    public void Sha1WhithEcdsaVerifyTest() throws Exception {
        verifyTest(SecurityConstants.EC_ALGORITHM, SCHEMES, KEY_SIZES);
    }
    
}
