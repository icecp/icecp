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
package com.intel.icecp.core.security.crypto.key.symmetric;

import com.intel.icecp.core.security.crypto.key.SecretKey;
import java.util.Objects;

/**
 * Symmetric key as a wrapper around {@link javax.crypto.SecretKey}
 *
 */
public class SymmetricKey implements SecretKey {

    /** Symmetric key instance */
    private final javax.crypto.SecretKey key;

    public SymmetricKey(javax.crypto.SecretKey key) {
        this.key = key;
    }

    /**
     * Returns the key in an encoded format
     * 
     * @return Encoded wrapped key bytes
     */
    public byte[] getEncoded() {
        return this.key.getEncoded();
    }

    /**
     * Getter for wrapped key
     * 
     * @return The wrapped key
     */
    public javax.crypto.SecretKey getWrappedKey() {
        return this.key;
    }

    /**
     * Returns the String name of the algorithm for which the key is used
     *
     * @return A String indicating the algorithm for which we use the key
     */
    public String getKeyType() {
        return this.key.getAlgorithm();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SymmetricKey)) {
            return false;
        }
        SymmetricKey k = (SymmetricKey) o;
        return this.getWrappedKey().equals(k.getWrappedKey());

    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + Objects.hashCode(this.key);
        return hash;
    }
}
