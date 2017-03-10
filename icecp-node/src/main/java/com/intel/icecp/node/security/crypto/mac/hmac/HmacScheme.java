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

package com.intel.icecp.node.security.crypto.mac.hmac;

import com.intel.icecp.core.security.crypto.exception.mac.MacError;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.core.security.crypto.mac.MacScheme;
import com.intel.icecp.node.security.crypto.utils.CryptoUtils;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Thin wrapper around {@link javax.crypto.Mac} Generic Hash-based MAC.
 *
 */
public abstract class HmacScheme implements MacScheme<SymmetricKey> {

    /**
     * {@inheritDoc }
     */
    @Override
    public byte[] computeMac(byte[] data, SymmetricKey key) throws MacError {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance(this.id());
            mac.init(key.getWrappedKey());
            byte[] macValue = mac.doFinal(data);
            return macValue;
        } catch (NoSuchAlgorithmException | ClassCastException | InvalidKeyException ex) {
            throw new MacError("Error in computing the HMAC.", ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void verifyMac(byte[] macBytes, byte[] data, SymmetricKey key) throws MacError {
        if (CryptoUtils.compareBytes(macBytes, computeMac(data, key)) == false) {
            throw new MacError("HMAC verification failed.");
        }
    }
}