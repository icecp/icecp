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

import com.intel.icecp.node.security.tpm.exception.TpmOperationError;

/**
 * Native interface to interact with the TouSerS TPMD daemon; all methods have
 * package visibility; another class in this package must take care of exposing
 * the TPM functionalities.
 *
 *
 */
class Tss1_2NativeInterface {

    static void load() throws TpmOperationError {
        try {
            System.loadLibrary("tss1_2");
        } catch (UnsatisfiedLinkError err) {
            throw new TpmOperationError("Unable to load the native library. Cause: " + err.getMessage());
        }
    }

    /**
     * Creates a new TPM context, if possible, and returns the corresponding
     * context identification code.
     *
     * @return new context ID, or -1 in case of error
     * @throws com.intel.icecp.node.security.tpm.exception.TpmOperationError
     *
     */
    synchronized static native int createTpmContext() throws TpmOperationError;

    /**
     * Loads all the registered keys (To be called at library loading time)
     *
     * @throws com.intel.icecp.node.security.tpm.exception.TpmOperationError
     *
     */
    static native void loadRegisteredKeys(int contextId) throws TpmOperationError;

    /**
     * Deletes the Context Identified by the given ID
     *
     * @param contextId the ID of the context to delete
     * @throws com.intel.icecp.node.security.tpm.exception.TpmOperationError
     */
    static native void deleteTpmContext(int contextId) throws TpmOperationError;

    /**
     * Sets owner privileges for the given context, using the owner's password
     *
     * @param contextId
     * @param ownerSecret
     * @throws com.intel.icecp.node.security.tpm.exception.TpmOperationError
     */
    static native void setTpmOwnerPriviledges(
            int contextId,
            byte[] ownerSecret) throws TpmOperationError;

    /**
     * Creates owner delegation (requires owner privileges)
     *
     * @param contextId
     * @return
     * @throws TpmOperationError
     */
    static native byte[] createOwnerDelegation(
            int contextId) throws TpmOperationError;

    /**
     * Loads the SRK (throws an exception if not possible)
     *
     * @param contextId
     * @param srkSecret the 20 bytes SRK secret (null for well known 0 secret)
     * @throws com.intel.icecp.node.security.tpm.exception.TpmOperationError
     */
    static native void loadSrk(
            int contextId,
            byte[] srkSecret) throws TpmOperationError;

    /**
     * Calculates the (SHA-1) hash value of the given data.
     *
     * @param context
     * @param data
     * @return
     * @throws TpmOperationError
     */
    static native byte[] calculateHash(
            int context,
            byte[] data) throws TpmOperationError;

    /**
     * Creates a key according to the parameters specified
     *
     * @param contextId
     * @param keyType
     * @param keySize
     * @param migratable
     * @param keyAuthorization
     * @param keyVolatile
     * @param persistentStorageType
     * @param load
     * @param keySecret
     *
     * @return key UUID index in [2,256), for later key retrieval
     * @throws com.intel.icecp.node.security.tpm.exception.TpmOperationError
     */
    static native byte[] createKey(
            int contextId,
            int keyType,
            int keySize,
            int migratable,
            int keyAuthorization,
            int keyVolatile,
            int persistentStorageType,
            boolean load,
            byte[] keySecret) throws TpmOperationError;

    /**
     *
     *
     * @param contextId
     * @param persistentStorageType
     * @throws com.intel.icecp.node.security.tpm.exception.TpmOperationError
     */
    static native void unregisterKey(
            int contextId,
            byte[] keyUuid,
            int persistentStorageType) throws TpmOperationError;

    /**
     * Returns the Public key bytes, in PEM format, from key UUID. No key secret
     * is required since we want only the public part
     *
     * @param contextId
     * @param uuid	Bytes of the UUID
     *
     * @return Public key in PEM format, as bytes
     *
     * @throws TpmOperationError
     */
    static native byte[] getPEMPublicKeyByUUID(
            int contextId,
            byte[] uuid) throws TpmOperationError;

    /**
     * Returns EK's public key (Requires TPM Owner privileges)
     *
     * @param contextId
     * @return
     */
    static native byte[] getPEMEKPublicKey(
            int contextId) throws TpmOperationError;

    /**
     * Returns the PEM public key from a PUB_KEY blob
     *
     * @param contextId
     * @param publicKeyBlob
     *
     * @return Public key in PEM format, as bytes
     *
     * @throws TpmOperationError
     */
    static native byte[] extractPEMPublicKeyFromTPMPubKeyBlob(
            int contextId,
            byte[] publicKeyBlob) throws TpmOperationError;

    /**
     * @TODO: NOT IMPLEMENTED YET
     *
     * Returns the PEM public key from a KEY_BLOB
     *
     * @param contextId
     * @param keyBlob
     *
     * @return Public key in PEM format, as bytes
     *
     * @throws TpmOperationError
     */
    static native byte[] extractPEMPublicKeyFromTPMKeyBlob(
            int contextId,
            byte[] keyBlob) throws TpmOperationError;

