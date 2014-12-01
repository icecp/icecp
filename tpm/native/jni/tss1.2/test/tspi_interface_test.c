#include <stdio.h>

#include "../log.h"
#include "../boolean.h"

#include "../context_manager.h"

#include "../tspi_interface.h"

#include "../cert_utils.h"

#include <stdarg.h>
#include <string.h>
#include <stdlib.h>

#include <math.h>

#include <openssl/bio.h>
#include <openssl/evp.h>


#define LOG_AND_QUIT(message) {print_err(__LINE__, __func__, message);  exit(-1);}


// Holds the handle
Handle handle = 0;

T_OUTCOME
tspi_interface_create_tpm_context_test() {
    if (!tspi_interface_create_tpm_context(&handle)) {
        LOG_AND_QUIT("Create context");
    }
}

T_OUTCOME
tspi_interface_delete_tpm_context_test() {
    if (!cm_delete_context(handle)) {
        LOG_AND_QUIT("Unable to close the handle");
    }
}

int
readFileBytes(const char *name, BYTE** fileBytes, size_t* fileBytesLen) {
    FILE *fl = fopen(name, "r");
    fseek(fl, 0, SEEK_END);
    (*fileBytesLen) = ftell(fl);
    *fileBytes = malloc(*fileBytesLen);
    fseek(fl, 0, SEEK_SET);
    fread(*fileBytes, 1, *fileBytesLen, fl);
    fclose(fl);
    return 1;
}

T_OUTCOME
tspi_utils_create_EK_credentials_test() {

    BYTE* credentials = NULL;
    size_t credentialsLen = 0;

    BYTE* publicKeyBytes = NULL;
    size_t publicKeyBytesLen = 0;
    BYTE* issuerPrivateKey = NULL;
    size_t issuerPrivateKeyLen = 0;
    BYTE* issuerCertificateBytes = NULL;
    size_t issuerCertificateBytesLen = 0;

    // Read EK pub key
    readFileBytes("cert/ek.pub", &publicKeyBytes, &publicKeyBytesLen);
    // Read issuer's private key
    readFileBytes("cert/ca_prv_key.key", &issuerPrivateKey, &issuerPrivateKeyLen);
    // Read issuer's certificate
    readFileBytes("cert/new_ca.chain", &issuerCertificateBytes, &issuerCertificateBytesLen);

    T_OUTCOME res = tspi_utils_create_EK_credentials(handle,
            publicKeyBytes,
            publicKeyBytesLen,
            issuerPrivateKey,
            issuerPrivateKeyLen,
            issuerCertificateBytes,
            issuerCertificateBytesLen,
            &credentials, &credentialsLen);


    if (res == SUCCESS) {
        FILE* createdCred = fopen("ek_cert.chain", "w");
        fwrite(credentials, 1, credentialsLen, createdCred);
        fclose(createdCred);
    }

    free(publicKeyBytes);
    free(issuerPrivateKey);
    free(issuerCertificateBytes);

    return res;

}

int
tspi_hash_test() {
    unsigned char* testString = "Test string to hash";
    unsigned char* expectedRes = "jGGVJNn65jWXl+1FrpXdV/R5tFI=";

    BYTE* hashVaue = NULL;
    size_t hashVaueLen = 0;

    int result = 1;

    T_OUTCOME res = tspi_utils_hash(
            handle,
            (BYTE*) testString,
            strlen(testString),
            &hashVaue,
            &hashVaueLen);


    if (res != SUCCESS || hashVaueLen != 20) {
        print_err(__LINE__, __func__, "Error in computing the Hash");
        return 0;
    }


    char* hashString = encodeBase64String(hashVaue, hashVaueLen);


    print_log(__LINE__, __func__, "The hash value is (in Base64): %s", hashString);


    // Compare the HASH with an expected one
    result = !strcmp(expectedRes, hashString);
    if (!result) {
        print_err(__LINE__, __func__, "Error, wrong hash value");
    }

cleanup:

    free(hashString);
    free(hashVaue);

    return result;


}

