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

import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.security.SecurityServicesTestUtils;
import com.intel.icecp.core.security.crypto.exception.key.InvalidKeyTypeException;
import com.intel.icecp.core.security.crypto.key.asymmetric.KeyPair;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import com.intel.icecp.core.security.crypto.signature.SignatureScheme;
import com.intel.icecp.core.security.trust.TrustModel;
import com.intel.icecp.core.security.trust.exception.TrustModelException;
import com.intel.icecp.node.messages.security.SignedMessage;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.node.security.crypto.key.KeyProvider;
import com.intel.icecp.node.security.crypto.signature.rsa.Sha1withRsaScheme;
import java.net.URI;
import org.junit.Before;

/**
 * Test for class {@link AsymmetricSignOperation}
 *
 */
public class AsymmetricSignOperationTest extends SignatureOperationTest {
    
    public final Class[] signatureAlgorithms = {Sha1withRsaScheme.class};
    
    /**
    * Mock trust model that holds, and always return, a key pair
    */
   public class MockTrustModel implements TrustModel<PrivateKey, PublicKey> {

       /**
        * Private and public key
        */
       private final PrivateKey sk;
       private final PublicKey pk;

       public MockTrustModel() throws InvalidKeyTypeException {
           KeyPair kp = KeyProvider.generateKeyPair("RSA", 1024);
           sk = kp.getPrivateKey();
           pk = kp.getPublicKey();
       }

       /**
        * {@inheritDoc }
        *
        */
       @Override
       public PrivateKey fetchSigningKey(URI signingKeyId) throws TrustModelException {
           return sk;
       }

       /**
        * {@inheritDoc }
        *
        */
       @Override
       public PublicKey fetchVerifyingKey(URI verifyingKeyId) throws TrustModelException {
           return pk;
       }

   }
    
    
    
    /**
     * Initialization steps, involving creating the configuration file to be able to
     * later load SHA-1-RSA signature algorithm.
     * 
     * @throws Exception In case initialization fails
     */
    @Before
    public void init() throws Exception {
        // Create configuration file for SPI loading 
        SecurityServicesTestUtils.createConfigurationFile(signatureAlgorithms, SignatureScheme.class);
        this.trustModel = new MockTrustModel();
        this.format = new JsonFormat<>(SignedMessage.class);
        // Note that key IDs are not important, as MockTrustModel will return a constant key pair
        // Keys should be published under a URI that makes the resource fetchable via a channel
        // e.g., an NDN channel. Note that, while the public key (and/or its associate certificate) 
        // could be exchanged over the network, the private key should not. However, we may keep the
        // same schema for its URI.
        this.signatureOperation = new AsymmetricSigningOperation(trustModel, URI.create("ndn://com/intel/test/signKey"), URI.create("ndn://com/intel/test/cert"), format, SecurityConstants.SHA1withRSA);
    }
}
