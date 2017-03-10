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
package com.intel.icecp.core.mock;

import com.intel.icecp.core.security.crypto.key.Key;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.core.security.keymanagement.IcecpKeyManager;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.KeyGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mock key manager; it keeps all the keys and certificates inside two 
 * hash maps.
 *
 */
public class MockKeyManager implements IcecpKeyManager {

    private static final Logger LOGGER = LogManager.getLogger();
    
    public static final URI DEFAULT_ID_PUB_KEY  = URI.create("ndn://com/intel/node_id/pub_key");
    public static final URI DEFAULT_ID_PRV_KEY  = URI.create("ndn://com/intel/node_id/prv_key");
    public static final URI DEFAULT_SYMM_KEY    = URI.create("ndn://com/intel/channel/channel_id/symm_key");
    public static final URI DEFAULT_CERTIFICATE = URI.create("ndn://com/intel/node_id/cert");
    
    private static final String NOT_FOUND = " not found";
    
    private static final String CA_CERT = "-----BEGIN CERTIFICATE-----\n"
            + "MIID5zCCAs+gAwIBAgIJALijwlx4UHG2MA0GCSqGSIb3DQEBCwUAMIGJMQswCQYD\n"
            + "VQQGEwJVUzELMAkGA1UECAwCT1IxEjAQBgNVBAcMCUhpbGxzYm9ybzEOMAwGA1UE\n"
            + "CgwFSW50ZWwxDTALBgNVBAsMBElvdEcxEDAOBgNVBAMMB0lvVEdfQ0ExKDAmBgkq\n"
            + "hkiG9w0BCQEWGWFtYnJvc2luLm1vcmVub0BpbnRlbC5jb20wHhcNMTUwOTI4MjMx\n"
            + "MTQwWhcNMTYwOTI3MjMxMTQwWjCBiTELMAkGA1UEBhMCVVMxCzAJBgNVBAgMAk9S\n"
            + "MRIwEAYDVQQHDAlIaWxsc2Jvcm8xDjAMBgNVBAoMBUludGVsMQ0wCwYDVQQLDARJ\n"
            + "b3RHMRAwDgYDVQQDDAdJb1RHX0NBMSgwJgYJKoZIhvcNAQkBFhlhbWJyb3Npbi5t\n"
            + "b3Jlbm9AaW50ZWwuY29tMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA\n"
            + "w3vEa3BPb54MVhCI4HcJ0YKF+tLjbGHXmbYft5329ba4wNHaEn628SfZJx+dmDNv\n"
            + "yR4Lh39LZgTNm7uHqH52mtYBfgrZ81bcyTLiECGbpfo/578It6AQy0Z5etxCFjO4\n"
            + "20TfdLcWAJJXLzcB1KBqkYfsolCfh9Ktb8KU+4RCckZ/kE8eeUNUDbg2iLWrAg1w\n"
            + "9cTxXO04BQUs63WfGejN+Xg07MTVmhbPkOL+v4GWe4wYLkdQRBX4UhRvWE2RccaN\n"
            + "qKLdY5aVFXZl9EfrgWUhVouFnq23IeH+Sf4THFiWRDCkMJ8QqT5qKfyRCNK0RqUR\n"
            + "j52rPt4kepD/uVAqayB2EwIDAQABo1AwTjAdBgNVHQ4EFgQUJCr1dC1dzOb3TkV0\n"
            + "kPe51rzVYIIwHwYDVR0jBBgwFoAUJCr1dC1dzOb3TkV0kPe51rzVYIIwDAYDVR0T\n"
            + "BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAwwsQaBZDywZGx3OIdJHs1o80soT4\n"
            + "l0rdwdJnGUNY/ZO04LGUYPr8JjKeZjIagHMWw3fHANMAosgbIQ89MQs6tlxaHq1N\n"
            + "G2x08d5+9iUmnkvxlIh5UTyEhnlXDYqrZqbTB7F1B2fxVMXyLLXsscdHVBJHfet3\n"
            + "n46BWiqznAUiS+DGXHcsUiAfJfOGdCYDdNPs23yot81yFSnFrmBHWlcozLXZIF2i\n"
            + "n/Csz7AsqsXkPVLzPk16NJgsvG7cydHjowFsSmfBQ8xrzgPhYfIzjqiVq3xeSvHI\n"
            + "1dY8dGEuXne3DXsVLgdd2qNdo2f9VQ5paF3ANrXugD55+p37le9s91JzuA==\n"
            + "-----END CERTIFICATE-----";
    
