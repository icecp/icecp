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
package com.intel.icecp.node.security.tpm.tpm12;

import com.intel.icecp.core.security.CryptoProvider;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.node.security.tpm.TpmManagerProvider;
import com.intel.icecp.core.security.keymanagement.DeapKeyManager;
import com.intel.icecp.node.security.tpm.TpmManager;
import com.intel.icecp.node.security.tpm.data.SealedData;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.node.security.tpm.exception.TpmOperationError;
import com.intel.icecp.node.security.tpm.exception.UnsupportedTpmTypeException;
import com.intel.icecp.node.messages.security.tpm.tpm1_2.TpmAttestationIdentityCredetialsResponse;
import com.intel.icecp.node.messages.security.tpm.tpm1_2.TpmAttestationIdentityRequest;
import com.intel.icecp.node.security.RandomBytesGenerator;
import com.intel.icecp.node.security.crypto.key.KeyProvider;
import com.intel.icecp.node.security.utils.PEMEncodingUtils;
import java.security.cert.Certificate;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @TODO: Improve this integration test
 * 
 * Test for Tpm1_2Manager class
 *
 */
@Ignore
public class Tpm1_2ManagerTest {

    private static final byte[] OWNER_PASSWORD = "1234".getBytes();
    private static final byte[] AIK_SECRET = RandomBytesGenerator.getRandomBytes(20);

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

    private static final String CA_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n"
            + "MIIEogIBAAKCAQEAw3vEa3BPb54MVhCI4HcJ0YKF+tLjbGHXmbYft5329ba4wNHa\n"
            + "En628SfZJx+dmDNvyR4Lh39LZgTNm7uHqH52mtYBfgrZ81bcyTLiECGbpfo/578I\n"
            + "t6AQy0Z5etxCFjO420TfdLcWAJJXLzcB1KBqkYfsolCfh9Ktb8KU+4RCckZ/kE8e\n"
            + "eUNUDbg2iLWrAg1w9cTxXO04BQUs63WfGejN+Xg07MTVmhbPkOL+v4GWe4wYLkdQ\n"
            + "RBX4UhRvWE2RccaNqKLdY5aVFXZl9EfrgWUhVouFnq23IeH+Sf4THFiWRDCkMJ8Q\n"
            + "qT5qKfyRCNK0RqURj52rPt4kepD/uVAqayB2EwIDAQABAoIBAHLu3fldOxchEZe4\n"
            + "eQGge4FXAHcMbvJWRHD3h4Zptb2aBN45g8HEBsOa7i32hK3r8BmLNLQhv67nvAuI\n"
            + "IqaKVCmNEauNrb9Int8cr5VUbTMVA2W6B+IOllEtylbWEXUES/d/cvIogyMq51+3\n"
            + "M7hT6NRP2m1EgcdVsus2uGl4xahxivUE3wlD7S4MVxYx+VRBINLgPnrdO8ONPaBz\n"
            + "+wx1KJuT3cn/rICkHDh2EyHmcVyIgsuQ24tqO95WLm1BFFwCDIVBR532kNH+fQ0U\n"
            + "CWbEA7W0Ah5qleYkhFp5rrBlh/jJwVM/Wx7PTILtssF6YFdSPwjp9EL1cvmmGEAh\n"
            + "amsrPmECgYEA79ri0Y9UEPW14DcgmnSnAko920QwMMfCFuMIu3UGxIEsHA9KSer2\n"
            + "97wYBiJ5cO5G8XEkbHesssTALp8DhpADQNT/IkXcOjGVhl/ZO4fiYySfgpVV7XO+\n"
            + "to9VB4ut5ffz9psiRrhZDd6unwC1A6tKxJjUJ6/IPVAXq1Gpck5vnmUCgYEA0KRI\n"
            + "ef93rvn4C4TWyd+wAx6lFLGuZPcYt5l9Q0/yZnqGxC8OINlm0e1+Hje3XNXVubnz\n"
            + "FjXFQOyWhs5/5mMCXBy1sKaF7hZL3zXsX0X8LPngX4Cig/flE87Dqni/XJ+Q0/qF\n"
            + "s9DiGKUq4aPkmBEJoQAwMW9le7OK1IlI1pcPHxcCgYAm+jEF0P7eSq+log/ASdih\n"
            + "/KKUsT4Lj0qxIW/X1qqk4EDkxm9SF8bOd1iIFq4Zdf89WR+MKN59po98hAa0pU6C\n"
            + "CJr1XNju9APSdFz+2ZQmfFsXg2EVV0vUqvIsabx2tJaGqqslRuvh3yANYrYHxJw/\n"
            + "2n4PfPdTT2KpSnn4w1pMKQKBgG1sK4KoI9nFyXwpEAjR4trQAJ4IzcCwAuuPeS2L\n"
            + "SULLsdBEKXG68vzYRFOOvwDWOP/t8PpN+wSg0BUlSdjHRl6OxA9AVm9WjDYlbrFM\n"
            + "4AOqjS0pJpJ1uVOZFe9a4mmuWeOCuQpkW5+3R/UM4n+KN/WTRrM2jA/DSuVKlsPM\n"
            + "d6bDAoGAFlDSjWsHBF01R3bo1bddDLZAV2iIDsyssQAogmOq5Tp9xY0ibuyPn60I\n"
            + "4a6SWEdREbPeSMv5MMXhldrs09uDc8epgcLuDZaKCKTztkCbv5bsfs01b9WHsR+k\n"
            + "y8wNzXarJvUcUWQbYUmraJ+PSJT65oDtnTEOrybMI+idRxXhGsU=\n"
            + "-----END PRIVATE KEY-----";
    
    
    private static final String BASE_DIR = "src/test/resources/fixtures";
    

