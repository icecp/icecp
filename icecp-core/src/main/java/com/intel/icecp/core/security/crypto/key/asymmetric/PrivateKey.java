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

import com.intel.icecp.core.security.crypto.key.SecretKey;

/**
 * Wrapper for {@link java.security.PrivateKey}
 *
 */
public class PrivateKey implements SecretKey {

    /** Private key */
    protected java.security.PrivateKey key;

    public PrivateKey(java.security.PrivateKey key) {
        this.key = key;
    }
    
    /**
     * Getter for wrapped key
     * 
     * @return The wrapped key
     */
    public java.security.PrivateKey getKey() {
        return key;
    }

    /**
     * Setter for wrapped key
     * 
     * @param key The key to wrap
     */
    public void setKey(java.security.PrivateKey key) {
        this.key = key;
    }


}
