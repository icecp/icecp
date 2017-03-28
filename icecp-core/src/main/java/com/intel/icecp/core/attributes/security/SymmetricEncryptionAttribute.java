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
package com.intel.icecp.core.attributes.security;

import com.intel.icecp.core.attributes.BaseAttribute;
import java.io.Serializable;
import java.net.URI;

/**
 * Specifies encryption-related attributes, such as algorithm to use, and
 * key characteristics.
 *
 */
public class SymmetricEncryptionAttribute extends BaseAttribute<SymmetricEncryptionAttribute.EncryptionSpecs>{
    
    public static final String ATTRIBUTE_NAME = "encryption";
    
    /** Algorithm specs */
    private final EncryptionSpecs encryptionSpecs;
    
    /**
     * Serializable key specifications
     * 
     */
    public static class EncryptionSpecs implements Serializable {
        /** 
         * Encryption specification comprising:
         *  <ul>
         *      <li> Encryption algorithm </li>
         *      <li> Key unique ID (e.g., for retrieval from key manager) </li>
         *      <li> Key algorithm (not always == encryption algorithm) </li>
         *      <li> Key Size (in bits) </li>
         *  </ul>
         */
        public final String encryptionAlgorithm;
        public final URI keyId;
        public final String keyAlgorithm;
        public final int keySize;

        public EncryptionSpecs(String encryptionAlgorithm, URI keyId, String keyAlgorithm, int keySize) {
            this.encryptionAlgorithm = encryptionAlgorithm;
            this.keyId = keyId;
            this.keyAlgorithm = keyAlgorithm;
            this.keySize = keySize;
        }
    }
    
    
    public SymmetricEncryptionAttribute(EncryptionSpecs encryptionSpecs) {
        super(ATTRIBUTE_NAME, EncryptionSpecs.class);
        this.encryptionSpecs = encryptionSpecs;
    }
    
    
    public SymmetricEncryptionAttribute(String encryptionAlgorithm, URI keyId, String keyAlgorithm, int keySize) {
        this(new EncryptionSpecs(encryptionAlgorithm, keyId, keyAlgorithm, keySize));
    }
    
    
    /**
     * Simply return the value of {@link #encryptionSpecs}
     * 
     * {@inheritDoc }
     */
    @Override
    public SymmetricEncryptionAttribute.EncryptionSpecs value() {
        return encryptionSpecs;
    }

    
}