    // Manager
    private TpmManager manager;
           
    
    /**
     * Create required files
     * 
     * @throws Exception 
     */
    @BeforeClass
    public static void createFiles() throws Exception {
        // Create a file to seal (content is not important)
        try(FileOutputStream os = new FileOutputStream(BASE_DIR + "/fileToSeal.txt")) {
            os.write(CA_CERT.getBytes());
        }
    }
    
    /**
     *  Cleanup operations
     */
    @AfterClass
    public static void cleanup() {
        // Delete file to seal
        new File(BASE_DIR + "/fileToSeal.txt").delete();
    }
    
    /**
     * Get a TPM manager instance
     * 
     * @throws Exception 
     */
    @Before
    public void init() throws Exception {
        manager = TpmManagerProvider.getTpmManager(SecurityConstants.TPM_1_2_SERVICE, false);
    }

    /**
     * Test for {@link TpmManager#sealData(java.io.InputStream, byte[]) }
     * 
     * @throws TpmOperationError
     * @throws IOException 
     */
    @Test
    public void dataSealTest() throws TpmOperationError, IOException {
        
        // Try to seal data bound to PCR values
        SealedData sd = manager.sealData(
                new FileInputStream(new File(BASE_DIR + "/fileToSeal.txt")),
                SecurityConstants.getTpmToolsSealSecret());

        Assert.assertNotNull(sd);
    }
    
    /**
     * Test for {@link Tpm1_2Manager#unsealData(java.io.InputStream, byte[]) }
     * from a given sealed file using {@literal IBM tpm tools}
     * 
     * NOTE THAT: FILES MUST BE CREATED BEFORE THIS WORKS
     * 
     * @throws Exception 
     */
    @Test
    public void unsealDataFromSealedFileTest() throws Exception {
        byte[] unsealedBytes;
        // Test 1: Compatibility with tpm_tools (maintained only in unsealing)
		try(FileInputStream is = new FileInputStream(new File(BASE_DIR + "/fileToSeal.txt"))) {
            unsealedBytes = manager.unsealData(
				is, 
				SecurityConstants.getTpmToolsSealSecret());
            Assert.assertNotNull(unsealedBytes);
        }
    }
    
    /**
     * Test for {@link Tpm1_2Manager#unsealData(java.io.InputStream, byte[]) }
     * from a given sealed file using {@literal IBM tpm tools}
     * 
     * NOTE THAT: FILES MUST BE CREATED BEFORE THIS WORKS
     * 
     * @throws Exception 
     */
    @Test
    public void unsealDataTest() throws Exception {
        // Test 2: Unseal our own sealed data.
        SealedData sd = manager.sealData(
                new FileInputStream(new File(BASE_DIR + "/fileToSeal.txt")),
                SecurityConstants.getTpmToolsSealSecret());

        String s = new String(SealedData.encode(sd));

        byte[] unsealedBytes = manager.unsealData(
                new ByteArrayInputStream(s.getBytes()),
                SecurityConstants.getTpmToolsSealSecret());

        // If all is correct, should be not null
        Assert.assertNotNull(unsealedBytes);

    }

