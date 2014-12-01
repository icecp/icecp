/*
 * File name: tspi_interface_attestation.c
 * 
 * Purpose: Implementation of tspi_interface functions related to device attestation 
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 *
 *
 */
#include "tspi_interface.h"


#include <limits.h>   // For USHRT_MAX

#include <openssl/bn.h>


#include "cert_utils.h"

/**
 * Imports the RSA public key of a certificate inside the TPM 
 * 
 * @param context			
 * @param hKey 				Pointer to the key handle
 * @param certificate 		X509 Certificate
 * 
 * @return SUCCESS/ERROR
 */
T_OUTCOME
load_key_from_certificate(
        Context* context,
        TSS_HKEY* hKey,
        X509* certificate) {

    RSA* rsa = NULL;
    uint8_t functionOutcome = SUCCESS;
    TSS_RESULT result;
    BYTE n[2048];
    int size_n;

    // Create the key for the TPM
    result = Tspi_Context_CreateObject(
            context -> hContext,
            TSS_OBJECT_TYPE_RSAKEY,
            TSS_KEY_TYPE_LEGACY | TSS_KEY_SIZE_2048,
            hKey);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionOutcome, "Tspi_Context_CreateObject");

    // Extract the exponent from PrivacyCA's certificate
    VERIFY_AND_TERMINATE(!cert_utils_rsa_from_x509_cert(certificate, &rsa), functionOutcome, "cert_utils_rsa_from_x509_cert");

    VERIFY_AND_TERMINATE((size_n = BN_bn2bin(rsa->n, n)) <= 0, functionOutcome, "BN_bn2bin");

    // Set it as an attribute
    result = Tspi_SetAttribData(
            *hKey,
            TSS_TSPATTRIB_RSAKEY_INFO,
            TSS_TSPATTRIB_KEYINFO_RSA_MODULUS,
            size_n,
            n);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionOutcome, "Tspi_SetAttribData for PCA modulus");

    // Set attribute with scheme used
    result = Tspi_SetAttribUint32(
            *hKey,
            TSS_TSPATTRIB_KEY_INFO,
            TSS_TSPATTRIB_KEYINFO_ENCSCHEME,
            TSS_ES_RSAESPKCSV15);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionOutcome, "Tspi_SetAttribUint32 for PCA encscheme");

cleanup:
    return functionOutcome;
}

/**
 * Creates an Identity Request (for attestation). The function
 * must be called after obtaining TPM owner privileges (with 
 * tspi_set_tpm_owner_privileges).
 *
 * @return ERROR/SUCCESS
 * 
 */
T_OUTCOME
tspi_interface_create_identity_request(
        Handle contextHandle,
        BYTE* keySecret,
        uint8_t keySecretLength,
        X509* privacyCaCert,
        BYTE* label,
        size_t labelLen,
        BYTE** identityRequestBytes,
        UINT32* identityRequestLength) {



    Context* context;

    uint8_t functionOutcome = SUCCESS;

    TSS_RESULT result;
    // Handle for Privacy CA's certificate
    TSS_HKEY hPCAKey;

    // Handle for the new AIK
    TSS_HKEY hAik;

    BYTE* newAikBytes = NULL;
    size_t newAikBytesLen = 0;
    size_t resultSize = 0;
    BYTE* resultBytes = NULL;
    UINT64 offset = 0;


    // AIK is volatile, non migratable and of size 2048
    UINT32 initFlags = TSS_KEY_TYPE_IDENTITY | TSS_KEY_SIZE_2048 | TSS_KEY_VOLATILE | TSS_KEY_NOT_MIGRATABLE;

    // Try to load the context
    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), functionOutcome, "cm_get_context");



    // Now we can create the AIK object 
    result = Tspi_Context_CreateObject(
            context -> hContext,
            TSS_OBJECT_TYPE_RSAKEY,
            initFlags,
            &hAik); // Create a pending AIK object
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionOutcome, "Tspi_Context_CreateObject");


    // Load CA's certificate into the TPM
    result = load_key_from_certificate(
            context,
            &hPCAKey,
            privacyCaCert);
    VERIFY_AND_TERMINATE(result == ERROR, functionOutcome, "load_key_from_certificate");


    BYTE* rgbIdentityLabelData = (BYTE *) Trspi_Native_To_UNICODE(label, &labelLen);

    VERIFY_AND_TERMINATE(rgbIdentityLabelData == NULL, functionOutcome, "Trspi_Native_To_UNICODE");

    result = Tspi_TPM_CollateIdentityRequest(
            context -> hTPM,
            context -> hSRK,
            hPCAKey,
            labelLen,
            rgbIdentityLabelData,
            hAik, // Handle of the AIK key object
            TSS_ALG_AES, // Use AES for encryption
            identityRequestLength, // Here we will have the length of the request (in bytes)
            identityRequestBytes); // Here we will have the request bytes
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionOutcome, "Tspi_TPM_CollateIdentityRequest");

    // We export the AIK as a KEY_BLOB
    result = Tspi_GetAttribData(
            hAik,
            TSS_TSPATTRIB_KEY_BLOB,
            TSS_TSPATTRIB_KEYBLOB_BLOB,
            &newAikBytesLen,
            &newAikBytes);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionOutcome, "Tspi_GetAttribData");

    resultSize = 2 * sizeof (UINT32) + *identityRequestLength + newAikBytesLen;
    resultBytes = malloc(resultSize);

    // Copy the key Info
    Trspi_LoadBlob_UINT32(
            &offset,
            newAikBytesLen,
            resultBytes);

    Trspi_LoadBlob(
            &offset,
            newAikBytesLen,
            resultBytes,
            newAikBytes);

    // Copy the aik request
    Trspi_LoadBlob_UINT32(
            &offset,
            *identityRequestLength,
            resultBytes);

    Trspi_LoadBlob(
            &offset,
            *identityRequestLength,
            resultBytes,
            *identityRequestBytes);


    *identityRequestBytes = resultBytes;
    *identityRequestLength = resultSize;

