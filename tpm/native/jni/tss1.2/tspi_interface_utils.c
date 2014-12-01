/*
 * File name: tspi_interface_utils.c
 * 
 * Purpose: Implementation of tspi_interface utility functions
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 *
 *
 */
#include "tspi_interface.h"

#include <openssl/rsa.h>
#include <openssl/pem.h>
#include <openssl/bn.h>

#include "cert_utils.h"

TSS_RESULT
tspi_utils_load_public_key_blob(
        Context* context,
        TSS_HKEY* hKey,
        int initFlags,
        size_t publicKeyBlobSize,
        BYTE* publicKeyBlob) {

    TSS_RESULT result;

    result = Tspi_Context_CreateObject(
            context -> hContext,
            TSS_OBJECT_TYPE_RSAKEY,
            initFlags,
            hKey);
    if (result != TSS_SUCCESS) {
        print_err(__LINE__, __func__, "Tspi_Context_CreateObject returned %i - %s ", result, Trspi_Error_String(result));
        goto cleanup;
    }

    // Load the public key blob into the created key object
    result = Tspi_SetAttribData(
            *hKey,
            TSS_TSPATTRIB_KEY_BLOB,
            TSS_TSPATTRIB_KEYBLOB_PUBLIC_KEY,
            publicKeyBlobSize,
            publicKeyBlob);
    if (result != TSS_SUCCESS) {
        print_err(__LINE__, __func__, "Tspi_SetAttribData returned %i - %s ", result, Trspi_Error_String(result));
    }

cleanup:

    return result;

}

TSS_RESULT
tspi_utils_read_pcrs(
        Context* context,
        int* pcrs,
        uint8_t pcrsNum,
        TSS_HPCRS hPcrs) {
    TSS_RESULT result = TSS_SUCCESS;
    BYTE* pcrValue = NULL;
    size_t pcrValueLen = 0;
    int j = 0;
    for (; j < pcrsNum; j++) {
        if (pcrs[j] <= 23 && pcrs[j] > 0) {
            result = Tspi_TPM_PcrRead(
                    context -> hTPM,
                    pcrs[j],
                    &pcrValueLen,
                    &pcrValue);
            if (result != TSS_SUCCESS) {
                print_err(__LINE__, __func__, " Tspi_TPM_PcrRead returned %i - %s ", result, Trspi_Error_String(result));
                return result;
            }

            // Set the value to the PCR composite
            result = Tspi_PcrComposite_SetPcrValue(
                    hPcrs,
                    pcrs[j],
                    pcrValueLen,
                    pcrValue);
            if (result != TSS_SUCCESS) {
                print_err(__LINE__, __func__, " Tspi_PcrComposite_SetPcrValue returned %i - %s ", result, Trspi_Error_String(result));
                return result;
            }

        }
    }
    return result;
}

TSS_RESULT
tspi_utils_set_object_policy(
        Context* context,
        TSS_HPOLICY* policy,
        TSS_HOBJECT objectHandle,
        UINT32 secretMode,
        bool assign,
        BYTE* secret,
        uint8_t secretLength) {
    if (context == NULL || objectHandle == 0 || policy == 0) {
        // Return a TSS_E_BAD_PARAMETER since parameters are not
        // correct
        return TSS_E_BAD_PARAMETER;
    }

    TSS_RESULT result = TSS_SUCCESS;

    // Create the policy
    result = Tspi_Context_CreateObject(
            context -> hContext,
            TSS_OBJECT_TYPE_POLICY,
            TSS_POLICY_USAGE,
            policy);
    if (result != TSS_SUCCESS) {
        print_err(__LINE__, __func__, " Tspi_Context_CreateObject returned %i - %s ", result, Trspi_Error_String(result));
        goto cleanup;
    }

    // Set the secret value to be the given one
    result = Tspi_Policy_SetSecret(
            *policy,
            secretMode,
            secretLength,
            secret);
    if (result != TSS_SUCCESS) {
        print_err(__LINE__, __func__, " Tspi_Policy_SetSecret returned %i - %s ", result, Trspi_Error_String(result));
        goto cleanup;
    }

    if (assign) {
        // Set the policy; we can not assign the policy until we have the handle
        result = Tspi_Policy_AssignToObject(
                *policy,
                objectHandle);
        if (result != TSS_SUCCESS) {
            print_err(__LINE__, __func__, " Tspi_Policy_AssignToObject returned %i - %s ", result, Trspi_Error_String(result));
            goto cleanup;
        }
    }

cleanup:

    return result;
}

