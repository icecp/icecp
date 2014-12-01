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

 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.icecp.node.security.tpm.tpm12;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.channels.ChannelProvider;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.metadata.formats.FormatEncodingException;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.node.pipeline.PipelineBuilder;
import com.intel.icecp.core.pipeline.exception.PipelineException;
import com.intel.icecp.core.security.CryptoProvider;
import com.intel.icecp.core.security.crypto.cipher.Cipher;
import com.intel.icecp.core.security.crypto.exception.cipher.CipherDecryptionError;
import com.intel.icecp.core.security.crypto.exception.cipher.CipherEncryptionError;
import com.intel.icecp.core.security.crypto.exception.cipher.UnsupportedCipherException;
import com.intel.icecp.core.security.crypto.exception.hash.HashError;
import com.intel.icecp.core.security.crypto.exception.key.InvalidKeyTypeException;
import com.intel.icecp.core.security.keymanagement.IcecpKeyManager;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerException;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerNotSupportedException;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.node.security.crypto.utils.CryptoUtils;
import com.intel.icecp.node.security.tpm.data.SealedData;
import com.intel.icecp.node.security.tpm.exception.TpmOperationError;
import com.intel.icecp.node.channels.file.FileChannelProvider;
import com.intel.icecp.node.messages.security.CertificateMessage;
import com.intel.icecp.node.pipeline.operations.FormattingOperation;
import com.intel.icecp.node.security.RandomBytesGenerator;
import com.intel.icecp.node.security.crypto.key.KeyProvider;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.node.security.utils.PEMEncodingUtils;
import com.intel.icecp.node.utils.StreamUtils;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.CertificateEncodingException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test for TPM 1.2 interface class
 * {@link com.intel.icecp.node.security.tpm.tpm12.Tss1_2NativeInterface}.
 *
 */
@Ignore
public class Tss1_2NativeInterfaceTest {

    // Context
    protected int contextId;

    private static final byte[] OWNER_PASSWORD = "1234".getBytes();
    private static final byte[] AIK_SECRET = RandomBytesGenerator.getRandomBytes(20);
    private static final int[] pcrs = {0, 17, 18, 19, 20, 21, 22, 23};

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

    /**
     * Same operations as in Tpm1_2Manager
     *
     * @param contextId
     * @param keyPassword
     * @param pcrRegisters
     * @param dataToSeal
     * @param symmetricEncryptionAlgorithm
     * @param symmetricKeySize
     * @return
     * @throws TpmOperationError
     */
    private SealedData dataSeal(
            int contextId,
            byte[] keyPassword,
            int[] pcrRegisters,
            byte[] dataToSeal,
            String symmetricEncryptionAlgorithm,
            int symmetricKeySize) throws TpmOperationError {

        // Create a Key, ecnrypt, and then seal the used symmetric key
        SymmetricKey sk;
        try {
            sk = KeyProvider.generateSymmetricKey(
                    symmetricEncryptionAlgorithm,
                    symmetricKeySize);
        } catch (InvalidKeyTypeException ex) {
            throw new TpmOperationError("Unsupported key type " + symmetricEncryptionAlgorithm);
        }

        // Encryt the data
        byte[] encryptedData;
        try {
            encryptedData = CryptoProvider.getCipher(SecurityConstants.AES_CBC_ALGORITHM, false).encrypt(dataToSeal, sk);
        } catch (CipherEncryptionError | UnsupportedCipherException ex) {
            throw new TpmOperationError("Unable to encrypt the given bytes. Cause: " + ex.getMessage());
        }

        // Now seal the symmetric key (May throw a TpmException)
        byte[] sealedDataStruct = Tss1_2NativeInterface.sealData(
                contextId,
                keyPassword,
                pcrRegisters, sk.getEncoded());

        // Parse the response bytes
        DataInputStream dis = new DataInputStream(
                new ByteArrayInputStream(sealedDataStruct));

        try {
            // Read the Sealing key Specs
            int sealingKeyLen = dis.readInt();
            byte[] sealingKeySpecsBytes = new byte[sealingKeyLen];
            for (int i = 0; i < sealingKeyLen; i++) {
                sealingKeySpecsBytes[i] = dis.readByte();
            }

            // Read the encrypted data
            int sealedData = dis.readInt();
            byte[] sealedDataBytes = new byte[sealedData];
            for (int i = 0; i < sealedData; i++) {
                sealedDataBytes[i] = dis.readByte();
            }

            // Return the SealedData
            return new SealedData(
                    sealedDataBytes,
                    sealingKeySpecsBytes,
                    encryptedData,
                    symmetricEncryptionAlgorithm);

        } catch (IOException ex) {
            throw new TpmOperationError("Unable to parse the sealed data response. Cause: " + ex.getMessage());
        }

    }