cleanup:
    Tspi_Context_CloseObject(context -> hContext, hPCAKey);
    Tspi_Context_CloseObject(context -> hContext, hAik);

    return functionOutcome;

}

/**
 * Utility function that loads an Identity key given a TCPA_PUBKEY
 *
 * @param context 				Valid context pointer
 * @param hIdKey 				Identity key handle pointer
 * @param identityKey 			TCPA_PUBKEY reference
 *
 * @return the TSS_RESULT result 
 *
 */
TSS_RESULT
load_identity_key_pub_key(
        Context* context,
        TSS_HKEY* hIdKey,
        TCPA_PUBKEY* identityKey) {
    BYTE blob[2048];
    UINT64 offset;

    // We need to blob the Identity Key to load it into the 
    // TPM (so the TSS takes care of filling all the required attributes)
    offset = 0;
    Trspi_LoadBlob_PUBKEY(
            &offset, // out: size of the identity key
            blob,
            identityKey); // in:  identity key

    return tspi_utils_load_public_key_blob(
            context,
            hIdKey,
            TSS_KEY_TYPE_SIGNING | TSS_KEY_SIZE_2048 | TSS_KEY_NO_AUTHORIZATION | TSS_KEY_NOT_MIGRATABLE,
            offset,
            blob);
}

/**
 * Given a valid context, verify the identity binding for the received identity proof
 *
 * @param context 			A valid context
 * @param privacyCaCert		Certificate of the Privacy CA (i.e., our certificate)
 * @param hPrvCA 			Handle of out priate key
 * @param identityProof 	The identity proof struct to verify
 * @param hIdKey 			Handle that will contain the public part of the AIK key relative to the request
 *
 * @return TSS_SUCCESS if the binding is verified
 *
 */
