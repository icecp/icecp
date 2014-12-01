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
package com.intel.icecp.node.pipeline.operations;

import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.security.SecurityServicesTestUtils;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.core.security.crypto.mac.MacScheme;
import com.intel.icecp.core.security.trust.TrustModel;
import com.intel.icecp.core.security.trust.exception.TrustModelException;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.node.security.crypto.key.KeyProvider;
import com.intel.icecp.node.security.crypto.mac.hmac.HmacSha1Scheme;
import java.net.URI;
import org.junit.Before;

/**
 * Test for the {@link MacSigningOperation} class
 *
 */
public class MacSigningOperationTest extends SignatureOperationTest {

    private final Class[] hmacTypes = {HmacSha1Scheme.class};
    
    /**
     * Mock trust model that always returns the same symmetric key.
     */
    private class MockSymmetricKeyTrustModel implements TrustModel<SymmetricKey, SymmetricKey> {

        private final SymmetricKey symmKey;

        public MockSymmetricKeyTrustModel() throws Exception {
            this.symmKey = KeyProvider.generateSymmetricKey(SecurityConstants.HmacSHA1);
        }

        @Override
        public SymmetricKey fetchSigningKey(URI keyId) throws TrustModelException {
            return symmKey;
        }

        @Override
        public SymmetricKey fetchVerifyingKey(URI keyId) throws TrustModelException {
            return symmKey;
        }
    }
    
    /**
     * Initialization steps, involving creating the configuration file to be able to
     * later load SHA-1 HMAC algorithm.
     * 
     * @throws Exception In case initialization fails
     */
    @Before
    public void init() throws Exception {
        // Create configuration file for SPI loading 
        SecurityServicesTestUtils.createConfigurationFile(hmacTypes, MacScheme.class);
        // Creat the necessary mock objects
        this.trustModel = new MockSymmetricKeyTrustModel();
        this.format = new JsonFormat(BytesMessage.class);
        
        // Key Id is not important, as the mock trust model will return always a constant key
        // Key should be published under a URI that makes the resource fetchable via a channel
        // e.g., an NDN or file channel. Note that, in this case the key is a secret key, 
        // and therefore is legitimate to assume it would be never exchanged over the network 
        this.signatureOperation = new MacSigningOperation(trustModel, URI.create("file://somekeyfile.key"), SecurityConstants.HmacSHA1, format);
    }

}
