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
