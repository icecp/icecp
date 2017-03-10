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
