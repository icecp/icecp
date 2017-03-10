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
package com.intel.icecp.core.security;

import com.intel.icecp.core.channels.Token;
import com.intel.icecp.core.security.crypto.cipher.Cipher;
import com.intel.icecp.core.security.crypto.mac.MacScheme;
import com.intel.icecp.core.security.crypto.signature.SignatureScheme;
import com.intel.icecp.core.security.crypto.exception.cipher.UnsupportedCipherException;
import com.intel.icecp.core.security.crypto.exception.mac.UnsupportedMacAlgorithmException;
import com.intel.icecp.core.security.crypto.exception.siganture.UnsupportedSignatureAlgorithmException;
import com.intel.icecp.core.security.crypto.key.Key;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import javax.crypto.SecretKey;

/**
 * Main entry point to retrieve cryptographic services. This class (lazily)
 * loads the available cryptographic tools (using {@link SecurityServices}), and
 * provides getter methods to retrieve them.
 *
 *
 */
public class CryptoProvider {

    /**
     * Security services used to load signatures, ciphers and MAC schemes
     */
    private static final SecurityServices<String, SignatureScheme<? extends SecretKey, ? extends PublicKey>> signatureSchemes
            = new SecurityServices<>(Token.of(SignatureScheme.class));
    private static final SecurityServices<String, Cipher<? extends Key, ? extends Key>> ciphers
            = new SecurityServices<>(Token.of(Cipher.class));
    private static final SecurityServices<String, MacScheme<? extends SecretKey>> macSchemes
            = new SecurityServices<>(Token.of(MacScheme.class));

    /**
     * Hides the default public constructor
     */
    private CryptoProvider() {
    }

    /**
     * Returns an instance of the (already loaded) service implementing the
     * signature scheme identified by the supplied name; throws an exception if
     * this cannot be found or instantiated
     *
     * @param name Name of the signature scheme wanted
     * @param force Forces service loading (e.g., to be used when new services
     * are added at runtime by a module)
     * @return An instance of the requested service
     * @throws UnsupportedSignatureAlgorithmException If the given scheme is not
     * supported or a new instance can not be created
     */
    public static SignatureScheme getSignatureScheme(String name, boolean force) throws UnsupportedSignatureAlgorithmException {
        SignatureScheme signature = signatureSchemes.get(name, force);
        if (signature != null) {
            return signature;
        }
        throw new UnsupportedSignatureAlgorithmException("Unsupported signature '" + name + "'");
    }

    /**
     * Returns an instance of the (already loaded) service implementing the
     * signature scheme identified by the supplied name; throws an exception if
     * this cannot be found or instantiated
     *
     * @param name Name of the signature scheme wanted
     * @return An instance of the requested service
     * @throws UnsupportedSignatureAlgorithmException If the given scheme is not
     * supported or a new instance can not be created
     */
    public static SignatureScheme getSignatureScheme(String name) throws UnsupportedSignatureAlgorithmException {
        return getSignatureScheme(name, false);
    }

    /**
     * Returns an instance of a service representing the cipher, or throws an
     * exception if it cannot be instantiated
     *
     * @param name String representation of the Cipher
     * @param force Force reloading of the available services
     * @return An instance of the requested cipher service
     * @throws UnsupportedCipherException In case the service is not supported
     */
    public static Cipher getCipher(String name, boolean force) throws UnsupportedCipherException {
        Cipher cipher = ciphers.get(name, force);
        if (cipher != null) {
            return cipher;
        }
        throw new UnsupportedCipherException("Unsupported cipher '" + name + "'");
    }

    /**
     * Returns an instance of an already loaded service representing the cipher,
     * or throws an exception if it cannot be instantiated
     *
     * @param name String representation of the Cipher
     * @return An instance of the requested cipher service
     * @throws UnsupportedCipherException In case the service is not supported
     */
    public static Cipher getCipher(String name) throws UnsupportedCipherException {
        return getCipher(name, false);
    }

    /**
     * Returns an instance of a service representing the MAC scheme, or throws
     * an exception if it cannot be instantiated
     *
     * @param name String representation of the Mac scheme
     * @param force Force reloading of the available services
     * @return An instance of the requested Mac scheme service
     * @throws UnsupportedMacAlgorithmException In case the service is not
     * supported
     */
    public static MacScheme getMacScheme(String name, boolean force) throws UnsupportedMacAlgorithmException {
        MacScheme macScheme = macSchemes.get(name, force);
        if (macScheme != null) {
            return macScheme;
        }
        throw new UnsupportedMacAlgorithmException("Unsupported MAC '" + name + "'");

    }

    /**
     * Returns an instance of an already loaded service representing the MAC
     * scheme, or throws an exception if it cannot be instantiated
     *
     * @param name String representation of the Mac scheme
     * @return An instance of the requested Mac scheme service
     * @throws UnsupportedMacAlgorithmException In case the service is not
     * supported
     */
    public static MacScheme getMacScheme(String name) throws UnsupportedMacAlgorithmException {
        return getMacScheme(name, false);
    }

}
