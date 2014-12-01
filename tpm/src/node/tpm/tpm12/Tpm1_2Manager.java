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

import com.intel.icecp.core.Message;
import com.intel.icecp.core.permissions.Tpm1_2Permissions;
import com.intel.icecp.core.security.CryptoProvider;
import com.intel.icecp.core.security.crypto.cipher.Cipher;
import com.intel.icecp.core.security.crypto.exception.cipher.CipherDecryptionError;
import com.intel.icecp.core.security.crypto.exception.cipher.CipherEncryptionError;
import com.intel.icecp.core.security.crypto.exception.cipher.UnsupportedCipherException;
import com.intel.icecp.core.security.crypto.exception.key.InvalidKeyTypeException;
import com.intel.icecp.core.security.keymanagement.IcecpKeyManager;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerException;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerNotSupportedException;
import com.intel.icecp.node.security.tpm.TpmManager;
import com.intel.icecp.node.security.tpm.data.SealedData;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.node.security.tpm.exception.TpmOperationError;
import com.intel.icecp.node.messages.security.tpm.tpm1_2.TpmAttestationIdentityCredetialsResponse;
import com.intel.icecp.node.messages.security.tpm.tpm1_2.TpmAttestationIdentityRequest;
import com.intel.icecp.node.security.crypto.key.KeyProvider;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.node.security.utils.PEMEncodingUtils;
import com.intel.icecp.node.utils.SecurityUtils;
import com.intel.icecp.node.utils.StreamUtils;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manager class for TPM 1.2. It exposes high-level functionalities and handles
 * TPM context and concurrent access. A context maintains (on the native code
 * side) all the handles to the resources required to use the TPM. Currently,
 * the context is local to each method call: each method call creates/closes a
 * context before/after performing each macro operation.
 *
 *
 */
public class Tpm1_2Manager implements TpmManager {

    private static final Logger logger = LogManager.getLogger();

    private static final String SYMMETRIC_ALGO_SEALING = "symmetric_algorithm";
    private static final String SYMMETRIC_KEY_SIZE_SEALING = "key_size";
    private static final String PCRS_SEALING = "pcrs";
    private static final String PCRS_SEALING_DELIMITER = ",";

    protected final String defaultSymmetricAlgorithm;
    protected final int defaultSymmetricKeySize;
    protected int[] pcrsSealing;
    
    private final IcecpKeyManager keyManager;
    