    /**
     * Creates an identity request
     *
     * @return
     * @throws Exception
     */
    private TpmAttestationIdentityRequest createIdentityRequest() throws Exception {
        // SETUP

//        DeapKeyManager keyManager = KeyStoreBasedManagerProvider__OLD.getKeyManager(SecurityConstants.KEY_STORE_BASED_MANAGER, false);

        int contextId = Tss1_2NativeInterface.createTpmContext();

        Certificate caCertificate = PEMEncodingUtils.decodeX509CertificateChain(CA_CERT.getBytes()).get(0); 
        byte[] CACertificateBytes = CA_CERT.getBytes();
        byte[] CAPrivateKeyBytes = CA_PRIVATE_KEY.getBytes();

        // Load SRK
        Tss1_2NativeInterface.loadSrk(contextId, null);

        // Set TPM owner secret
        Tss1_2NativeInterface.setTpmOwnerPriviledges(contextId, OWNER_PASSWORD);

        byte[] ekPub = Tss1_2NativeInterface.getPEMEKPublicKey(contextId);
        // Simulate the creation of EK credentials (this operation should be done by the Manifacturer, and we 
        // should have manifacurer's certiicate into our trust store. For now, assume the CA we have is 
        // issuing our EK credentials.
        byte[] ekCredentials = Tss1_2NativeInterface.createCredentials(contextId, ekPub, CAPrivateKeyBytes, CACertificateBytes);
        // write the credentials to file, chained with the trusted cert
        FileOutputStream fos = new FileOutputStream(BASE_DIR + "ek_cred.chain");
        fos.write(ekCredentials);
        fos.write(CACertificateBytes);
        fos.close();

        Tss1_2NativeInterface.deleteTpmContext(contextId);

        return (TpmAttestationIdentityRequest) manager.createIdentityRequest(AIK_SECRET, caCertificate);
    }

    @Test
    public void createIdentityRequestTest() throws Exception {

//        DeapKeyManager keyManager = KeyStoreBasedManagerProvider__OLD.getKeyManager(SecurityConstants.KEY_STORE_BASED_MANAGER, false);

        // TEST:
        TpmAttestationIdentityRequest m = createIdentityRequest();

        // (1) Message should be not null
        Assert.assertNotNull(m);
        
        PrivateKey caPrivateKey = PEMEncodingUtils.decodeRSAPrivateKey(CA_PRIVATE_KEY.getBytes());
        
        // (2) Now verify each field is correctly encrypted
        byte[] sk = CryptoProvider.getCipher(SecurityConstants.RSA_ALGORITHM, false).decrypt(m.ecryptedSymmetricKey, caPrivateKey);

        CryptoProvider.getCipher(SecurityConstants.AES_CBC_ALGORITHM, false).decrypt(m.encryptedEkCertificate, KeyProvider.generateSymmetricKey(SecurityConstants.AES_CBC_ALGORITHM, sk));

    }

    @Test
    public void issueIdentityCredentialsTest() throws Exception {

        TpmAttestationIdentityRequest m = createIdentityRequest();

        // (1) Should work fine
        TpmAttestationIdentityCredetialsResponse resp = (TpmAttestationIdentityCredetialsResponse) manager.issueIdentityCredentials(m);

        Assert.assertNotNull(resp);
        Assert.assertNotNull(resp.aikIdentityResponse);

        TpmAttestationIdentityRequest m2 = new TpmAttestationIdentityRequest();
        m2.ecryptedSymmetricKey = null;
        m2.encryptedEkCertificate = m.encryptedEkCertificate;
        m2.identityRequest = m.identityRequest;

        try {
            // (2) Should fail since we put a null item
            manager.issueIdentityCredentials(m2);
        } catch (TpmOperationError ex) {

        }

        TpmAttestationIdentityRequest m3 = new TpmAttestationIdentityRequest();
        m3.ecryptedSymmetricKey = m.ecryptedSymmetricKey;
        m3.encryptedEkCertificate = null;
        m3.identityRequest = m.identityRequest;

        try {
            // (3) Should fail since we put a null item
            manager.issueIdentityCredentials(m3);
        } catch (TpmOperationError ex) {

        }

        TpmAttestationIdentityRequest m4 = new TpmAttestationIdentityRequest();
        m4.ecryptedSymmetricKey = m.ecryptedSymmetricKey;
        m4.encryptedEkCertificate = m.encryptedEkCertificate;
        m4.identityRequest = null;

        try {
            // (4) Should fail since we put a null item
            manager.issueIdentityCredentials(m4);
        } catch (TpmOperationError ex) {

        }

        m4.identityRequest = manager.getRandomBytes(20);

        try {
            // (5) Should fail since we put random bytes
            manager.issueIdentityCredentials(m4);
        } catch (TpmOperationError ex) {

        }

    }

}