    /**
     * Perform unsealing of the given data
     *
     * @param contextId
     * @param keyPassword
     * @param sealedData
     * @param symmetricEncryptionAlgorithm
     * @return
     * @throws TpmOperationError
     */
    private byte[] dataUnseal(
            int contextId,
            byte[] keyPassword,
            SealedData sealedData,
            String symmetricEncryptionAlgorithm) throws TpmOperationError, Exception {

        // Unsealthe symmetric key (may throw TpmException)
        byte[] symmKeyBytes = Tss1_2NativeInterface.unsealData(
                contextId,
                keyPassword,
                sealedData.sealedSymmKey,
                sealedData.sealingKeySpecs);

        // get the symmetric key from the bytes
        SymmetricKey sk;

        sk = KeyProvider.generateSymmetricKey(symmetricEncryptionAlgorithm, symmKeyBytes);
        try {

            // Return the data unencrypted
            Cipher c = CryptoProvider.getCipher(
                    symmetricEncryptionAlgorithm,
                    false);

            return c.decrypt(sealedData.encryptedData, sk);

        } catch (CipherDecryptionError | UnsupportedCipherException ex) {
            throw new TpmOperationError("Unable to decrypt the given data. Cause: " + ex.getMessage());
        }

    }

    @Before
    public void init() throws TpmOperationError {

        // Load the Native library
        Tss1_2NativeInterface.load();

        // Create an empty context
        contextId = Tss1_2NativeInterface.createTpmContext();

        Assert.assertTrue(contextId != -1);
    }