int
tspi_interface_pcr_quote_test() {

    BYTE* nonce = NULL;

    if (!tspi_interface_get_random_bytes(handle, 20, &nonce)) {
        return FALSE;
    }

    int pcrs[] = {1, 5, 7};

    BYTE* aikBytes = NULL;
    size_t aikBytesLen = 0;
    BYTE* quoteRes = NULL;
    size_t quoteResLen = 0;

    // Read AIK enc key
    readFileBytes("aik/aikKey.enckey", &aikBytes, &aikBytesLen);

    BYTE* keyBytes;
    size_t keyBytesLen;
    TSS_HKEY newKeyHandle;




    // We need the SRK
    BYTE srkSecretBytes[20];
    memset(srkSecretBytes, 0, 20);


    T_OUTCOME res = tspi_interface_load_SRK(
            handle,
            srkSecretBytes,
            20);

    res = tspi_interface_create_key(
            handle,
            TSS_KEY_TYPE_SIGNING,
            TSS_KEY_SIZE_2048,
            TSS_KEY_NOT_MIGRATABLE,
            TSS_KEY_AUTHORIZATION,
            TSS_KEY_VOLATILE,
            0,
            FALSE,
            "secret",
            6,
            NULL,
            NULL,
            &keyBytes,
            &keyBytesLen,
            &newKeyHandle);


    res = tspi_interface_pcr_quote(
            handle,
            keyBytes,
            keyBytesLen,
            pcrs,
            3,
            nonce,
            &quoteRes,
            &quoteResLen);


    if (res == SUCCESS) {
        print_log(__LINE__, __func__, "Quote res size: %i", quoteResLen);
    } else {
        return -1;
    }

    // Cleaning

    if (aikBytes != NULL) {
        free(aikBytes);
    }


    // ***** QUOTE VERIFICATION

    // Now verify the quote
    BYTE* aikCred = NULL;
    size_t aikCredLen = 0;

    readFileBytes("aik/aik_cred.chain", &aikCred, &aikCredLen);


    // To make this work, we extract the values from the previous respnse
    // In order to obtain configuration values, one may use tspi_interface_pcr_quote
    // with a random key, and save the result to file for later use (CHECK THE ENCODING).
    BYTE* expectedPCRDigest = NULL;
    BYTE* quoteVal = NULL;
    size_t quoteValLen = 0;
    BYTE* quoteInfo = NULL;
    size_t quoteInfoLen = 0;


    UINT64 offset = 0;

    Trspi_UnloadBlob_UINT32(
            &offset,
            &quoteValLen,
            quoteRes);

    quoteVal = malloc(quoteValLen);

    Trspi_UnloadBlob(
            &offset,
            quoteValLen,
            quoteRes,
            quoteVal);

    Trspi_UnloadBlob_UINT32(
            &offset,
            &quoteInfoLen,
            quoteRes);

    quoteInfo = malloc(quoteInfoLen);

    Trspi_UnloadBlob(
            &offset,
            quoteInfoLen,
            quoteRes,
            quoteInfo);



    expectedPCRDigest = ((TPM_QUOTE_INFO*) quoteInfo) -> compositeHash.digest;



    res = tspi_interface_verify_quote(
            handle,
            aikCred,
            aikCredLen,
            quoteVal,
            quoteValLen,
            expectedPCRDigest, // Always 20 bytes, because SHA-1
            nonce,
            quoteInfo,
            quoteInfoLen);

    if (!res) {
        print_err(__LINE__, __func__, "Verification failed!");
    }


    // Cleaning...
    free(quoteVal);
    free(quoteInfo);


    return res;

}

T_OUTCOME
tspi_interface_create_key_test() {

    BYTE keySecret[4] = {'1', '1', '1', '1'};
    size_t keySecretLen = 4;


    BYTE* uuidBytes = NULL;
    size_t uuidBytesLen = 0;

    BYTE* keyBytes = NULL;
    size_t keyBytesLen = 0;

    TSS_HKEY newKeyHandle = 0;


    // We need the SRK
    BYTE srkSecretBytes[20];
    memset(srkSecretBytes, 0, 20);


    T_OUTCOME res = tspi_interface_load_SRK(
            handle,
            srkSecretBytes,
            20);

    // Load the registered keys

    res = tspi_interface_load_registered_keys(
            handle,
            TSS_PS_TYPE_USER);

    if (!res) {
        print_err(__LINE__, __func__, "tspi_interface_load_registered_keys failed!");
        return ERROR;
    }


    // Call actual create key function
    res = tspi_interface_create_key(
            handle,
            TSS_KEY_TYPE_SIGNING,
            TSS_KEY_SIZE_2048,
            TSS_KEY_NOT_MIGRATABLE,
            TSS_KEY_AUTHORIZATION,
            TSS_KEY_NON_VOLATILE,
            TSS_PS_TYPE_USER,
            FALSE,
            keySecret,
            keySecretLen,
            &uuidBytes, // out
            &uuidBytesLen, // out
            NULL, // out
            0, // out
            NULL); // No handle needs to be saved

    if (!res) {
        print_err(__LINE__, __func__, "tspi_interface_create_key failed!");
        return ERROR;
    }

    BYTE* publicKeyBytes;
    size_t publicKeyBytesLen;

    res = tspi_interface_get_public_key(
            handle,
            3,
            uuidBytes,
            &publicKeyBytes,
            &publicKeyBytesLen);
    if (!res) {
        print_err(__LINE__, __func__, "tspi_interface_get_public_key failed!");
        return ERROR;
    }


    res = tspi_interface_unregister_key(
            handle,
            uuidBytes,
            TSS_PS_TYPE_USER);
    if (!res) {
        print_err(__LINE__, __func__, "tspi_interface_unregister_key failed!");
        return ERROR;
    }



    // Call actual create key function
    res = tspi_interface_create_key(
            handle,
            TSS_KEY_TYPE_SIGNING,
            TSS_KEY_SIZE_2048,
            TSS_KEY_NOT_MIGRATABLE,
            TSS_KEY_AUTHORIZATION,
            TSS_KEY_VOLATILE, // It SHOULD NOT create a UUID, nor register the key
            TSS_PS_TYPE_USER,
            FALSE,
            keySecret,
            keySecretLen,
            NULL, // out
            0, // out
            &keyBytes, // out
            &keyBytesLen, // out
            NULL); // No handle needs to be saved

    if (!res) {
        print_err(__LINE__, __func__, "tspi_interface_create_key failed!");
        return ERROR;
    }

    return SUCCESS;


}

int
main(int argc, const char** argv) {
    tspi_interface_create_tpm_context_test();
    print_log(__LINE__, __func__, "Handle is %u", handle);


    // if (!tspi_utils_create_EK_credentials_test())
    // {
    // 	goto cleanup;
    // }


    if (!tspi_hash_test()) {
        goto cleanup;
    }


    // if (!tspi_interface_pcr_quote_test())
    // {
    // 	goto cleanup;
    // }


    // tspi_interface_create_key_test();


cleanup:

    tspi_interface_delete_tpm_context_test(handle);


}
