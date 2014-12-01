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
 * Contains all signature details
 *
 */
public class SignAttribute extends BaseAttribute<SignAttribute.SigningDetails> {
    
    public static final String ATTRIBUTE_NAME = "sign";
    
    /** *  Attribute value wrapped in a {@link SigningDetails} class */
    private final SigningDetails signingDetails;

    /**
     * Wrapper class for all the signature details
     * 
     */
    public static class SigningDetails implements Serializable {
        private static final long serialVersionUID = 1L;
        /** Unique algorithm ID, e.g., "SHA256withRSA" */
        public final String algorithmId;
        /** Unique identifier of the key, as a URI such as: "ndn:/where/to/fetch/the/key" */
        public final URI signingKeyId;
        public final URI verificationKey;
        
        public SigningDetails(String algorithmId, URI signingKeyId, URI verificationKey) {
            this.algorithmId = algorithmId;
            this.signingKeyId = signingKeyId;
            this.verificationKey = verificationKey;
        }
    }
    
    public SignAttribute(SigningDetails signingDetails) {
        super(ATTRIBUTE_NAME, SigningDetails.class);
        this.signingDetails = signingDetails;
    }
    
    public SignAttribute(String algorithmId, URI signingKeyId, URI verificationKey) {
        this(new SigningDetails(algorithmId, signingKeyId, verificationKey));
    }
    
    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public SigningDetails value() {
        return signingDetails;
    }
    
}
