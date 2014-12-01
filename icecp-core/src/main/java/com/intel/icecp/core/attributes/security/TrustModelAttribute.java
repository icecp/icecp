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

/**
 * Attribute identifying a trust model
 *
 */
public class TrustModelAttribute extends BaseAttribute<TrustModelAttribute.TrustModelSpecs>{
    
    public static final String ATTRIBUTE_NAME = "trustModel";
    
    /** Trust model specs */
    private final TrustModelSpecs specs;

    /**
     * Wrapper class for unique trust model specs
     * 
     */
    public static class TrustModelSpecs implements Serializable{
        /** Unique trust model details */
        public final String trustModelId;

        public TrustModelSpecs(String trustModelId) {
            this.trustModelId = trustModelId;
        }
    }
    
    public TrustModelAttribute(TrustModelSpecs specs) {
        super(ATTRIBUTE_NAME, TrustModelSpecs.class);
        this.specs = specs;
    }
    
    /**
     * Simply return the value of {@link #specs}
     * 
     * {@inheritDoc } 
     */
    @Override
    public TrustModelSpecs value() {
        return specs;
    }
    
}