TSS_RESULT
verify_identity_binding(
        Context* context,
        X509* privacyCaCert,
        TSS_HKEY* hPrvCA,
        TCPA_IDENTITY_PROOF* identityProof,
        TSS_HKEY* hIdKey) {

    TSS_RESULT result;
    // Offset
    UINT64 offset;
    TCPA_DIGEST digest;
    // Digest of the ID chosen by the request sender (SHA-1(label))
    TCPA_DIGEST chosenId;

    BYTE* keyBlob;
    size_t keyBlobSize;

    // Utility bytes container
    BYTE blob[2048];

    // The public key
    TCPA_KEY key;

    TSS_HHASH hHash;
    // Create hash object
    result = Tspi_Context_CreateObject(
            context -> hContext,
            TSS_OBJECT_TYPE_HASH,
            TSS_HASH_SHA1, &hHash);
    if (result != TSS_SUCCESS) {
        print_err(__LINE__, __func__, "Tspi_Context_CreateObject returned %i - %s ", result, Trspi_Error_String(result));
        // No need for cleanup
        return result;
    }


    // Load the public key into the TPM and get the handle hPrvCA
    result = load_key_from_certificate(
            context,
            hPrvCA,
            privacyCaCert);
    if (result == ERROR) {
        print_err(__LINE__, __func__, "load_key_from_certificate");
        goto cleanup;
    }


    // Get the key blob
    result = Tspi_GetAttribData(
            *hPrvCA,
            TSS_TSPATTRIB_KEY_BLOB,
            TSS_TSPATTRIB_KEYBLOB_BLOB,
            &keyBlobSize, &keyBlob);
    if (result != TSS_SUCCESS) {
        print_err(__LINE__, __func__, "Tspi_GetAttribData returned %i - %s ", result, Trspi_Error_String(result));
        goto cleanup;
    }



    offset = 0;
    // Safe operation since we are "unblobbing" a blob just retrieved from Tspi_GetAttribData
    result = Trspi_UnloadBlob_KEY(&offset, keyBlob, &key);
    if (result != TSS_SUCCESS) {
        print_err(__LINE__, __func__, "Trspi_UnloadBlob_KEY returned %i - %s ", result, Trspi_Error_String(result));
        goto cleanup;
    }


    // Compute the hash of the ID (label)
    offset = 0;
    // Load the label into the blob
    Trspi_LoadBlob(
            &offset,
            identityProof -> labelSize,
            blob,
            identityProof -> labelArea);
    // Load the key into blob
    Trspi_LoadBlob_KEY_PARMS(
            &offset,
            blob,
            &key.algorithmParms);
    // load public key bytes into the blob
    Trspi_LoadBlob_STORE_PUBKEY(
            &offset,
            blob,
            &key.pubKey);


    // Compute the hash with SHA-1
    // SHA-1 ( label || TCPA_PUBKEY(CA_key) )
    result = Trspi_Hash(
            TSS_HASH_SHA1,
            offset,
            blob,
            (BYTE *) & chosenId.digest);
    if (result != TSS_SUCCESS) {
        print_err(__LINE__, __func__, "Trspi_Hash returned %i - %s ", result, Trspi_Error_String(result));
        goto cleanup;
    }




    // TPM version for the Privacy CA attestation is TPM 1.1
    TSS_VERSION VERSION_1_1 = {1, 1, 0, 0};

    // Reset the offset
    offset = 0;

    // size_t versionSize;
    // BYTE* versionBlob;
    // if ((result = Tspi_TPM_GetCapability(
    // 	context -> hTPM,
    // 	TSS_TPMCAP_VERSION,
    // 	0,
    // 	NULL,
    // 	&versionSize,
    // 	&versionBlob))) {
    // 	print_err (__LINE__, __func__, "Tspi_TPM_GetCapability returned %i - %s ",result, Trspi_Error_String(result));
    // 	return result;
    // }
    // Trspi_LoadBlob(
    // 	&offset,
    // 	versionSize,
    // 	blob,
    // 	versionBlob);




    Trspi_LoadBlob_TSS_VERSION(
            &offset,
            blob,
            VERSION_1_1);
    Trspi_LoadBlob_UINT32(
            &offset,
            TPM_ORD_MakeIdentity,
            blob);
    Trspi_LoadBlob(
            &offset,
            20,
            blob,
            (BYTE *) & chosenId.digest);
    Trspi_LoadBlob_PUBKEY(
            &offset,
            blob,
            &identityProof -> identityKey);


    // Hash the info
    result = Trspi_Hash(
            TSS_HASH_SHA1,
            offset,
            blob,
            (BYTE *) & digest.digest);
    if (result != TSS_SUCCESS) {
        goto cleanup;
    }


    // Set the new hash vale
    result = Tspi_Hash_SetHashValue(hHash, 20, (BYTE *) & digest.digest);

    if (result != TSS_SUCCESS) {
        print_err(__LINE__, __func__, "Tspi_Hash_SetHashValue", result);
        goto cleanup;
    }



    result = load_identity_key_pub_key(
            context,
            hIdKey,
            &identityProof -> identityKey);
    if (result != TSS_SUCCESS) {
        print_err(__LINE__, __func__, "load_identity_key_pub_key returned %i - %s ", result, Trspi_Error_String(result));
        goto cleanup;
    }


    // Now we can verify the signature on the binding size
    result = Tspi_Hash_VerifySignature(
            hHash,
            *hIdKey,
            identityProof -> identityBindingSize,
            identityProof -> identityBinding);

    if (result != TSS_SUCCESS) {
        print_err(__LINE__, __func__, "Tspi_Hash_VerifySignature returned %i - %s ", result, Trspi_Error_String(result));
    }

cleanup:
    // Close hash
    Tspi_Context_CloseObject(context -> hContext, hHash);
    Tspi_Context_CloseObject(context -> hContext, *hIdKey);

    // Cleanup: free keyBlob
    if (keyBlob) {
        Tspi_Context_FreeMemory(context -> hContext, keyBlob);
    }

    if (key.algorithmParms.parms) {
        free(key.algorithmParms.parms);
    }
    if (key.pubKey.key) {
        free(key.pubKey.key);
    }
    if (key.encData) {
        free(key.encData);
    }
    if (key.PCRInfo) {
        free(key.PCRInfo);
    }

    return result;
}