T_OUTCOME
tspi_utils_public_key_bytes_from_handle(
        Context* context,
        TSS_HKEY keyHandle,
        BYTE** publicKey,
        size_t* publicKeyLen) {

    T_OUTCOME outcome = SUCCESS;
    BYTE* modulus = NULL;
    size_t modulusSize = 0;
    BYTE* exponent = NULL;
    size_t exponentSize = 0;
    RSA* rsa = NULL;
    TSS_RESULT result;

    // Verify handle is not 0
    VERIFY_AND_TERMINATE(keyHandle == 0 || context == NULL, outcome, "Check for valid key handle and context");

    // Now we can extract the public parameters
    result = Tspi_GetAttribData(
            keyHandle,
            TSS_TSPATTRIB_RSAKEY_INFO,
            TSS_TSPATTRIB_KEYINFO_RSA_MODULUS,
            &modulusSize,
            &modulus);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_GetAttribData");

    result = Tspi_GetAttribData(
            keyHandle,
            TSS_TSPATTRIB_RSAKEY_INFO,
            TSS_TSPATTRIB_KEYINFO_RSA_EXPONENT,
            &exponentSize,
            &exponent);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_GetAttribData");

    // Initialize the RSA struct
    rsa = RSA_new();
    rsa -> n = BN_bin2bn(modulus, modulusSize, NULL);
    rsa -> e = BN_bin2bn(exponent, exponentSize, NULL);

    BYTE* pemRsaKey;

    BIO *bio = BIO_new(BIO_s_mem());
    // Write RSA key bytes in PEM format
    // WARNING: in order for Java to later read the key, we need
    // to use PEM_write_bio_RSA_PUBKEY, which uses PEM format.
    // The function PEM_write_bio_RSAPublicKey() outputs in PKCS#1 instead,
    // which creates headaches with Java for  
    PEM_write_bio_RSA_PUBKEY(bio, rsa);
    BIO_flush(bio);
    *publicKeyLen = BIO_pending(bio);
    *publicKey = calloc((*publicKeyLen) + 1, 1); /* Null-terminate */

    // Load the bytes into the given locations
    BIO_read(bio, *publicKey, *publicKeyLen);


cleanup:

    if (bio) {
        // Free the BIO
        BIO_free(bio);
    }
    if (modulus) {
        Tspi_Context_FreeMemory(context -> hContext, modulus);
    }
    if (exponent) {
        Tspi_Context_FreeMemory(context -> hContext, exponent);
    }
    if (rsa) {
        RSA_free(rsa);
    }

    return outcome;

}

/**
 * Utility function that converts a string to HEX
 * 
 * @param string 		The string to convert
 * @param stringlen 	Lenght of the string to convert
 * @param hexStr 		Output HEX string
 */
void
stringToHex(char* string, size_t stringlen, char** hexStr) {

    (*hexStr) = malloc(sizeof (char)*(2 * stringlen + 1));
    int i;
    for (i = 0; i < stringlen; i++) {
        int iByte = string[i];
        iByte &= 0x000000ff;
        char* next;
        int len = snprintf(NULL, 0, "%02x", iByte);

        next = malloc(sizeof (char) *len);

        snprintf(next, len + 1, "%02x", iByte);

        (*hexStr)[i * 2] = next[0];
        (*hexStr)[i * 2 + 1] = next[1];

    }
    (*hexStr)[2 * stringlen] = '\0';
}

T_OUTCOME
tspi_utils_create_EK_credentials(
        Handle contextHandle,
        unsigned char* publicKeyBytes,
        size_t publicKeyBytesLen,
        unsigned char* issuerPrivateKey,
        size_t issuerPrivateKeyLen,
        unsigned char* issuerCertificateBytes,
        size_t issuerCertificateBytesLen,
        unsigned char** createdCertificateBytes,
        size_t* createdCertificateBytesLen) {

    TSS_RESULT result = 0;
    Context* context = NULL;
    T_OUTCOME outcome = SUCCESS;
    BYTE* tpmInfoBlob = NULL;
    size_t tpmInfoBlobLen = 0;

    UINT64 offset = 0;

    // TPM 1.2 only info
    char* vendorID = NULL;
    char* chipVersion = NULL;
    // TPM 1.1 and 1.2 info
    char* tpmVersion = NULL;
    size_t tpmVersionLen = 0;
    BYTE* manifacturer = NULL;
    size_t manifacturerLen = 0;


    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), outcome, "cm_get_context");

    result = Tspi_TPM_GetCapability(
            context -> hTPM,
            TSS_TPMCAP_VERSION_VAL,
            0,
            NULL,
            &tpmInfoBlobLen,
            &tpmInfoBlob);
    // The above should fail if we have a TPM 1.1
    if (result == TPM_E_BAD_MODE) {
        goto tpm11_info; // Extract only TPM 1.1 info
    }
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_TPM_GetCapability");

    TPM_CAP_VERSION_INFO tpm12Info;
    // Safe operation since tpmInfoBlob has been retrieved from Tspi_TPM_GetCapability
    result = Trspi_UnloadBlob_CAP_VERSION_INFO(&offset, tpmInfoBlob, &tpm12Info);


    // Vendor ID is always a string of size 4
    vendorID = malloc(sizeof (char) * (4 + 1));
    vendorID[0] = tpm12Info.tpmVendorID[0];
    vendorID[1] = tpm12Info.tpmVendorID[1];
    vendorID[2] = tpm12Info.tpmVendorID[2];
    vendorID[3] = tpm12Info.tpmVendorID[3];
    vendorID[4] = '\0';

    size_t len = snprintf(NULL, 0, "%i.%i.%i.%i", tpm12Info.version.major, tpm12Info.version.minor,
            tpm12Info.version.revMajor, tpm12Info.version.revMinor);

    // Fail if length is <= 0
    VERIFY_AND_TERMINATE(len <= 0, outcome, "chip version len check");

    chipVersion = malloc(sizeof (char)*len);


    // I assume MODEL is CHIP VERSION  (3.2.9 Credential Profiles v1.2 level 2 - revision 8)
    snprintf(chipVersion, len + 1, "%i.%i.%i.%i", tpm12Info.version.major, tpm12Info.version.minor,
            tpm12Info.version.revMajor, tpm12Info.version.revMinor);


    // Spec level
    // tpm12Info.specLevel;
    // tpm12Info.errataRev;


    print_log(__LINE__, __func__, "TPM 1.2 specific stuff...");
    print_log(__LINE__, __func__, "VENDOR ID: %s", vendorID);
    print_log(__LINE__, __func__, "SPECS LEVEL: %u", tpm12Info.specLevel);
    print_log(__LINE__, __func__, "ERRATA REV: %u", tpm12Info.errataRev);
    print_log(__LINE__, __func__, "CHIP VERSION: %s", chipVersion);