    private final Map<URI, Key> keystore = new HashMap<>();
    
    private final Map<URI, Certificate[]> truststore = new HashMap<>();
    
   
    /**
     * Decodes a chain of X.509 certificates
     *
     * @param certificateChain Bytes of the certificates chain (in PEM format) to decode
     * @return The decoded certificates chain (may be empty)
     */
    private static List<Certificate> decodeX509CertificateChain(byte[] certificateChain) {
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
    
    
    public MockKeyManager init() throws NoSuchAlgorithmException {
        // Create and add a default RSA key pair inside the keystore
        java.security.KeyPairGenerator keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024, new SecureRandom());
        java.security.KeyPair keyPair = keyPairGenerator.generateKeyPair();
        
        keystore.put(DEFAULT_ID_PUB_KEY, new PublicKey(keyPair.getPublic()));
        keystore.put(DEFAULT_ID_PRV_KEY, new PrivateKey(keyPair.getPrivate()));
        
        // Create and add a default AES symmetric key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SymmetricKey sk = new SymmetricKey(keyGen.generateKey());
        keystore.put(DEFAULT_SYMM_KEY, sk);
        
        // Add a certificate chain to truststore
        List certs = decodeX509CertificateChain(CA_CERT.getBytes());
        Certificate[] certsArray = Arrays.copyOf(certs.toArray(), certs.size(), Certificate[].class);
        truststore.put(DEFAULT_CERTIFICATE, certsArray);
        return this;
    }
    
    
    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public PublicKey getPublicKey(URI keyId) throws KeyManagerException {
        try {
            if (keystore.containsKey(keyId)) {
                return (PublicKey) keystore.get(keyId);
            }
        } catch (ClassCastException ex) {
            throw new KeyManagerException("Key " + keyId + " is not a public key", ex);
        }
        throw new KeyManagerException("Key " + keyId + NOT_FOUND);
    }

    private <T> T getKeyOfType(URI keyId, Class<T> desiredClass) throws KeyManagerException {
        if (keystore.containsKey(keyId)) {
            Key theKey = keystore.get(keyId);
            if (desiredClass.isAssignableFrom(theKey.getClass())) {
                return (T) theKey;
            } else {
                throw new KeyManagerException("Key " + keyId + " is not the correct type; type is <" + theKey.getClass() + ">; desired is <" + desiredClass + ">");
            }
        } else {
            throw new KeyManagerException("Key " + keyId + NOT_FOUND);
        }
    }

    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public PrivateKey getPrivateKey(URI keyId) throws KeyManagerException {
        return getKeyOfType(keyId, PrivateKey.class);
    }

    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public SymmetricKey getSymmetricKey(URI keyId) throws KeyManagerException {
        return getKeyOfType(keyId, SymmetricKey.class);
    }

    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public void addSymmetricKey(URI keyId, SymmetricKey k) throws KeyManagerException {
        keystore.put(keyId, k);
    }

    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public void deleteSymmetricKey(URI keyId) throws KeyManagerException {
        keystore.remove(keyId);
    }

    /**
     * {@inheritDoc }
     * 
     */
    @Override
    public Certificate getCertificate(URI certificateID) throws KeyManagerException {
        try {
            if (truststore.containsKey(certificateID)) {
                return truststore.get(certificateID)[0];
            }
        } catch (ClassCastException ex) {
            throw new KeyManagerException("Cert " + certificateID + " is not a symmetric key", ex);
        }
        throw new KeyManagerException("Cert " + certificateID + NOT_FOUND);
    }

    /**
     * Always return the certificate we have in {@link #truststore}
     * {@inheritDoc }
     * 
     */
    @Override
    public Certificate verifyCertificateChain(byte[] certificate) throws KeyManagerException {
        return truststore.get(DEFAULT_CERTIFICATE)[0];
    }
    
}