T_OUTCOME
tspi_interface_validate_identity_binding(
        Handle contextHandle,
        BYTE* identityReqBlob,
        size_t identityReqBlobLen,
        BYTE* privCAsecretKey,
        size_t privCAsecretKeyLen,
        X509* privacyCaCert,
        BYTE** aikPublicKey,
        size_t* aikPublicKeyLen) {
    Context* context;

    // Identity request sent by the client
    TCPA_IDENTITY_REQ identityReq;
    // Symmetric key inside the request
    TCPA_SYMMETRIC_KEY symKey;
    TCPA_ALGORITHM_ID algID;

    // Handle to the public part of AIK, to be loaded into the TPM
    TSS_HKEY hIdKey;

    // The identity proof sent by the client. TCPA_IDENTITY_PROOF is defined as
    //
    // typedef struct tdTPM_IDENTITY_PROOF                         /* 1.1b */
    // {
    //     TPM_STRUCT_VER  ver;
    //     UINT32          labelSize;
    //     UINT32          identityBindingSize;
    //     UINT32          endorsementSize;
    //     UINT32          platformSize;
    //     UINT32          conformanceSize;
    //     TPM_PUBKEY      identityKey;
    //     SIZEIS(labelSize)
    //       BYTE         *labelArea;
    //     SIZEIS(identityBindingSize)
    //       BYTE         *identityBinding;
    //     SIZEIS(endorsementSize)
    //       BYTE         *endorsementCredential;
    //     SIZEIS(platformSize)
    //       BYTE         *platformCredential;
    //     SIZEIS(conformanceSize)
    //       BYTE         *conformanceCredential;
    // } TPM_IDENTITY_PROOF;
    TCPA_IDENTITY_PROOF identityProof;

    // Handle for provacy CA cert (in this case, we are the privacy CA)
    TSS_HKEY hPrvCA;

    UINT64 offset;
    RSA* rsa = NULL;
    BYTE utilityBlob[USHRT_MAX];
    size_t utilityBlobSize;
    // Padding type
    int padding = RSA_PKCS1_PADDING;
    // Temporary int to collect results
    int decryptedReqSize;
    TSS_RESULT result;


    // Result
    UINT16 outcome = SUCCESS;

    // Try to load the context
    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), outcome, "cm_get_context");

    // Read the private key, and fail if not possible
    VERIFY_AND_TERMINATE(!cert_utils_rsa_priv_key_from_bytes(privCAsecretKey, privCAsecretKeyLen, &rsa), outcome, "cert_utils_rsa_priv_key_from_bytes");


    // Unlod the TCPA_IDENTITY_REQ blobs 
    offset = 0;
    result = unblob_IDENTITY_REQ(
            &offset,
            identityReqBlob,
            &identityReq,
            identityReqBlobLen);
    VERIFY_AND_TERMINATE(!result, outcome, "unblob_IDENTITY_REQ");


    // Decrypt the request with our private key
    decryptedReqSize = RSA_private_decrypt(
            identityReq.asymSize,
            identityReq.asymBlob, // To be decrypted
            utilityBlob, // Result will be placed here
            rsa, padding);
    VERIFY_AND_TERMINATE(decryptedReqSize <= 0, outcome, "RSA_private_decrypt");

    // Extract the secret key from utilityBlob and place it in symKey
    offset = 0;
    result = unblob_SYMMETRIC_KEY(
            &offset,
            utilityBlob,
            &symKey,
            decryptedReqSize);
    VERIFY_AND_TERMINATE(!result, outcome, "unblob_SYMMETRIC_KEY");

    // utilityBlobSize = sizeof(utilityBlob);
    // Decrypt
    result = Trspi_SymDecrypt(
            symKey.algId, // Symmetric key algorithm
            symKey.encScheme, // Symmetric encryption mode (must be either TSS_ES_NONE or TCPA_ES_NONE)
            symKey.data, // Key to use for decryption
            NULL, // No IV provided
            identityReq.symBlob, // Blob to decrypt
            identityReq.symSize,
            utilityBlob, // Location where to store the result
            &utilityBlobSize);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Trspi_SymDecrypt");

    // Reset the offset
    offset = 0;
    // Extract the identity proof
    result = Trspi_UnloadBlob_IDENTITY_PROOF(
            &offset,
            utilityBlob,
            &identityProof);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Trspi_UnloadBlob_IDENTITY_PROOF");

    // Verify idendity binding, i.e., verify that:
    // 	identityProof -> identityBinding 
    //					= 
    //	Sign_{AIK_prv} ( SHA-1 (identityProof -> labelArea || TCPA_PUBKEY(Priv_CA_pub_key) ) )
    result = verify_identity_binding(
            context,
            privacyCaCert,
            &hPrvCA,
            &identityProof,
            &hIdKey);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "verify_identity_binding");

    // If the result is TSS_SUCCESS, the binding is verifies.
    // We can extract the AIK public key and return it inside the given
    // parameters.
    BYTE blob[2048];
    offset = 0;
    Trspi_LoadBlob_PUBKEY(
            &offset, // out: size of the identity key
            blob,
            &identityProof.identityKey); // in: Identity key

    *aikPublicKeyLen = offset;
    *aikPublicKey = (BYTE*) malloc(sizeof (BYTE) * (*aikPublicKeyLen));
    memcpy(*aikPublicKey, blob, *aikPublicKeyLen);


