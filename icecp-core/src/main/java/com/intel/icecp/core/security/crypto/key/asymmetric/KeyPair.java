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
package com.intel.icecp.core.security.crypto.key.asymmetric;

/**
 * Represents a key pair.
 *
 */
public class KeyPair {

    /** Public key */
    private PublicKey publicKey;
    /** Private key */
    private PrivateKey privateKey;

    /**
     * Getter for {@link KeyPair#publicKey}
     * 
     * @return The public key
     */
    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    /**
     * Getter for {@link KeyPair#privateKey}
     * 
     * @return The private key
     */
    public PrivateKey getPrivateKey() {
        return this.privateKey;
    }

    /**
     * Setter for {@link KeyPair#publicKey}
     * 
     * @param publicKey Public key to set
     */
    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    /**
     * Setter for {@link KeyPair#privateKey}
     * 
     * @param privateKey Private key to set
     */
    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

}
