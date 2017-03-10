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
package com.intel.icecp.core.security.crypto.mac;

import com.intel.icecp.core.security.SecurityService;
import com.intel.icecp.core.security.crypto.key.SecretKey;
import com.intel.icecp.core.security.crypto.exception.mac.MacError;

/**
 * Interface for a generic Message Authentication Code (MAC)
 *
 * @param <S> MAC key type
 */
public interface MacScheme<S extends SecretKey> extends SecurityService<String> {

    /**
     * Given data byte and a secret key, produces a MAC
     *
     * @param data Data on which compute a MAC
     * @param key MAC key
     * @return MAC bytes
     * @throws MacError In case of error in computing the MAC
     */
    byte[] computeMac(byte[] data, S key) throws MacError;

    /**
     * Given MAC, the data, and a secret key, returns true iif the MAC is
     * verified
     *
     * @param macBytes MAC bytes
     * @param data Data on which verify the MAC
     * @param key Key to use for verification
     * @throws MacError In case of error in MAC verification
     */
    void verifyMac(byte[] macBytes, byte[] data, S key) throws MacError;

}