cleanup:
    // Close identity key handle
    Tspi_Context_CloseObject(context -> hContext, hIdKey);
    if (rsa) {
        RSA_free(rsa);
    }

    return outcome;
}

T_OUTCOME
tspi_interface_create_attestation_data(
        Handle contextHandle,
        BYTE* aikPublicKeyBlob,
        size_t aikPublicKeyBlobLen,
        RSA* ekPubKey,
        BYTE* aikCredentials, // NOTE: this may sound redundant, but makes life easy, since there are X509 bytes,
        size_t aikCredentialsLen, // while aikPublicKeyBlob is an encoded TCPA_PUBKEY structure.
        BYTE** attestationData,
        size_t* attestationDataLen) {
    Context* context;
    // Result
    UINT16 outcome = SUCCESS;
    // Public part of AIK
    TCPA_PUBKEY idKey;

    UINT64 offset = 0;

    TSS_RESULT result;

    // Utility blobs for serialization/deserialization
    BYTE blob[2048], blob2[2048];
    size_t blob2Size;

    // Asym CA content for attestation
    TCPA_ASYM_CA_CONTENTS asymContents;
    // Sym CA content for attestation
    TCPA_SYM_CA_ATTESTATION symAttestation;


    // Try to load the context
    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), outcome, "cm_get_context");


    // First, we load the aikPublic key
    result = unblob_PUBKEY(
            &offset,
            aikPublicKeyBlob,
            &idKey,
            aikPublicKeyBlobLen);
    VERIFY_AND_TERMINATE(!result, outcome, "unblob_PUBKEY");

    // (A) Compute the hash of the key, loading each part in a blob, and place
    // the hash value in the corresponding field in asymContents
    offset = 0;
    Trspi_LoadBlob_KEY_PARMS(
            &offset,
            blob,
            &idKey.algorithmParms);
    Trspi_LoadBlob_STORE_PUBKEY(
            &offset,
            blob,
            &idKey.pubKey);
    // Hash bytes go into the digest part of asymContents
    result = Trspi_Hash(
            TSS_HASH_SHA1,
            offset,
            blob,
            (BYTE *) & asymContents.idDigest.digest);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "TestSuite_Hash");


    // (B) Fill the session key part of asymContents. 
    // For now set AES as encryption algorithm
    asymContents.sessionKey.algId = TSS_ALG_AES;
    asymContents.sessionKey.encScheme = TCPA_ES_NONE;
    asymContents.sessionKey.size = 128 / 8;

    // Create a random symm key 
    result = Tspi_TPM_GetRandom(
            context -> hTPM,
            asymContents.sessionKey.size,
            &asymContents.sessionKey.data);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_TPM_GetRandom");

    // (C) Initalize symAttestation data structure
    symAttestation.algorithm.algorithmID = TSS_ALG_AES;
    symAttestation.algorithm.encScheme = TCPA_ES_NONE;
    symAttestation.algorithm.sigScheme = 0;
    symAttestation.algorithm.parmSize = 0;
    symAttestation.algorithm.parms = NULL;


    // (D) Encrypt the newly created AIK certificate with the symmetric key
    result = Trspi_SymEncrypt(
            TSS_ALG_AES,
            TCPA_ES_NONE,
            asymContents.sessionKey.data,
            NULL,
            aikCredentials,
            aikCredentialsLen,
            blob2, &blob2Size);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Trspi_SymEncrypt");


    // Copy the bytes
    symAttestation.credential = (BYTE*) malloc(sizeof (blob2Size));

    symAttestation.credential = blob2;

    // Ad the lenght
    symAttestation.credSize = blob2Size;



    UINT32 asymBlobSize, symBlobSize;
    BYTE* asymBlob;
    BYTE* symBlob;

    // (E) Serialize SYM_CA_ATTESTATION into blob
    offset = 0;
    Trspi_LoadBlob_SYM_CA_ATTESTATION(
            &offset,
            blob,
            &symAttestation);


    symBlob = malloc(offset);
    symBlobSize = offset;
    memcpy(symBlob, blob, offset);



    // (F) Serialize the asym structure and encrypt it with EK's Public key
    offset = 0;
    Trspi_LoadBlob_ASYM_CA_CONTENTS(
            &offset,
            blob,
            &asymContents);

    TSS_HKEY hEkPubKey;
    BYTE* pubEkPart = NULL;
    size_t pubEkPartSize = 0;





    // Load the public part of EK into the TPM, from the given cert
    // Create the key for the TPM
    result = Tspi_Context_CreateObject(
            context -> hContext,
            TSS_OBJECT_TYPE_RSAKEY,
            TSS_KEY_TYPE_LEGACY | TSS_KEY_SIZE_2048,
            &hEkPubKey);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Context_CreateObject");

    BYTE n[2048];
    int size_n;
    VERIFY_AND_TERMINATE((size_n = BN_bn2bin(ekPubKey -> n, n)) <= 0, outcome, "BN_bn2bin");

    // Set it as an attribute
    result = Tspi_SetAttribData(
            hEkPubKey,
            TSS_TSPATTRIB_RSAKEY_INFO,
            TSS_TSPATTRIB_KEYINFO_RSA_MODULUS,
            size_n,
            n);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_SetAttribData for PCA modulus");

    // Set attribute with scheme used
    result = Tspi_SetAttribUint32(
            hEkPubKey,
            TSS_TSPATTRIB_KEY_INFO,
            TSS_TSPATTRIB_KEYINFO_ENCSCHEME,
            TSS_ES_RSAESPKCSV15);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_SetAttribUint32 for PCA encscheme");


    result = Tspi_GetAttribData(
            hEkPubKey,
            TSS_TSPATTRIB_RSAKEY_INFO,
            TSS_TSPATTRIB_KEYINFO_RSA_MODULUS,
            &pubEkPartSize,
            &pubEkPart);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_GetAttribData");

    // ******************** END EK LOADING


    size_t tmpblobsize = 0x2000;
    BYTE tmpblob[0x2000];

    // Encrypt the structure with EK public key
    result = Trspi_RSA_Encrypt(
            blob,
            offset,
            tmpblob,
            &tmpblobsize,
            pubEkPart,
            pubEkPartSize);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "TestSuite_TPM_RSA_Encrypt");


    // Fill the asym blob
    asymBlob = malloc(tmpblobsize);
    asymBlobSize = tmpblobsize;
    memcpy(asymBlob, tmpblob, tmpblobsize);


    // Now we serialize and send back to caller
    *attestationDataLen = asymBlobSize + symBlobSize + 2 * sizeof (UINT32);
    *attestationData = (BYTE*) malloc(*attestationDataLen);
    offset = 0;
    Trspi_LoadBlob_UINT32(
            &offset,
            asymBlobSize,
            *attestationData);

    Trspi_LoadBlob(
            &offset,
            asymBlobSize,
            *attestationData,
            asymBlob);

    Trspi_LoadBlob_UINT32(
            &offset,
            symBlobSize,
            *attestationData);

    Trspi_LoadBlob(
            &offset,
            symBlobSize,
            *attestationData,
            symBlob);



