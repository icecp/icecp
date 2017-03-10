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
package com.intel.icecp.node.security.utils;

import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import com.intel.icecp.node.security.crypto.utils.CryptoUtils;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides utility methods for PEM encoding/decoding of 
 * keys and certificates, as defined in 
 * http://tools.ietf.org/html/rfc7468#page-11
 *
 */
public class PemEncodingUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    // Number of characters per line
    private static final int CHARS_PER_LINE = 64;
    
    // Headers and footers
    private static final String X509_PEM_CERT_HEADER = "-----BEGIN CERTIFICATE-----";
    private static final String X509_PEM_CERT_FOOTER = "-----END CERTIFICATE-----";

    private static final String PUB_KEY_HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String PUB_KEY_FOOTER = "-----END PUBLIC KEY-----";

    private static final String PRIV_KEY_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIV_KEY_FOOTER = "-----END PRIVATE KEY-----";

    
    /**
     * Hides the default public constructor
     */
    private PemEncodingUtils() {
        
    }
    
    /**
     * Utility method that builds a Public Key from PEM format
     *
     * @param pemPublicKey PEM encoded public key (in bytes)
     * @return Decoded RSA public key, or NULL
     */
    public static PublicKey decodeRSAPublicKey(byte[] pemPublicKey) {
        PublicKey publicKey = null;

        String pk = new String(pemPublicKey);
        // Remove headers and '\n'
        pk = pk.replace(PUB_KEY_HEADER + "\n", "");
        pk = pk.replace(PUB_KEY_FOOTER, "");
        pk = pk.replace("\n", "");

        try {
            byte[] encoded = CryptoUtils.base64Decode(pk.getBytes());
            X509EncodedKeySpec spec = new X509EncodedKeySpec(encoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            publicKey = new PublicKey(kf.generatePublic(spec));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
            LOGGER.error("Error decoding the given Public Key", ex);
        }
        return publicKey;
    }

    /**
     * Utility method that builds a Private Key from PEM format
     *
     * @param pemPrivateKey PEM encoded private key (in bytes)
     * @return Decoded private key
     */
    public static PrivateKey decodeRSAPrivateKey(byte[] pemPrivateKey) {
        PrivateKey privateKey = null;

        String pk = new String(pemPrivateKey);
        // Remove headers and '\n'
        pk = pk.replace(PRIV_KEY_HEADER + "\n", "");
        pk = pk.replace(PRIV_KEY_FOOTER, "");
        pk = pk.replace("\n", "");

        try {
            byte[] encoded = CryptoUtils.base64Decode(pk.getBytes());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privateKey = new PrivateKey(kf.generatePrivate(new PKCS8EncodedKeySpec(encoded)));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
            LOGGER.debug("Error decoding the given Private Key", ex);
        }
        return privateKey;
    }

    /**
     * Utility method that given a Private Key returns a PEM formatted string in
     * bytes
     *
     * @param privateKey The private key to encode
     * @return PEM encoded private key bytes
     */
    public static byte[] encodePrivateKey(PrivateKey privateKey) {
        if (privateKey != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(PRIV_KEY_HEADER).append("\n")
                    .append(new String(CryptoUtils.base64Encode(privateKey.getKey().getEncoded())).replaceAll("(.{" + CHARS_PER_LINE + "})", "$1\n")).append("\n")
                    .append(PRIV_KEY_FOOTER).append("\n");
            return sb.toString().getBytes();
        }
        return new byte[0];
    }

    /**
     * Utility method that given a Private Key returns a PEM formatted string in
     * bytes
     *
     * @param publicKey The public key to encode
     * @return PEM encoded public key bytes
     */
    public static byte[] encodePublicKey(PublicKey publicKey) {
        if (publicKey != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(PUB_KEY_HEADER).append("\n")
                    .append(new String(CryptoUtils.base64Encode(publicKey.getPublicKey().getEncoded())).replaceAll("(.{" + CHARS_PER_LINE + "})", "$1\n")).append("\n")
                    .append(PUB_KEY_FOOTER).append("\n");
            return sb.toString().getBytes();
        }
        return new byte[0];
    }

    /**
     * Encodes a certificate into PEM format
     *
     * @param certificate Input certificate
     * @return bytes representing an encoded X.509 certificate in PEM format
     * @throws CertificateEncodingException
     */
    public static byte[] encodeCertificate(Certificate certificate) throws CertificateEncodingException {
        if (certificate != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(X509_PEM_CERT_HEADER).append("\n")
                    .append(new String(CryptoUtils.base64Encode(certificate.getEncoded())).replaceAll("(.{" + CHARS_PER_LINE + "})", "$1\n")).append("\n")
                    .append(X509_PEM_CERT_FOOTER).append("\n");
            return sb.toString().getBytes();
        }
        return new byte[0];
    }

    /**
     * Decodes a chain of X.509 certificates
     *
     * @param certificateChain Bytes of the certificates chain (in PEM format) to decode
     * @return The decoded certificates chain (may be empty)
     */
    public static List<Certificate> decodeX509CertificateChain(byte[] certificateChain) {
        // List of certificates
        List<Certificate> certs = new ArrayList<>();
        // Certificate factory, to reconstruct the certificate
        CertificateFactory cf;
        try {
            // Read the serialized certificate field.
            InputStream is = new BufferedInputStream(new ByteArrayInputStream(certificateChain));
            cf = CertificateFactory.getInstance("X.509");
            // Read all the certificates one by one (may be more that one)
            while (is.available() > 0) {
                certs.add(cf.generateCertificate(is));
            }
        } catch (CertificateException | IOException ex) {
            // If something goes wrong, we return an empty chain
            LOGGER.warn("Unable to parse the given certificate chain bytes.", ex);
        }
        return certs;
    }

}
