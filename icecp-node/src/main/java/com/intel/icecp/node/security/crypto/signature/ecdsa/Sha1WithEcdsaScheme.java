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

import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.node.security.crypto.signature.AsymmetricSignatureScheme;

/**
 * SHA-1 based ECDSA signature scheme
 *
 */
public class Sha1WithEcdsaScheme extends AsymmetricSignatureScheme {

    /**
     * 
     * {@inheritDoc }
     */
    @Override
    public String id() {
        return SecurityConstants.SHA1withECDSA;
    }
    
}