cleanup:
    if (ekPubKey) {
        RSA_free(ekPubKey);
    }

    if (symBlob) {
        free(symBlob);
    }

    if (asymBlob) {
        free(asymBlob);
    }

    return outcome;

}

T_OUTCOME
tspi_interface_install_aik(
        Handle contextHandle,
        BYTE* caRespBlob,
        size_t caRespBlobLen,
        BYTE* aikBytes,
        size_t aikBytesLen,
        BYTE** aikCertificate,
        size_t* aikCertificateLen) {
    Context* context;
    // Result
    UINT16 outcome = SUCCESS;

    UINT32 asymBlobSize, symBlobSize;
    BYTE* asymBlob = NULL;
    BYTE* symBlob = NULL;

    // Handle to the AIK
    TSS_HKEY hAik;

    UINT64 offset = 0;
    TSS_RESULT result;

    // Try to load the context`
    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), outcome, "cm_get_context");


    // Read asymm blob
    outcome = unblob_UINT32(
            &offset,
            &asymBlobSize,
            caRespBlob,
            caRespBlobLen);
    VERIFY_AND_TERMINATE(!outcome, outcome, "unblob_UINT32");

    // Allocate space for asym blob
    asymBlob = malloc(asymBlobSize);

    outcome = unblob_Blob(
            &offset,
            asymBlobSize,
            caRespBlob,
            asymBlob,
            caRespBlobLen);
    VERIFY_AND_TERMINATE(!outcome, outcome, "unblob_Blob");


    // Read symm blob
    outcome = unblob_UINT32(
            &offset,
            &symBlobSize,
            caRespBlob,
            caRespBlobLen);
    VERIFY_AND_TERMINATE(!outcome, outcome, "unblob_UINT32");

    // Allocate space for symm blob
    symBlob = malloc(symBlobSize);

    outcome = unblob_Blob(
            &offset,
            symBlobSize,
            caRespBlob,
            symBlob,
            caRespBlobLen);
    VERIFY_AND_TERMINATE(!outcome, outcome, "unblob_Blob");

    // Load the AIK blob
    result = Tspi_Context_LoadKeyByBlob(
            context -> hContext,
            context -> hSRK,
            aikBytesLen,
            aikBytes,
            &hAik);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Context_LoadKeyByBlob");

    // If everything is fine, we can load the identity key and activate it
    result = Tspi_Key_LoadKey(
            hAik,
            context -> hSRK);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Key_LoadKey");

    // From this operation, we obtain back the AIK credentials (decrypted + checked consistency
    // with the AIK). 
    result = Tspi_TPM_ActivateIdentity(
            context -> hTPM,
            hAik,
            asymBlobSize,
            asymBlob,
            symBlobSize,
            symBlob,
            aikCertificateLen,
            aikCertificate);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_TPM_ActivateIdentity");