    @After
    public void deleteContext() throws TpmOperationError {
        // Free context memory
        Tss1_2NativeInterface.deleteTpmContext(contextId);
    }

    
    
    
    /**
     * 
     * **************************** TESTS **********************************
     * 
     */
    
    
    
    
    /**
     * TEST
     *
     * Tries to create multiple contexts concurrently
     *
     * @throws TpmOperationError
     */
    @Test
    public void multipleContexts() throws TpmOperationError {
        int[] context = new int[10];

        for (int i = 0; i < 10; i++) {
            final int j = i;
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        int cont = Tss1_2NativeInterface.createTpmContext();
                        Thread.sleep(2000);
                        Tss1_2NativeInterface.deleteTpmContext(cont);

                    } catch (TpmOperationError | InterruptedException ex) {
                        Logger.getLogger(Tss1_2NativeInterfaceTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }).start();
        }
    }

    /**
     * TEST: NOT WORKING ON EMULATOR!
     *
     *
     * @throws TpmOperationError
     * @throws KeyManagerNotSupportedException
     * @throws KeyManagerException
     */
    @Test
    public void CreateOwnerDelegationTest() throws TpmOperationError, KeyManagerNotSupportedException, KeyManagerException, CertificateEncodingException {

        Tss1_2NativeInterface.setTpmOwnerPriviledges(contextId, OWNER_PASSWORD);

        Tss1_2NativeInterface.flushTpmOwnerPriviledges(contextId);

        // TEST: try to perform a priviliged operation
        // Load SRK
        Tss1_2NativeInterface.loadSrk(contextId, null);

        byte[] caCert = CA_CERT.getBytes(); 
        
        byte[] req = Tss1_2NativeInterface.generateIdentityRequest(
                contextId,
                AIK_SECRET,
                caCert, "aikLabel".getBytes());

        Assert.assertNotNull(req);
        Assert.assertTrue(req.length > 0);
    }

    /**
     * Compute the Hash (SHA-1) using the TPM
     *
     * @throws TpmOperationError
     * @throws HashError
     */
    @Test
    public void CalculateHashTest() throws TpmOperationError, HashError {
        byte[] toHash = "Test String to Hash".getBytes();

        byte[] hashValue = Tss1_2NativeInterface.calculateHash(contextId, toHash);

        Assert.assertNotNull(hashValue);
        Assert.assertEquals(20, hashValue.length);

        Assert.assertArrayEquals(hashValue, CryptoUtils.hash(toHash, SecurityConstants.SHA1));

    }

    /**
     * Tests key creation, registration and unregistration
     *
     * @throws TpmOperationError
     * @throws java.io.IOException
     */
    @Test
    public void CreateKeyTest() throws TpmOperationError, IOException {

        // Load SRK
        Tss1_2NativeInterface.loadSrk(contextId, null);

        // TEST 1: Create register/uregister key
        byte[] uuid = Tss1_2NativeInterface.createKey(contextId,
                Tss1_2Codes.TSS_KEY_TYPE_SIGNING,
                Tss1_2Codes.TSS_KEY_SIZE_2048,
                Tss1_2Codes.TSS_KEY_NOT_MIGRATABLE,
                Tss1_2Codes.TSS_KEY_AUTHORIZATION,
                Tss1_2Codes.TSS_KEY_NON_VOLATILE, // This option makes sure we are going to register the key
                Tss1_2Codes.TSS_PS_TYPE_USER,
                true,
                AIK_SECRET);

        // Key's UUID should be not null
        Assert.assertNotNull(uuid);

        try {
            // This should NOT fail
            Tss1_2NativeInterface.getPEMPublicKeyByUUID(contextId, uuid);
            Assert.assertTrue(true);
        } catch (TpmOperationError e) {
            Assert.assertTrue(false);
        } finally {
            Tss1_2NativeInterface.unregisterKey(contextId, uuid, Tss1_2Codes.TSS_PS_TYPE_USER);
        }

        // TEST 2: Create a key and get the key blob (not registered)
        byte[] keyBlob = Tss1_2NativeInterface.createKey(contextId,
                Tss1_2Codes.TSS_KEY_TYPE_SIGNING,
                Tss1_2Codes.TSS_KEY_SIZE_2048,
                Tss1_2Codes.TSS_KEY_NOT_MIGRATABLE,
                Tss1_2Codes.TSS_KEY_AUTHORIZATION,
                Tss1_2Codes.TSS_KEY_VOLATILE, // This tells that we want the key blob out and not the key must not be registered
                Tss1_2Codes.TSS_PS_TYPE_USER,
                true,
                "123".getBytes());

        Assert.assertNotNull(keyBlob);

        // To make sure it worked, we try to get the public key out
        Assert.assertNotNull(Tss1_2NativeInterface.extractPEMPublicKeyFromTPMKeyBlob(contextId, keyBlob));
    }

    /**
     * Identity request creation
     *
     * @throws Exception
     */
    @Test
    public void GenerateIdentityRequestTest() throws Exception {

        // Set TPM owner secret
        Tss1_2NativeInterface.setTpmOwnerPriviledges(contextId, OWNER_PASSWORD);

        // Load SRK
        Tss1_2NativeInterface.loadSrk(contextId, null);

        byte[] caCert = CA_CERT.getBytes();

        byte[] req = Tss1_2NativeInterface.generateIdentityRequest(
                contextId,
                AIK_SECRET,
                caCert, "aikLabel".getBytes());

        Assert.assertNotNull(req);
        Assert.assertTrue(req.length > 0);

        // Output the request in bytes
        try (OutputStream os = new FileOutputStream(new File("certificates/TPM/aikRequest.bytes"))) {
            os.write(req);
        }

        try {
            Tss1_2NativeInterface.generateIdentityRequest(
                    contextId,
                    AIK_SECRET,
                    null, "aikLabel".getBytes());
            Assert.assertTrue(false);
        } catch (TpmOperationError ex) {
            Assert.assertTrue(true);
        }

        Tss1_2NativeInterface.flushTpmOwnerPriviledges(contextId);
    }

    /**
     * Utility method that either loads or creates a new Identity request. Note
     * that this works iif Tss1_2NativeInterface.generateIdentityRequest works.
     *
     * @param pubKey
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws TpmOperationError
     */
    private byte[] getIdReq(byte[] pubKey) throws IOException, TpmOperationError {
        // Load the request if exists, otherwise we generate one on the fly
        byte[] aikRequest;
        File request = new File("certificates/TPM/aikRequest.bytes");
        if (request.exists()) {
            InputStream is = new FileInputStream(request);
            aikRequest = StreamUtils.readAll(is);
        } else {
            aikRequest = Tss1_2NativeInterface.generateIdentityRequest(
                    contextId,
                    AIK_SECRET,
                    pubKey, "aikLabel".getBytes());
            // Output the request in bytes
            OutputStream os = new FileOutputStream(new File("certificates/TPM/aikRequest.bytes"));
            os.write(aikRequest);
            os.close();
        }

        return aikRequest;
    }

    /**
     * Test for getRandomBytes; we retrieve 256 bits and use them to create a
     * symmetric key
     *
     *
     * @throws InvalidKeyTypeException
     * @throws UnsupportedCipherException
     * @throws CipherEncryptionError
     * @throws CipherDecryptionError
     */
    @Test
    public void GetRandomBytesTest() throws InvalidKeyTypeException, UnsupportedCipherException, CipherEncryptionError, CipherDecryptionError {

        byte[] rnd = Tss1_2NativeInterface.getRandomBytes(contextId, 256 / 8);

        // Create a key with these bytes
        SymmetricKey sk = KeyProvider.generateSymmetricKey(SecurityConstants.AES_CBC_ALGORITHM, rnd);

        // And to encrypt/decrypt a string
        byte[] data = RandomBytesGenerator.getRandomBytes(500);

        Cipher cp = CryptoProvider.getCipher(SecurityConstants.AES_CBC_ALGORITHM, true);

        Assert.assertArrayEquals(cp.decrypt(cp.encrypt(data, sk), sk), data);

    }

    /**
     * TEST
     *
     * Test for data sealing
     *
     * @throws TpmOperationError
     * @throws FileNotFoundException
     * @throws IOException
     */
    @Test
    public void SealingTest() throws TpmOperationError, IOException {

        // Load SRK
        Tss1_2NativeInterface.loadSrk(contextId, null);

        // Seal data with the SRK
        SealedData sealedData = dataSeal(
                contextId,
                null, pcrs,
                "Data_to_seal".getBytes(),
                SecurityConstants.AES_CBC_ALGORITHM,
                256);

        Assert.assertNotNull(sealedData);

    }

    /**
     * TEST
     *
     * Test for Data unsealing
     *
     * @throws InterruptedException
     * @throws IOException
     * @throws TpmOperationError
     */
    @Test
    public void UnsealingTest() throws Exception {

        // Load SRK
        Tss1_2NativeInterface.loadSrk(contextId, null);

        // And to encrypt/decrypt a string
        byte[] dataToSeal = RandomBytesGenerator.getRandomBytes(500);

        // Test 1: Seal some data and try to unseal it
        // Seal data 
        SealedData sealedData = dataSeal(
                contextId,
                null,
                pcrs,
                dataToSeal,
                SecurityConstants.AES_CBC_ALGORITHM,
                256);

        // And then unseal them
        byte[] res = dataUnseal(
                contextId,
                null,
                sealedData,
                SecurityConstants.AES_CBC_ALGORITHM);

        // The result should be equals to the original message
        Assert.assertNotNull(res);
        Assert.assertArrayEquals(dataToSeal, res);

        // Test 2: Load the output from tpm_sealdata command of tpm_tools and try to unseal 
        InputStream is = new FileInputStream(new File("keystores/trusted.jceks.sealed"));
        String sealedDataString = new String(StreamUtils.readAll(is));

        SealedData sData = SealedData.decode(sealedDataString.getBytes());

        // We should be able to unseal
        byte[] res2 = dataUnseal(
                contextId,
                SecurityConstants.getTpmToolsSealSecret(),
                sData,
                SecurityConstants.AES_CBC_ALGORITHM);

        // The result should be equals to the original message
        Assert.assertNotNull(res2);

    }

    /**
     * TEST
     *
     * Test for pcrExtend
     *
     *
     * @throws TpmOperationError
     * @throws com.intel.icecp.core.security.crypto.exception.hash.HashError
     */
    @Test
    public void PcrExtendTest() throws Exception {

        // Load SRK
        Tss1_2NativeInterface.loadSrk(contextId, null);

        byte[] value = CryptoUtils.hash(RandomBytesGenerator.getRandomBytes(200), SecurityConstants.SHA1);

        // Test 1: simple extension of PCR 12
        byte[] res = Tss1_2NativeInterface.pcrExtend(contextId, 12, value);

        Assert.assertNotNull(res);

        // Test 2: Seal data PCR 12, then extend and try to unseal
        int[] pcrsToSealTo = {12};
        SealedData sealedData = dataSeal(
                contextId,
                null,
                pcrsToSealTo,
                "Data_to_seal".getBytes(),
                SecurityConstants.AES_CBC_ALGORITHM,
                256);

        Tss1_2NativeInterface.pcrExtend(contextId, 12, value);

        try {
            dataUnseal(
                    contextId,
                    null,
                    sealedData,
                    SecurityConstants.AES_CBC_ALGORITHM);
            Assert.assertTrue(false);

        } catch (TpmOperationError ex) {
            Assert.assertTrue(true);
        }

    }

    /**
     * TEST
     *
     * Test method for GetPEMPublicKey; here we test only retrieval by key ID,
     * since public key retrieval by TPM_PUBKEY blob is tested in attestation
     * process test
     *
     * @throws TpmOperationError
     * @throws FileNotFoundException
     * @throws IOException
     */
    @Test
    public void GetPublicKeyTest() throws TpmOperationError, IOException {

        // Load SRK
        Tss1_2NativeInterface.loadSrk(contextId, null);

        // Set TPM owner secret
        Tss1_2NativeInterface.setTpmOwnerPriviledges(contextId, OWNER_PASSWORD);

        byte[] res = Tss1_2NativeInterface.getPEMEKPublicKey(contextId);

        // TEST: returned bytes should be not null
        Assert.assertNotNull(res);

        String pk = new String(res);

        // TEST: Try to build an RSA key from the given bytes
        Assert.assertNotNull(PEMEncodingUtils.decodeRSAPublicKey(res));

        // Flush owner privileges
        Tss1_2NativeInterface.flushTpmOwnerPriviledges(contextId);

        FileOutputStream os = new FileOutputStream(new File("certificates/ek.pub"));
        os.write(res);
        os.close();
    }

    /**
     * TEST
     *
     * Test for createCredentials method
     *
     * @throws IOException
     * @throws KeyManagerException
     * @throws KeyManagerNotSupportedException
     * @throws FormatEncodingException
     * @throws ChannelLifetimeException
     * @throws PipelineException
     * @throws TpmOperationError
     */
    @Test
    public void createCredetialsTest() throws IOException, KeyManagerException, KeyManagerNotSupportedException, FormatEncodingException, ChannelLifetimeException, PipelineException, TpmOperationError {

        byte[] pubKey = Tss1_2NativeInterface.getPEMEKPublicKey(contextId);//StreamUtils.readAll(new FileInputStream(new File("certificates/ek.pub")));
        byte[] prtivKey = CA_PRIVATE_KEY.getBytes();
        byte[] issuerCert = StreamUtils.readAll(new FileInputStream(new File("certificates/CAX509/ca/new_ca.chain")));
        byte[] credentials = Tss1_2NativeInterface.createCredentials(contextId, pubKey, prtivKey, issuerCert);

        System.out.println(new String(prtivKey));

        // TEST 1: Make sure the credentials are not null
        Assert.assertNotNull(credentials);

        // write the credentials to file, chained with the trusted cert
        FileOutputStream fos = new FileOutputStream("created_credential.chain");
        fos.write(credentials);
        fos.write(issuerCert);
        fos.close();

        // TEST 2: Make sure we can use those credentials
        createJsonCertificateMessage("created_credential.chain");

        // Make sure we can validate this certificate against our trust anchors
        PipelineBuilder pipeline = new PipelineBuilder<>(CertificateMessage.class, InputStream.class);
        pipeline.addAll(new FormattingOperation(new JsonFormat<>(CertificateMessage.class)));
        Channel channel = new FileChannelProvider().build(Paths.get("").resolve("created_credential.chain.json").toUri(), pipeline.build(), new Persistence());
//        Assert.assertNotNull(manager.getCertificate("created_credential.chain.json", channel));

        try {
            Files.delete(Paths.get("created_credential.chain"));
            Files.delete(Paths.get("created_credential.chain.json"));
        } catch (IOException ex) {

        }

    }

    /**
     * Utility function, creates a JSON certificate message and outputs to file
     *
     * @param cert
     * @throws FileNotFoundException
     * @throws FormatEncodingException
     * @throws IOException
     */
    private void createJsonCertificateMessage(String cert) throws FormatEncodingException, IOException {

        FileInputStream fis = new FileInputStream(cert);

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            br = new BufferedReader(new InputStreamReader(fis));
            line = br.readLine();
            sb.append(line);
            while ((line = br.readLine()) != null) {
                sb.append("\n").append(line);
            }
        } catch (IOException e) {

        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {

                }
            }
        }

        // We create a CertificateMessage
        String crt = sb.toString();
        JsonFormat jf = new JsonFormat<>(CertificateMessage.class);
        CertificateMessage cm = new CertificateMessage();
        cm.certificate = crt;

        // Write it to file
        FileOutputStream fos = new FileOutputStream(cert + ".json");
        fos.write(StreamUtils.readAll(jf.encode(cm)));
        fos.close();
    }

    
    
    
    
    /**
     * **************************** ATTESTATION PROCESS TEST  ****************************
     */

    
    

    /**
     * Tests the whole Attestation process based on Privacy CA
     *
     *
     * @throws TpmOperationError
     * @throws FileNotFoundException
     * @throws IOException
     * @throws KeyManagerNotSupportedException
     * @throws KeyManagerException
     * @throws ChannelLifetimeException
     * @throws PipelineException
     * @throws FormatEncodingException
     */
    @Test
    public void AttestationProcessTest() throws TpmOperationError, IOException, KeyManagerNotSupportedException, KeyManagerException, ChannelLifetimeException, PipelineException, FormatEncodingException, CertificateEncodingException {

        // Get a default key manager instance
//        IcecpKeyManager manager = KeyStoreBasedManagerProvider__OLD.getKeyManager(SecurityConstants.KEY_STORE_BASED_MANAGER, false);
        // Load SRK
        Tss1_2NativeInterface.loadSrk(contextId, null);

        // Set TPM owner secret
        Tss1_2NativeInterface.setTpmOwnerPriviledges(contextId, OWNER_PASSWORD);

        // Read the X.509 certificate of the Privacy CA (no need to supply channel)
        byte[] CACertificate = CA_CERT.getBytes();
        byte[] CAPrivateKey = CA_PRIVATE_KEY.getBytes();
        // EK public key
        byte[] ekPub = StreamUtils.readAll(new FileInputStream(new File("certificates/ek.pub")));

        // ***********************************************************************
        // ***************************** OFFLINE PHASE ***************************
        // ***********************************************************************
        // Simulate the creation of EK credentials (this operation should be done by the Manifacturer, and we 
        // should have manifacurer's certiicate into our trust store. For now, assume the CA we have is 
        // issuing our EK credentials.
        byte[] ekCredentials = Tss1_2NativeInterface.createCredentials(contextId, ekPub, CAPrivateKey, CACertificate);
        // write the credentials to file, chained with the trusted cert
        FileOutputStream fos = new FileOutputStream("ek_cred.chain");
        fos.write(ekCredentials);
        fos.write(CACertificate);
        fos.close();

        // **************************************************************************************
        // ************************ CLIENT: Generate an Identity Request ************************
        // **************************************************************************************
        // (a) Create an Identity Request
        byte[] aikRequestRes = Tss1_2NativeInterface.generateIdentityRequest(
                contextId,
                AIK_SECRET,
                CACertificate, "aikLabel".getBytes());

        // (b) Extract the bytes of the AIK + the AIK credentials request
        DataInputStream aikIs = new DataInputStream(new ByteArrayInputStream(aikRequestRes));

        // Read the Key bytes
        int aikLen = aikIs.readInt();
        byte[] aikBytes = new byte[aikLen];
        aikIs.read(aikBytes, 0, aikLen);


        // Read the Request bytes
        int aikRequestLen = aikIs.readInt();
        byte[] aikRequest = new byte[aikRequestLen];
        aikIs.readFully(aikRequest);

        // (c) @TODO: Send to the PrivacyCA something like:
        //					EK_cred, E_{CA_pub} (k2), E_{k2} (AIK_request), ...
        // **************************************************************************************
        // ************************ PRIVACY CA: Verify identity binding *************************
        // **************************************************************************************
        // (a) @TODO: Unpack EK_cred and AIK_request
        // (b) Verifies EK_cred (in this case, check whether EK certificate matches against out trust anchors)
        // Write those credentials to file, to "simulate" we retreived a CertificateMessage
        createJsonCertificateMessage("ek_cred.chain");

        PipelineBuilder pipeline = new PipelineBuilder<>(CertificateMessage.class, InputStream.class);
        pipeline.addAll(new FormattingOperation(new JsonFormat<>(CertificateMessage.class)));
        
        // @TODO: Add test to verify if "ek_cred.chain.json" can be verified against a 
        // trust store (use IcecpKeyManager#getCertificate)

        // (c) EK credentials are OK, we can proceed by verifying the identity binding on the AIK request		
        byte[] aikres = Tss1_2NativeInterface.verifyIdentityRequestBinding(contextId, aikRequest, CAPrivateKey, CACertificate);
        Assert.assertNotNull(aikres);

        // (d) Create the AIK credentials!
        // Extract the public key from the PUBKEY structure returned by verifyIdentityRequestBinding 
        byte[] aikPublicKey = Tss1_2NativeInterface.extractPEMPublicKeyFromTPMPubKeyBlob(contextId, aikres);

        // And create AIK credentials
        byte[] aikCredentials = Tss1_2NativeInterface.createCredentials(contextId, aikPublicKey, CAPrivateKey, CACertificate);

        // Test the credentials (i.e., if they are verified against the trusted anchors in our key storage)		
        fos = new FileOutputStream("aik_cred.chain");
        fos.write(aikCredentials);
        fos.write(CACertificate);
        fos.close();
        createJsonCertificateMessage("aik_cred.chain");

        PipelineBuilder pp = new PipelineBuilder<>(CertificateMessage.class, InputStream.class);
        pipeline.addAll(new FormattingOperation(new JsonFormat<>(CertificateMessage.class)));

        
        // @TODO: Add test to verify if "aik_cred.chain.json" can be verified against a 
        // trust store (use IcecpKeyManager#getCertificate)

        // (e) ...and issue the response for the client
        byte[] attestData = Tss1_2NativeInterface.createAttestationResponse(
                contextId,
                ekPub, // This should be EK pub key.
                aikres, // Result of verifyIdentityRequestBinding
                aikCredentials);

        Assert.assertNotNull(attestData);

        // ****************************************************************************
        // ************************ CLIENT: activate Identity *************************
        // ****************************************************************************
        
        // (a) Activate the identity 
        byte[] aikCred = Tss1_2NativeInterface.activateIdentity(contextId, attestData, aikBytes);

        // Test that what we obtain is what we passed to createAttestationResponse as last parameter
        Assert.assertArrayEquals(aikCred, aikCredentials);

        // (b) @TODO: quote the PCRs
        byte[] nonce = Tss1_2NativeInterface.getRandomBytes(contextId, 20);
        byte[] quoteRes = Tss1_2NativeInterface.quote(contextId, aikBytes, nonce, pcrs);

        Assert.assertNotNull(quoteRes);

        // We can extract the quote request as well as the info
        DataInputStream quoteIs = new DataInputStream(new ByteArrayInputStream(quoteRes));

        // Read the bytes of the quote
        int quoteLen = quoteIs.readInt();
        byte[] quoteValueBytes = new byte[quoteLen];
        quoteIs.read(quoteValueBytes, 0, quoteLen);

        // Read the quote info byte
        int quoteInfoLen = quoteIs.readInt();
        byte[] quoteInfoBytes = new byte[quoteInfoLen];
        quoteIs.read(quoteInfoBytes, 0, quoteInfoLen);

        // This file is written by the previous call
        FileInputStream fis = new FileInputStream("expectedValuePcr.dat");		
        byte[] expectedPcrValue = Base64.getDecoder().decode(new String(StreamUtils.readAll(fis)));

        // Verify the quote 
        Tss1_2NativeInterface.verifyQuote(
                contextId,
                quoteValueBytes,
                quoteInfoBytes,
                aikCred,
                expectedPcrValue,
                nonce);

        // Cleanup: delete the files crated for the test
        Tss1_2NativeInterface.flushTpmOwnerPriviledges(contextId);

        try {
            Files.delete(Paths.get("aik_cred.chain"));
            Files.delete(Paths.get("aik_cred.chain.json"));
            Files.delete(Paths.get("ek_cred.chain"));
            Files.delete(Paths.get("ek_cred.chain.json"));
            Files.delete(Paths.get("expectedValuePcr.dat"));
        } catch (IOException ex) {
            // Ignore any error....
        }

        Assert.assertTrue(true);

    }

    @Test
    public void PrivilegeExcalationResilienceTest() throws TpmOperationError {

        // Load SRK
        Tss1_2NativeInterface.loadSrk(contextId, null);

        // Set TPM owner secret
        Tss1_2NativeInterface.setTpmOwnerPriviledges(contextId, OWNER_PASSWORD);

        // This will work
        Tss1_2NativeInterface.getPEMEKPublicKey(contextId);

        int contextId2 = 0;
        try {
            // Create another context. Here, we should not be able to get out the EK pub key
            contextId2 = Tss1_2NativeInterface.createTpmContext();

            Tss1_2NativeInterface.getPEMEKPublicKey(contextId);

            Assert.assertTrue(false);

        } catch (TpmOperationError ex) {
            // Do nothing
        } finally {
            Tss1_2NativeInterface.deleteTpmContext(contextId2);
        }

        // Flush owner privileges
        Tss1_2NativeInterface.flushTpmOwnerPriviledges(contextId);

    }

}
