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
package com.intel.icecp.core.attributes.security;

import com.intel.icecp.core.attributes.BaseAttribute;
import java.io.Serializable;
import java.net.URI;

/**
 * Specifies encryption-related attributes, such as algorithm to use, and
 * key characteristics.
 *
 */
public class EncryptAttribute extends BaseAttribute<EncryptAttribute.EncryptionSpecs>{
    
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
    
    
    public EncryptAttribute(EncryptionSpecs encryptionSpecs) {
        super(ATTRIBUTE_NAME, EncryptionSpecs.class);
        this.encryptionSpecs = encryptionSpecs;
    }
    
    
    public EncryptAttribute(String encryptionAlgorithm, URI keyId, String keyAlgorithm, int keySize) {
        this(new EncryptionSpecs(encryptionAlgorithm, keyId, keyAlgorithm, keySize));
    }
    
    
    /**
     * Simply return the value of {@link #encryptionSpecs}
     * 
     * {@inheritDoc }
     */
    @Override
    public EncryptAttribute.EncryptionSpecs value() {
        return encryptionSpecs;
    }

    
}