cleanup:

    if (symBlob)
        free(symBlob);

    if (asymBlob)
        free(asymBlob);


    return outcome;
}

T_OUTCOME
tspi_interface_pcr_quote(
        Handle contextHandle,
        BYTE* aikKeyBytes,
        size_t aikKeyBytesLen,
        int* pcrs,
        size_t pcrsNum,
        BYTE* nonce, // Always 20 bytes, since a SHA-1
        BYTE** quoteRes,
        size_t* quoteResLen) {
    Context* context;
    T_OUTCOME outcome = SUCCESS;
    TSS_RESULT result;
    TSS_HKEY hAik;
    TSS_HPCRS hPcrs;
    TSS_VALIDATION validationData;

    bool isTpm12 = TRUE;

    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), outcome, "cm_get_context");


    // Load the AIK blob
    result = Tspi_Context_LoadKeyByBlob(
            context -> hContext,
            context -> hSRK,
            aikKeyBytesLen,
            aikKeyBytes,
            &hAik);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Context_LoadKeyByBlob");

    // If everything is fine, we can load the AIK
    result = Tspi_Key_LoadKey(
            hAik,
            context -> hSRK);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Key_LoadKey");



    /* Create a PCR composite object. */
    result = Tspi_Context_CreateObject(
            context -> hContext,
            TSS_OBJECT_TYPE_PCRS,
            TSS_PCRS_STRUCT_INFO_SHORT, // This works with TPM 1.2; if the operation fails, we try TSS_PCRS_STRUCT_INFO
            &hPcrs);

    if (!result) {
        print_log(__LINE__, __func__, "Creating a TPM QUOTE_INFO 1.1 struct");

        // We had an error, for sure we have not a TPM 1.2
        isTpm12 = FALSE;
        result = Tspi_Context_CreateObject(
                context -> hContext,
                TSS_OBJECT_TYPE_PCRS,
                TSS_PCRS_STRUCT_INFO,
                &hPcrs);
        VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Context_CreateObject - PCR ");
    }


    size_t i = 0;
    for (; i < pcrsNum; i++) {
        if (isTpm12) {
            result = Tspi_PcrComposite_SelectPcrIndexEx(
                    hPcrs,
                    pcrs[i],
                    TSS_PCRS_DIRECTION_RELEASE);
        } else {
            result = Tspi_PcrComposite_SelectPcrIndex(
                    hPcrs,
                    pcrs[i]);
        }
        VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_PcrComposite_SelectPcrIndex");
    }


    // Set the 20 bytes nonce
    validationData.ulExternalDataLength = 20;
    validationData.rgbExternalData = nonce;


    BYTE* versionInfo = NULL;
    UINT32 versionInfoLen = 0;

    // We can now perform the quote
    if (isTpm12) {
        result = Tspi_TPM_Quote2(
                context -> hTPM,
                hAik,
                FALSE,
                hPcrs,
                &validationData,
                &versionInfoLen,
                &versionInfo);
    } else {
        result = Tspi_TPM_Quote(
                context -> hTPM,
                hAik,
                hPcrs,
                &validationData);
    }
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Quote");



    // Return byes:
    // 			  		32 bits
    // 	-----------------------------------------
    //	|			QUOTE INFO SIZE          	|
    //	-----------------------------------------
    // 	|               	...					|
    // 	|               	...					|
    // 						...
    // 	-----------------------------------------
    // 	|		PCR COMPOSITE HASH SIZE        	|
    //	-----------------------------------------
    // 	|               	...					|
    // 	|               	...					|
    // 						...

    // First retrieve Key Info blob 

    UINT64 offset = 0;


    // Compose the structure
    *quoteResLen = 2 * sizeof (UINT32) + validationData.ulValidationDataLength + validationData.ulDataLength;
    // Allocate space for quoteResLen bytes
    *quoteRes = malloc(*quoteResLen);

    // Load QUOTE SIZE (4 bytes)
    Trspi_LoadBlob_UINT32(
            &offset,
            validationData.ulValidationDataLength,
            *quoteRes);

    Trspi_LoadBlob(
            &offset,
            validationData.ulValidationDataLength,
            *quoteRes,
            validationData.rgbValidationData);

    // Load PCR composite hash len (4 bytes)
    Trspi_LoadBlob_UINT32(
            &offset,
            validationData.ulDataLength,
            *quoteRes);

    Trspi_LoadBlob(
            &offset,
            validationData.ulDataLength,
            *quoteRes,
            validationData.rgbData); // rgbData may contain either TPM_QUOTE_INFO (v1.1) or TPM_QUOTE_INFO2 (v1.2),
    // Both contain NONCE and PCR_VALUES


    // @FIXME: this is done for testing; it may be replaced by a function that reads the
    // value of specific PCRs and writes the result on file.
    char* encodedPcrVal = encodeBase64String(((TPM_QUOTE_INFO*) validationData.rgbData) -> compositeHash.digest, 20);
    FILE* f = fopen("expectedValuePcr.dat", "w");
    fwrite(encodedPcrVal, 1, strlen(encodedPcrVal), f);
    fclose(f);