tpm11_info:
    ; // empty statement	

    print_log(__LINE__, __func__, "TPM 1.1 and 1.2 stuff...");

    // Extract TPM version
    // VERSION NUMBER in Section 3.2.9, Credential Profiles v1.2 level 2 - revision 8
    result = Tspi_TPM_GetCapability(
            context -> hTPM,
            TSS_TPMCAP_VERSION,
            0,
            NULL,
            &tpmVersionLen,
            (BYTE**) & tpmVersion);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_TPM_GetCapability");

    result = Tspi_TPM_GetCapability(
            context -> hTPM,
            TSS_TPMCAP_VERSION,
            0,
            NULL,
            &tpmVersionLen,
            (BYTE**) & tpmVersion);


    char* string = NULL;
    stringToHex(tpmVersion, tpmVersionLen, &string);
    print_log(__LINE__, __func__, "VERSION %s", string);
    free(string);

    // MANIFACTURER in Section 3.2.9, Credential Profiles v1.2 level 2 - revision 8

    UINT32 cat = TSS_TPMCAP_PROP_MANUFACTURER;
    BYTE* pSubCap = (BYTE *) & cat;
    Tspi_TPM_GetCapability(
            context -> hTPM,
            TSS_TPMCAP_PROPERTY,
            sizeof (TSS_TPMCAP_PROP_MANUFACTURER),
            pSubCap,
            &manifacturerLen,
            &manifacturer);

    stringToHex(manifacturer, manifacturerLen, &string);
    print_log(__LINE__, __func__, "MANIFACTURER  %s", string);
    free(string);



    // @TODO: Now we can create the credentials...
    //	EKCertData ekCertD;
    // ekCertD.basicConstraints = ;
    // ekCertD.certificatePolicies = ;
    // ekCertD.subjectAltNames	=



    outcome = cert_utils_create_certificate_from_public_key(
            publicKeyBytes,
            publicKeyBytesLen,
            issuerPrivateKey,
            issuerPrivateKeyLen,
            issuerCertificateBytes,
            issuerCertificateBytesLen,
            createdCertificateBytes,
            createdCertificateBytesLen,
            NULL);



cleanup:


    if (vendorID) {
        free(vendorID);
    }
    if (chipVersion) {
        free(chipVersion);
    }
    Tspi_Context_FreeMemory(context -> hContext, manifacturer);
    Tspi_Context_FreeMemory(context -> hContext, tpmVersion);


    return outcome;


}

T_OUTCOME
tspi_utils_hash(
        Handle contextHandle,
        BYTE* dataToHash,
        size_t dataToHashLen,
        BYTE** hashValue,
        size_t* hashValueLen) // TPM 1.2 supports only SHA-1 so this should be always 20
{

    Context* context = NULL;
    T_OUTCOME outcome = SUCCESS;
    TSS_RESULT result;

    // handle to the hash object
    TSS_HHASH hash;



    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), outcome, "cm_get_context");


    result = Tspi_Context_CreateObject(
            context -> hContext,
            TSS_OBJECT_TYPE_HASH,
            TSS_HASH_SHA1,
            &hash);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Context_CreateObject - hash");

    // Compute the hash
    result = Tspi_Hash_UpdateHashValue(
            hash,
            dataToHashLen,
            dataToHash);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Hash_UpdateHashValue");

    // Retrieve the hash value from the TPM
    result = Tspi_Hash_GetHashValue(
            hash,
            hashValueLen,
            hashValue);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Hash_GetHashValue");


cleanup:

    // Free the allocated hash object
    Tspi_Context_CloseObject(context -> hContext, hash);

    return outcome;

}