    public Tpm1_2Manager(IcecpKeyManager keyManager, Properties properties) {
        this.keyManager = keyManager;
        defaultSymmetricAlgorithm = properties.getProperty(SYMMETRIC_ALGO_SEALING);
        defaultSymmetricKeySize = Integer.parseInt(properties.getProperty(SYMMETRIC_KEY_SIZE_SEALING));

        String[] pcrs = properties.getProperty(PCRS_SEALING).split(PCRS_SEALING_DELIMITER);

        if (pcrs != null && pcrs.length > 0) {
            pcrsSealing = new int[pcrs.length];
            for (int j = 0; j < pcrs.length; j++) {
                try {
                    pcrsSealing[j] = Integer.parseInt(pcrs[j]);
                } catch (NumberFormatException ex) {
                    pcrsSealing = null;
                    break;
                }
            }
        } else {
            // PCRs may be null
            pcrsSealing = null;
        }

        // Finally, we load the keys registered to the TPM
        loadRegisteredKeys();
    }
    
    
    public static TpmManager build(IcecpKeyManager keyManager) throws TpmOperationError {
        // We read the property file to obtain:
        //	- default symm encryption algorithm for sealing
        //	- default key size
        //	- sealing PCRs to consider
        // If unable to read these parameters, throws and exception

        // Try to load the Native library (this may throw a TpmOperationError)
        Tss1_2NativeInterface.load();
        
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(new File(SecurityConstants.TPM_1_2_SERVICE_PROPERTIES)));
        } catch (IOException ex) {
            throw new TpmOperationError("Unable to read properties file " + SecurityConstants.TPM_1_2_SERVICE_PROPERTIES + ". Cause: " + ex.getMessage());
        }
        
        // Everything is fine, we can create an instance
        return new Tpm1_2Manager(keyManager, properties);
    }
    
    
    

    /**
     * Loads all the registered keys
     *
     */
    private void loadRegisteredKeys() {
        try {
            int context = initContext();
            Tss1_2NativeInterface.loadSrk(context, null);
            Tss1_2NativeInterface.loadRegisteredKeys(context);
            closeContext(context);
        } catch (TpmOperationError ex) {
            // Do nothing
        }
    }

    /**
     * Initializes a context and returns the corresponding ID. Only one thread
     * at a time can create a context. IMPORTANT: Note that the maximum number
     * of contexts that can be created is 256. There is no control on this for
     * now.
     *
     * @throws TpmOperationError
     */
    synchronized private int initContext() throws TpmOperationError {
        // Initialize a new context
        int context = Tss1_2NativeInterface.createTpmContext();
        logger.debug("Closing context " + context);
        return context;
    }

    /**
     * Closes a context. This method must be called before the conclusion of
     * every TPM operation. Only one thread at a time can close a context.
     *
     * @param contextId
     * @return
     * @throws TpmOperationError
     */
    synchronized private boolean closeContext(int contextId) throws TpmOperationError {
        if (contextId != 0) {

            logger.debug("Closing context " + contextId);
            Tss1_2NativeInterface.deleteTpmContext(contextId);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @throws com.intel.icecp.node.security.tpm.exception.TpmOperationError
     */
    @Override
    public byte[] getRandomBytes(int bytesNum) throws TpmOperationError {

        // No permissions check; everybody can get random bytes from the TPM
        // Initialize if necessary
        int context = initContext();

        byte[] rnd = Tss1_2NativeInterface.getRandomBytes(context, bytesNum);

        // No need the context anymore
        closeContext(context);

        return rnd;
    }

    /**
     * Seals using the same encoding as tpm_sealdata from tpm_tools
     *
     * {@inheritDoc}
     *
     * @throws com.intel.icecp.node.security.tpm.exception.TpmOperationError
     */
    @Override
    public SealedData sealData(InputStream data, byte[] password) throws TpmOperationError {

        // Check if caller has the permissions to seal data
        SecurityUtils.checkPermission(new Tpm1_2Permissions(Tpm1_2Permissions.SEAL));

        // Initialize TPM context
        int context = initContext();

        try {
            // Load the SRK, passing null as we assume using the well known secret
            Tss1_2NativeInterface.loadSrk(context, null);

            // Create a Key, ecnrypt, and then seal the used symmetric key
            SymmetricKey sk;
            // Encryted data
            byte[] encryptedData;

            byte[] rnd = Tss1_2NativeInterface.getRandomBytes(context, this.defaultSymmetricKeySize / 8);

            // This call should use TPM to get random bytes
            sk = KeyProvider.generateSymmetricKey(
                    this.defaultSymmetricAlgorithm,
                    rnd);

            try {
                encryptedData = CryptoProvider.getCipher(SecurityConstants.AES_CBC_ALGORITHM, false).encrypt(StreamUtils.readAll(data), sk);
            } catch (CipherEncryptionError | UnsupportedCipherException | IOException ex) {
                throw new TpmOperationError("Unable to encrypt the given bytes. Cause: " + ex.getMessage());
            }

            // Now seal the symmetric key (May throw a TpmException)
            byte[] sealedDataStruct = Tss1_2NativeInterface.sealData(
                    context,
                    password,
                    this.pcrsSealing,
                    sk.getEncoded());

            // Parse the response bytes
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(sealedDataStruct));

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
                        this.defaultSymmetricAlgorithm);

            } catch (IOException ex) {
                throw new TpmOperationError("Unable to parse the sealed data response. Cause: " + ex.getMessage());
            }

        } catch (TpmOperationError ex) {
            throw ex;
        } finally {
            // Cleanup. Close the context
            closeContext(context);
        }

    }

    /**
     * Data is encoded using the same encoding as tpm_sealdata from tpm_tools
     *
     * {@inheritDoc}
     *
     * @throws com.intel.icecp.node.security.tpm.exception.TpmOperationError
     */
    @Override
    public byte[] unsealData(InputStream data, byte[] password) throws TpmOperationError {

        // Check if caller has the permissions to unseal data
        SecurityUtils.checkPermission(new Tpm1_2Permissions(Tpm1_2Permissions.UNSEAL));

        // Initialize a context
        int context = initContext();

        try {
            //	a) extract what we need from the data
            //	b) buils a SealedData object
            //	c) call DataUnseal

            // Try to read bytes from the inputstream
            byte[] sealedDataBytes = null;
            try {
                sealedDataBytes = StreamUtils.readAll(data);
            } catch (IOException ex) {
                throw new TpmOperationError("Unable to unseal the given input. Cause: " + ex.getMessage());
            }

            SealedData toUnseal = SealedData.decode(sealedDataBytes);
            if (toUnseal == null) {
                throw new TpmOperationError("Unable to decode the given sealed data into a SealedData instance.");
            }

            // We now load the SRK, passing null as we assume using the well known secret
            Tss1_2NativeInterface.loadSrk(context, null);

            // Unseal the symmetric key (may throw TpmOperationError)
            byte[] symmKeyBytes = Tss1_2NativeInterface.unsealData(context, password, toUnseal.sealedSymmKey, toUnseal.sealingKeySpecs);

            // get the symmetric key from the bytes
            SymmetricKey sk;

            sk = KeyProvider.generateSymmetricKey(this.defaultSymmetricAlgorithm, symmKeyBytes);
            try {

                // Return the data unencrypted
                Cipher c = CryptoProvider.getCipher(this.defaultSymmetricAlgorithm, false);
                return c.decrypt(toUnseal.encryptedData, sk);

            } catch (CipherDecryptionError | UnsupportedCipherException ex) {
                throw new TpmOperationError("Unable to decrypt the given data. Cause: " + ex.getMessage());
            }
        } catch (TpmOperationError ex) {
            throw ex;
        } finally {
            closeContext(context);
        }

    }

    /**
     * ***************************** ATTESTATION *****************************
     */
    /**
     * Callback used for Identity request
     *
     */
    interface IdentityRequestCallback {

        /**
         * After identity has been created
         *
         * @param identityCertificate
         * @param identityKey
         */
        void onIdentityCreated(byte[] identityCertificate, byte[] identityKey);

        /**
         * If an error happened
         *
         * @return
         */
        void onError();

    }

    class IdentityRequestHandler implements Runnable {

        private final IdentityRequestCallback callback;

        public IdentityRequestHandler(IdentityRequestCallback callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            // Do stuff...

            // callback.onIdentityCreated(identityCertificate, identityKey);
            // or
            // callback.onError(); 
        }

    }

    /**
     * {@inheritDoc}
     *
     * @throws com.intel.icecp.node.security.tpm.exception.TpmOperationError
     */
    @Override
    public Message requestDeviceAttestation() throws TpmOperationError {
        // Initialize if necessary
//		int context = init();
//		
//		try {
//		
//		
//		// @TODO
//		
//		} catch (TpmOperationError ex) {
//			
//		} finally {
//			close(context);
//		}
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * {@inheritDoc}
     *
     * @throws com.intel.icecp.node.security.tpm.exception.TpmOperationError
     */
    @Override
    public Message createIdentityRequest(byte[] identityKeySecret, Certificate caCertificate) throws TpmOperationError {

        // Check if caller has the permissions to create the identity request
        SecurityUtils.checkPermission(new Tpm1_2Permissions(Tpm1_2Permissions.ATTEST));

        // Initialize a context
        int context = initContext();

        try {

            // Load SRK
            Tss1_2NativeInterface.loadSrk(context, null);

            //@TODO: MOVE OWNER PASSWORD AND AIK LABEL TO A CONFIGURATION FILE!
            // Set TPM owner secret
            Tss1_2NativeInterface.setTpmOwnerPriviledges(context, "1234".getBytes());

            byte[] aikRequestRes = Tss1_2NativeInterface.generateIdentityRequest(context, identityKeySecret, PEMEncodingUtils.encodeCertificate(caCertificate), "aikLabel".getBytes());

            Tss1_2NativeInterface.flushTpmOwnerPriviledges(context);

            DataInputStream aikIs = new DataInputStream(new ByteArrayInputStream(aikRequestRes));

            // Read the Key bytes
            int aikLen = aikIs.readInt();
            byte[] aikBytes = new byte[aikLen];
            aikIs.read(aikBytes, 0, aikLen);

            // Read the Request bytes
            int aikRequestLen = aikIs.readInt();
            byte[] aikRequest = new byte[aikRequestLen];
            aikIs.readFully(aikRequest);

            // @TODO:	We should the encrypted AIK key bytes somewhere, and keep track of the pending 
            //			Identity requests!
            // generate a symmetric key
            SymmetricKey sk = KeyProvider.generateSymmetricKey(SecurityConstants.AES_CBC_ALGORITHM, 128);

            // Read EK credentials from file
            FileInputStream fis = new FileInputStream("certificates/ek_cred.chain");
            byte[] encEKCredentials = CryptoProvider.getCipher(SecurityConstants.AES_CBC_ALGORITHM, false).encrypt(StreamUtils.readAll(fis), sk);
            fis.close();

            // Finally, we can encrypt the symmetric key with Privacy CA's public key
            PublicKey caPubKey = new PublicKey(caCertificate.getPublicKey());
            byte[] encryptedSymmKey = CryptoProvider.getCipher(SecurityConstants.RSA_ALGORITHM, false).encrypt(sk.getEncoded(), caPubKey);

            // Create and return the message 
            TpmAttestationIdentityRequest identityRequest = new TpmAttestationIdentityRequest();

            identityRequest.identityRequest = aikRequest;
            identityRequest.encryptedEkCertificate = encEKCredentials;
            identityRequest.ecryptedSymmetricKey = encryptedSymmKey;

            return identityRequest;

        } catch (IOException | CertificateEncodingException | CipherEncryptionError | UnsupportedCipherException | InvalidKeyTypeException | TpmOperationError ex) {
            throw new TpmOperationError("Unable to create identity request. Cause: " + ex.getMessage());
        } finally {
            closeContext(context);
        }

    }

    /**
     * {@inheritDoc}
     *
     * @throws com.intel.icecp.node.security.tpm.exception.TpmOperationError
     */
    @Override
    public Message issueIdentityCredentials(Message identityRequestMessage) throws TpmOperationError {

        // Initialize if necessary
        int context = initContext();

        try {

            //	a.1) Retrieve our private and public key from keystore				
            PrivateKey caPriveateKey;
            Certificate caCertificate;
            byte[] caCertificateBytes;
            byte[] caPrivateKeyBytes;
            try {
                caPriveateKey = keyManager.getPrivateKey("/root/ca");
                caCertificate = keyManager.getCertificate("/root/ca", null); // @FIXME: for now, we assume it is stored under /root/ca

                caCertificateBytes = PEMEncodingUtils.encodeCertificate(caCertificate);
                caPrivateKeyBytes = PEMEncodingUtils.encodePrivateKey(caPriveateKey);

            } catch (CertificateEncodingException | KeyManagerException ex) {
                throw new TpmOperationError("Error in processing identity request. Unable to retrieve public and private key. Cause: " + ex.getMessage());
            }

            //	a.2) Unpack the request message
            TpmAttestationIdentityRequest message = (TpmAttestationIdentityRequest) identityRequestMessage;

            // This should be encrypted with CA's public key
            SymmetricKey symmKey;
            try {
                byte[] symmKeyBytes = CryptoProvider.getCipher(SecurityConstants.RSA_ALGORITHM, false).decrypt(message.ecryptedSymmetricKey, caPriveateKey);
                symmKey = KeyProvider.generateSymmetricKey(SecurityConstants.AES_CBC_ALGORITHM, symmKeyBytes);
            } catch (CipherDecryptionError | UnsupportedCipherException ex) {
                throw new TpmOperationError("Error in processing identity request. Unable to decrypt symmetric key. Cause: " + ex.getMessage());
            }

            // If we get the symmetric key, we can extract EK cert
            byte[] requesterEkCert;
            try {
                requesterEkCert = CryptoProvider.getCipher(SecurityConstants.AES_CBC_ALGORITHM, false).decrypt(message.encryptedEkCertificate, symmKey);
            } catch (UnsupportedCipherException | CipherDecryptionError ex) {
                throw new TpmOperationError("Error in processing identity request. Unable to decrypt EK cert. Cause: " + ex.getMessage());
            }

            //	b) Verify EK and Patform credentials				@FIXME: For now we ignore platform credentials
            Certificate ekCertificate;
            try {
                ekCertificate = keyManager.verifyCertificateChain(requesterEkCert);
            } catch (KeyManagerException ex) {
                throw new TpmOperationError("Impossible to verify EK credentials. Cause: " + ex.getMessage());
            }

            //	c) Verify identity binding
            byte[] aikres = Tss1_2NativeInterface.verifyIdentityRequestBinding(context, message.identityRequest, caPrivateKeyBytes, caCertificateBytes);

            //	d) Generate the credentials for the client								
            // Extract the public key from the PUBKEY structure returned by verifyIdentityRequestBinding 
            byte[] aikPublicKey = Tss1_2NativeInterface.extractPEMPublicKeyFromTPMPubKeyBlob(context, aikres);

            // And create AIK credentials
            byte[] aikCredentials = Tss1_2NativeInterface.createCredentials(context, aikPublicKey, caPrivateKeyBytes, caCertificateBytes);

            //	e) Construct an attestation response
            byte[] attestData = Tss1_2NativeInterface.createAttestationResponse(
                    context,
                    PEMEncodingUtils.encodePublicKey(new PublicKey(ekCertificate.getPublicKey())), // This should be EK pub key.
                    aikres, // Result of verifyIdentityRequestBinding
                    aikCredentials);

            TpmAttestationIdentityCredetialsResponse result = new TpmAttestationIdentityCredetialsResponse();
            result.aikIdentityResponse = attestData;

            return result;

        } catch (TpmOperationError ex) {
            throw ex;
        } finally {
            closeContext(context);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String id() {
        return SecurityConstants.TPM_1_2_SERVICE;
    }

}