cleanup:

    Tspi_Context_CloseObject(context -> hContext, hPcrs);
    Tspi_Context_CloseObject(context -> hContext, hAik);


    return outcome;
}

T_OUTCOME
tspi_interface_verify_quote(
        Handle contextHandle,
        BYTE* aikCert,
        size_t aikCertLen,
        BYTE* quote,
        size_t quoteLen,
        BYTE* expectedPCRDigest, // Always 20 bytes since SHA-1
        BYTE* nonce,
        BYTE* tpmQuoteInfo,
        size_t tpmQuoteInfoLen) {
    Context* context = NULL;

    TSS_RESULT result;
    TSS_HKEY hAik;
    TSS_HHASH hHash;

    T_OUTCOME outcome = SUCCESS;

    X509* certificate = NULL;


    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), outcome, "cm_get_context");


    // Get a X509 certificate from bytes
    VERIFY_AND_TERMINATE(!cert_utils_bytes_to_x509_cert(aikCert, aikCertLen, &certificate), outcome, "cert_utils_bytes_to_x509_cert");


    // Load the public part of the AIK
    outcome = load_key_from_certificate(
            context,
            &hAik,
            certificate);
    VERIFY_AND_TERMINATE(!outcome, outcome, "load_key_from_certificate");

    // We need to replace the nonce into the given TPM_QUOTE_INFO or TPM_QUOTE_INFO2 structure 
    // with our current one.
    // Try to get the nonce out. Is contained in externalData field of TPM_QUOTE_INFO and TPM_QUOTE_INFO2
    BYTE* tpm_nonce;
    BYTE* PCRdigestBytes;

    TPM_QUOTE_INFO2* q2 = (TPM_QUOTE_INFO2*) tpmQuoteInfo;
    if (memcmp(q2 -> fixed, "QUT2", 4) == 0) // Check for TPM 1.2 info
    {
        tpm_nonce = q2 -> externalData.nonce;
        PCRdigestBytes = q2 -> infoShort.digestAtRelease.digest;
    } else // Else check for TPM 1.1
    {
        TPM_QUOTE_INFO* q1 = (TPM_QUOTE_INFO*) tpmQuoteInfo;
        if (memcmp(q1 -> fixed, "QUOT", 4) == 0) {
            tpm_nonce = q1 -> externalData.nonce;
            PCRdigestBytes = q1 -> compositeHash.digest;
        } else {
            VERIFY_AND_TERMINATE(TRUE, outcome, "Quote info extraction");
        }
    }

    // (1) Compare PCR value with the expected one
    VERIFY_AND_TERMINATE(memcmp(PCRdigestBytes, expectedPCRDigest, 20) != 0, outcome, "PCR values check");

    // (2) Compare the nonce with the expected one
    VERIFY_AND_TERMINATE(memcmp(nonce, tpm_nonce, 20) != 0, outcome, "Nonce values check");

    // (3) Now we can verify the signature.
    result = Tspi_Context_CreateObject(
            context -> hContext,
            TSS_OBJECT_TYPE_HASH,
            TSS_HASH_SHA1,
            &hHash);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Context_CreateObject");

    // Set the PCR values and compute the hash
    result = Tspi_Hash_UpdateHashValue(
            hHash,
            tpmQuoteInfoLen,
            tpmQuoteInfo);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Hash_UpdateHashValue");

    // Verify the signature on the quote
    result = Tspi_Hash_VerifySignature(
            hHash,
            hAik,
            quoteLen,
            quote);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Hash_VerifySignature");


cleanup:

    Tspi_Context_CloseObject(context -> hContext, hAik);

    return outcome;

}


