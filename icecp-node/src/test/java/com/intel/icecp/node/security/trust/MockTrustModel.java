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
package com.intel.icecp.node.security.trust;

import com.intel.icecp.core.security.crypto.exception.key.InvalidKeyTypeException;
import com.intel.icecp.core.security.crypto.key.asymmetric.KeyPair;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import com.intel.icecp.core.security.trust.TrustModel;
import com.intel.icecp.core.security.trust.exception.TrustModelException;
import com.intel.icecp.node.security.crypto.key.KeyProvider;
import java.net.URI;

/**
 * Mock trust model that holds a key pair
 *
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