    /**
     * Resets the TPM policy by removing owner privileges from the context
     *
     * @param contextId
     * @throws com.intel.icecp.node.security.tpm.exception.TpmOperationError
     */
    static native void flushTpmOwnerPriviledges(
            int contextId) throws TpmOperationError;

    /**
     * Performs sealing operation on the given data, using the given sealing key
     * index; if keyIndex is 0, data is sealed using SRK. Sealing is bound to
     * the given PCRs, and is useful, for example, if we want to allow the
     * decryption only if specific PCRs remain in a certain status
     *
     * @param contextId
     * @param keyPassword
     * @param pcrRegisters
     * @param dataToSeal
     * @return
     * @throws TpmOperationError
     */
    static native byte[] sealData(
            int contextId,
            byte[] keyPassword,
            int[] pcrRegisters,
            byte[] dataToSeal) throws TpmOperationError;

    /**
     *
     * Perform unsealing of the given data using the given key id.
     *
     * @param contextId
     * @param keyPassword
     * @param dataToUnseal
     * @param sealingKeyBlob
     * @return
     * @throws TpmOperationError
     */
    static native byte[] unsealData(
            int contextId,
            byte[] keyPassword,
            byte[] dataToUnseal,
            byte[] sealingKeyBlob) throws TpmOperationError;

    /**
     * Extends a PCR value with the given input data ("Attestee" method)
     *
     * @param contextId
     * @param pcrIndex
     * @param data
     * @return
     * @throws TpmOperationError
     */
    static native byte[] pcrExtend(
            int contextId,
            int pcrIndex,
            byte[] data) throws TpmOperationError;

    /**
     * Returns a random number of bytes
     *
     * @param contextId
     * @param bytesNum
     * @return
     */
    static native byte[] getRandomBytes(
            int contextId,
            int bytesNum);

    /**
     * ************************** ATTESTATION *******************************
     */
    /**
     * Generates an Attestation Identity Request, for Privacy CA based remote
     * attestation.
     *
     * @param contextId
     * @param keySecret
     * @param caX509Cert
     * @return Request bytes
     * @throws com.intel.icecp.node.security.tpm.exception.TpmOperationError
     */
    static native byte[] generateIdentityRequest(
            int contextId,
            byte[] keySecret,
            byte[] caX509Cert,
            byte[] keyLabel) throws TpmOperationError;

    /**
     * Verifies the identity binding of the given Identity request (Privacy CA
     * method)
     *
     * @param contextId
     * @param aikRequest
     * @param privacyCAprivateKey
     * @param privacyCApubKey
     * @return
     * @throws TpmOperationError
     */
    static native byte[] verifyIdentityRequestBinding(
            int contextId,
            byte[] aikRequest,
            byte[] privacyCAprivateKey,
            byte[] privacyCApubKey) throws TpmOperationError;

    /**
     * Creates the attestation data for client's TPM (Privacy CA method)
     *
     * @param contextId
     * @param ekCertificate
     * @param aikPublicKey
     * @param aikCertificate
     * @return
     * @throws TpmOperationError
     */
    static native byte[] createAttestationResponse(
            int contextId,
            byte[] ekCertificate,
            byte[] aikPublicKey, // This is the value returned by verifyIdentityRequestBinding
            byte[] aikCertificate) // X509 cerificare released by the CA		
            throws TpmOperationError;

    /**
     * Activates the identity received from a Privacy CA. (Client method)
     *
     * @param contextId
     * @param attestationData
     * @return
     * @throws TpmOperationError
     */
    static native byte[] activateIdentity(
            int contextId,
            byte[] attestationData,
            byte[] aikBytes) throws TpmOperationError;

    /**
     * Creates a X509 certificate for the given public key
     *
     * @param contextId
     * @param publicKey
     * @param issuerPrivateKey
     * @param issuerCertificate
     *
     * @return X509 certificate in PEM format
     */
    static native byte[] createCredentials(
            int contextId,
            byte[] publicKey,
            byte[] issuerPrivateKey,
            byte[] issuerCertificate);

    /**
     * Quoting operation: sign the value of the given PCR with a given AIK.
     *
     * @param contextId
     * @param encAikBytes	AIK key blob, exported encrypted from the TPM using
     * TSS_TSPATTRIB_KEY_BLOB, returned by generateIdentityRequest
     * @param nonce	Random 20 bytes nonce
     * @param pcrs	PCR indexes to consider
     * @return
     * @throws TpmOperationError
     */
    static native byte[] quote(
            int contextId,
            byte[] encAikBytes,
            byte[] nonce,
            int[] pcrs) throws TpmOperationError;

    static native void verifyQuote(
            int contextId,
            byte[] quoteValue,
            byte[] quoteInfo,
            byte[] aikCertificate,
            byte[] expectedPCRValue,
            byte[] nonce) throws TpmOperationError;

}
