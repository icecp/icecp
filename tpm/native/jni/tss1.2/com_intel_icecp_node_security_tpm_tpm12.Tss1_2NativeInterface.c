/*
 * File name: com_intel_icecp_node_security_tpm_tpm12.Tss1_2NativeInterface.c
 * 
 * Purpose: Implementation of the native interface methods callable from Java
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 *
 *
 */
//Basic includes look like this:
#include <stdio.h>
#include <string.h>
#include <stdarg.h>


#include "com_intel_icecp_node_security_tpm_tpm12_Tss1_2NativeInterface.h"
#include "tspi_interface.h"

#include <jni.h>

#include <trousers/trousers.h>


#include <openssl/bio.h>
#include <openssl/rsa.h>
#include <openssl/pem.h>

#include "boolean.h"
#include "cert_utils.h"
#include "outcome.h"



#define  JAVA_TPM_EXCEPTION "com/intel/icecp/core/security/tpm/exception/TpmOperationError"

T_OUTCOME
extractIntArray(
        JNIEnv *env,
        jintArray jArray,
        size_t* arraySize,
        int** array) {
    if (jArray == NULL) {
        return ERROR;
    }

    // Get byte* from jbyteArray
    *arraySize = (*env) -> GetArrayLength(env, jArray);
    *array = (*env) -> GetIntArrayElements(env, jArray, 0);
    return SUCCESS;
}

T_OUTCOME
releaseIntArray(
        JNIEnv *env,
        jintArray jArray,
        int* array) {
    if (jArray == NULL || array == NULL) {
        return ERROR;
    }

    (*env) -> ReleaseIntArrayElements(env, jArray, array, 0);
    return SUCCESS;
}

T_OUTCOME
extractByteArray(
        JNIEnv *env,
        jbyteArray jArray,
        size_t* arraySize,
        BYTE** array) {
    if (jArray == NULL) {
        return ERROR;
    }

    // Get byte* from jbyteArray
    *arraySize = (*env) -> GetArrayLength(env, jArray);
    *array = (*env) -> GetByteArrayElements(env, jArray, 0);
    return SUCCESS;
}

T_OUTCOME
releaseByteArray(
        JNIEnv *env,
        jbyteArray jArray,
        BYTE* array) {
    if (jArray == NULL || array == NULL) {
        return ERROR;
    }

    (*env) -> ReleaseByteArrayElements(env, jArray, array, 0);
    return SUCCESS;
}

/**
 * Throws an exception for the Java code, of type JAVA_TPM_EXCEPTION
 * 
 * @param env 		JNI environment
 * @param message 	Template of the message to output
 */
void
throwException(
        JNIEnv* env,
        const char* message, ...) {

    char* buff;
    va_list args;
    va_start(args, message);

    // Determine the size of the buffer
    int size = snprintf(NULL, 0, message, args);
    // If size is 0, return 
    if (!size)
        return;
    // Allocate buffer
    buff = (char*) malloc(sizeof (char)*size + 1);
    // Compose message string
    vsprintf(buff, message, args);

    jclass exceptionClass = (*env) -> FindClass(
            env,
            JAVA_TPM_EXCEPTION);

    if (exceptionClass) {
        (*env) -> ThrowNew(
                env,
                exceptionClass,
                buff);
    }

    free(buff);

    va_end(args);

}

JNIEXPORT jint JNICALL
JNICALL Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_createTpmContext(
        JNIEnv *env,
        jobject obj) {

    Handle contextHandle;
    T_OUTCOME result;
    result = tspi_interface_create_tpm_context(&contextHandle);

    if (result == SUCCESS) {
        return contextHandle;
    } else {
        // if no context has been created, we throw an exception
        throwException(env, " tspi_interface_create_tpm_context failed.");
        return -1;
    }

}

JNIEXPORT void JNICALL
Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_loadRegisteredKeys(
        JNIEnv *env,
        jobject obj,
        jint handle) {
    if (tspi_interface_load_registered_keys(handle, TSS_PS_TYPE_USER)) {
        throwException(env, "Unable to load the registered keys");
    }

}

JNIEXPORT void JNICALL
JNICALL Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_deleteTpmContext(
        JNIEnv *env,
        jobject obj,
        jint handle) {

    if (!cm_delete_context(handle)) {
        // if no context has been created, we throw an exception
        throwException(env, "Unable to delete context %i.", handle);
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_createOwnerDelegation(
        JNIEnv *env,
        jobject obj,
        jint contextHandle) {


    BYTE* delegationBlob = NULL;
    size_t delegationBlobLen = 0;


    T_OUTCOME res = tspi_interface_create_owner_delagation(
            contextHandle,
            &delegationBlob,
            &delegationBlobLen);


    // res = tspi_interface_load_owner_delagation(
    // 	contextHandle,
    // 	delegationBlob,
    // 	delegationBlobLen);


    // if (res == SUCCESS)
    // {
    // 	jbyteArray ret = (*env) -> NewByteArray(env, delegationBlobLen);
    //     (*env) -> SetByteArrayRegion (env, ret, 0, delegationBlobLen, delegationBlob);
    //     return ret;	
    // }
    // throwException(env, "Failed to create owner delegation.");
    return NULL;
}

JNIEXPORT void JNICALL
JNICALL Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_loadSrk(
        JNIEnv* env,
        jobject obj,
        jint jhandle,
        jbyteArray jSecret) {

    size_t srkSecretBytesLen = 0;
    BYTE* srkSecretBytes = NULL;

    if (jSecret != NULL) {
        if (!extractByteArray(env, jSecret, &srkSecretBytesLen, &srkSecretBytes)) {
            throwException(env, "Invalid or NULL SRK secret provided.");
            return;
        }
    } else {
        // Well known secret: all bytes to 0
        srkSecretBytes = (BYTE*) malloc(sizeof (BYTE) * 20);
        memset(srkSecretBytes, 0, 20);
        srkSecretBytesLen = 20;
    }

    // Load SRK
    T_OUTCOME res = tspi_interface_load_SRK(
            jhandle,
            srkSecretBytes,
            srkSecretBytesLen);

    // cleanup

    if (jSecret) {
        releaseByteArray(env, jSecret, srkSecretBytes);
    } else {
        free(srkSecretBytes);
    }

    if (res != SUCCESS) {
        throwException(env, "Unable to load SRK");
    }

}

JNIEXPORT jbyteArray
JNICALL Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_calculateHash(
        JNIEnv* env,
        jobject obj,
        jint contextHandle,
        jbyteArray jData) {

    BYTE* dataToHash = NULL;
    size_t dataToHashLen = 0;

    if (!extractByteArray(env, jData, &dataToHashLen, &dataToHash)) {
        throwException(env, "NULL or invalid input data");
        return NULL;
    }


    BYTE* resultHash = NULL;
    size_t resultHashLen = 0;


    T_OUTCOME res = tspi_utils_hash(
            contextHandle,
            dataToHash,
            dataToHashLen,
            &resultHash,
            &resultHashLen);

    // cleanup

    releaseByteArray(env, jData, dataToHash);

    if (res == SUCCESS && resultHashLen > 0) {
        jbyteArray ret = (*env) -> NewByteArray(env, resultHashLen);
        (*env) -> SetByteArrayRegion(env, ret, 0, resultHashLen, resultHash);
        return ret;
    } else {
        throwException(env, "Unable to compute the hash of the given data.");
        return NULL;
    }

}

JNIEXPORT void JNICALL
JNICALL Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_unregisterKey(
        JNIEnv* env,
        jobject obj,
        jint contextHandle,
        jbyteArray jUuidBytes,
        jint persistentStorageType) {

    BYTE* uuidByte = NULL;
    size_t uuidByteLen = 0;

    if (!extractByteArray(env, jUuidBytes, &uuidByteLen, &uuidByte)) {
        throwException(env, "Invalid or NULL UUID");
        return;
    }

    T_OUTCOME res = tspi_interface_unregister_key(contextHandle, uuidByte, persistentStorageType);

    releaseByteArray(env, jUuidBytes, uuidByte);

    if (res != SUCCESS) {
        throwException(env, "Unable to unregister the key");
    }
}

JNIEXPORT jbyteArray JNICALL
JNICALL Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_createKey(
        JNIEnv* env,
        jobject obj,
        jint handle,
        jint keyType,
        jint keySize,
        jint keyMigratable,
        jint keyAuthorization,
        jint keyVolatile,
        jint persistentStorageType,
        jboolean load,
        jbyteArray jKeySecret) {


    size_t keySecretBytesLen = 0;
    BYTE* keySecretBytes = NULL;
    extractByteArray(env, jKeySecret, &keySecretBytesLen, &keySecretBytes);


    // output

    BYTE* uuidBytes = NULL;
    size_t uuidBytesLen = 0;
    BYTE* keyBytes = NULL;
    size_t keyBytesLen = 0;


    // Call actual create key function
    T_OUTCOME res = tspi_interface_create_key(
            handle,
            keyType,
            keySize,
            keyMigratable,
            keyAuthorization,
            keyVolatile,
            persistentStorageType,
            load,
            keySecretBytes,
            keySecretBytesLen,
            &uuidBytes, // out
            &uuidBytesLen, // out
            &keyBytes, // out
            &keyBytesLen, // out
            NULL); // No handle needs to be saved when called from Java



    // cleanup...

    // release the bytes allocated for key secret and filename
    releaseByteArray(env, jKeySecret, keySecretBytes);


    if (res == SUCCESS) {

        jbyteArray ret = NULL;

        if (uuidBytes != NULL && uuidBytesLen > 0) {
            ret = (*env) -> NewByteArray(env, uuidBytesLen);
            (*env) -> SetByteArrayRegion(env, ret, 0, uuidBytesLen, uuidBytes);
        } else // Return the key bytes and not the UUID
        {
            ret = (*env) -> NewByteArray(env, keyBytesLen);
            (*env) -> SetByteArrayRegion(env, ret, 0, keyBytesLen, keyBytes);
        }

        return ret;

    } else {
        throwException(env, "Unable to complete key creation");
    }
}

JNIEXPORT void JNICALL
JNICALL Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_setTpmOwnerPriviledges(
        JNIEnv *env,
        jobject obj,
        jint contextHandle,
        jbyteArray jOwnerSecretBytes) {
    // Can be null
    size_t ownerSecretBytesLen = 0;
    BYTE* ownerSecretBytes = NULL;
    extractByteArray(env, jOwnerSecretBytes, &ownerSecretBytesLen, &ownerSecretBytes);


    T_OUTCOME res = tspi_set_tpm_owner_privileges(
            contextHandle,
            ownerSecretBytes,
            ownerSecretBytesLen);

    releaseByteArray(env, jOwnerSecretBytes, ownerSecretBytes);

    if (res == ERROR) {
        throwException(env, "Unable to set owner privileges");
    }

}

JNIEXPORT jbyteArray JNICALL
Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_getPEMPublicKeyByUUID(
        JNIEnv* env,
        jclass obj,
        jint contextId,
        jbyteArray jUuidBytes) {

    BYTE* pubKey = NULL;
    size_t pubKeyLen = 0;


    BYTE* uuidByte = NULL;
    size_t uuidByteLen = 0;

    if (!extractByteArray(env, jUuidBytes, &uuidByteLen, &uuidByte)) {
        throwException(env, "Invalid or NULL UUID");
        return NULL;
    }


    T_OUTCOME res = tspi_interface_get_public_key(
            contextId,
            3,
            uuidByte,
            &pubKey,
            &pubKeyLen);

    releaseByteArray(env, jUuidBytes, uuidByte);

    // In case of SUCCESS and not empty request
    if (res != ERROR && pubKeyLen > 0) {
        jbyteArray ret = (*env) -> NewByteArray(env, pubKeyLen);
        (*env) -> SetByteArrayRegion(env, ret, 0, pubKeyLen, pubKey);
        return ret;
    } else {
        throwException(env, "Unable to retrieve the public key from the given UUID");
        return NULL;
    }

}

JNIEXPORT jbyteArray JNICALL
Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_getPEMEKPublicKey(
        JNIEnv* env,
        jclass obj,
        jint contextId) {

    BYTE* pubKey = NULL;
    size_t pubKeyLen = 0;

    T_OUTCOME res = tspi_interface_get_public_key(
            contextId,
            TPM_EK_KEY_ID, // 
            NULL, // NULL uuid bytes
            &pubKey,
            &pubKeyLen);

    // In case of SUCCESS and not empty request
    if (res != ERROR && pubKeyLen > 0) {
        jbyteArray ret = (*env) -> NewByteArray(env, pubKeyLen);
        (*env) -> SetByteArrayRegion(env, ret, 0, pubKeyLen, pubKey);
        return ret;
    } else {
        throwException(env, "Unable to retrieve EK's public key");
        return NULL;
    }

}

JNIEXPORT jbyteArray JNICALL
Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_extractPEMPublicKeyFromTPMPubKeyBlob(
        JNIEnv* env,
        jclass obj,
        jint contextId,
        jbyteArray jPublicKeyBlob) {

    BYTE* pubKey = NULL;
    size_t pubKeyLen = 0;
    BYTE* publicKeyBlob = NULL;
    size_t publicKeyBlobLen = 0;

    T_OUTCOME res;

    // If we have bytes, we extract the public key from them
    if (!extractByteArray(env, jPublicKeyBlob, &publicKeyBlobLen, &publicKeyBlob)) {
        throwException(env, "Unable to retrieve the public key. Invalid PUBLIC_KEY blob");
    }

    res = tspi_interface_get_public_key_from_public_key_blob(
            contextId,
            publicKeyBlob,
            publicKeyBlobLen,
            &pubKey,
            &pubKeyLen);

    releaseByteArray(env, jPublicKeyBlob, publicKeyBlob);


    // In case of SUCCESS and not empty request
    if (res != ERROR && pubKeyLen > 0) {
        jbyteArray ret = (*env) -> NewByteArray(env, pubKeyLen);
        (*env) -> SetByteArrayRegion(env, ret, 0, pubKeyLen, pubKey);
        return ret;
    } else {
        throwException(env, "Unable to retrieve the public key from the given PUBLIC_KEY blob.");
        return NULL;
    }

}

JNIEXPORT jbyteArray JNICALL
Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_extractPEMPublicKeyFromTPMKeyBlob(
        JNIEnv* env,
        jclass obj,
        jint contextId,
        jbyteArray jKeyBlob) {

    BYTE* pubKey = NULL;
    size_t pubKeyLen = 0;
    BYTE* keyBlob = NULL;
    size_t keyBlobLen = 0;

    T_OUTCOME res;

    if (!extractByteArray(env, jKeyBlob, &keyBlobLen, &keyBlob)) {
        throwException(env, "Unable to retrieve the public key. Invalid KEY_BLOB bytes");
    }

    res = tspi_interface_get_public_key_from_key_blob(
            contextId,
            keyBlob,
            keyBlobLen,
            &pubKey,
            &pubKeyLen);

    releaseByteArray(env, jKeyBlob, keyBlob);


    // In case of SUCCESS and not empty request
    if (res != ERROR && pubKeyLen > 0) {
        jbyteArray ret = (*env) -> NewByteArray(env, pubKeyLen);
        (*env) -> SetByteArrayRegion(env, ret, 0, pubKeyLen, pubKey);
        return ret;
    } else {
        throwException(env, "Unable to retrieve the public key from the given KEY_BLOB bytes.");
        return NULL;
    }

}

JNIEXPORT void JNICALL
JNICALL Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_flushTpmOwnerPriviledges(
        JNIEnv *env,
        jobject obj,
        jint contextId) {
    // Simply set an empty secret for the owner
    if (tspi_flush_tpm_owner_privileges(contextId) == ERROR) {
        throwException(env, "Unable to flush owner privileges");
    }
}

JNIEXPORT jbyteArray
JNICALL JNICALL Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_sealData(
        JNIEnv* env,
        jobject obj,
        jint contextId,
        jbyteArray jKeySecret,
        jintArray jPcrRegisters,
        jbyteArray jData) {

    // Get PCRs (can be NULL)
    size_t pcrsNum = 0;
    int* pcrs = NULL;
    size_t dataLen = 0;
    BYTE* dataToSeal = NULL;

    extractIntArray(env, jPcrRegisters, &pcrsNum, &pcrs);
    // Data to seal can not be null!
    if (!extractByteArray(env, jData, &dataLen, &dataToSeal)) {
        throwException(env, "NULL data provided.");
        return NULL;
    }

    // Sealing key secret (can be NULL)
    size_t sealingKeySecretLen = 0;
    BYTE* sealingKeySecret = NULL;
    extractByteArray(env, jKeySecret, &sealingKeySecretLen, &sealingKeySecret);


    BYTE* sealedData = NULL;
    size_t sealedDataLen = 0;

    T_OUTCOME res = tspi_interface_seal_data(
            contextId,
            sealingKeySecret,
            sealingKeySecretLen,
            pcrs,
            pcrsNum,
            dataToSeal,
            dataLen,
            &sealedData,
            &sealedDataLen);

    // Release the bytes used
    releaseByteArray(env, jData, dataToSeal);
    releaseByteArray(env, jKeySecret, sealingKeySecret);
    releaseIntArray(env, jPcrRegisters, pcrs);

    // Either return the encrypted bytes or throw an exc.
    if (res == SUCCESS && sealedData != NULL) {
        jbyteArray ret = (*env) -> NewByteArray(env, sealedDataLen);
        (*env) -> SetByteArrayRegion(env, ret, 0, sealedDataLen, sealedData);
        return ret;
    } else {
        throwException(env, "Unable to seal the given data, tspi_interface_seal_data failed.");
        return NULL;
    }
}

JNIEXPORT jbyteArray JNICALL JNICALL Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_unsealData(
        JNIEnv* env,
        jobject obj,
        jint contextId,
        jbyteArray jKeySecret,
        jbyteArray jSealedData,
        jbyteArray jSealingKeyBlob) {

    // Extract sealed data from jbyteArray
    size_t dataToUnsealLen = 0;
    BYTE* dataToUnseal = NULL;
    size_t sealingKeyBlobLen = 0;
    BYTE* sealingKeyBlob = NULL;
    size_t sealingKeySecretLen = 0;
    BYTE* sealingKeySecret = NULL;

    if (!extractByteArray(env, jSealedData, &dataToUnsealLen, &dataToUnseal)) {
        throwException(env, "Data to unseal can not be NULL");
        return NULL;
    }
    // Extract the sealing key specs
    if (!extractByteArray(env, jSealingKeyBlob, &sealingKeyBlobLen, &sealingKeyBlob)) {
        throwException(env, "Sealing key specs can not be NULL");
        return NULL;
    }
    // Extract the key secret (if not null)
    extractByteArray(env, jKeySecret, &sealingKeySecretLen, &sealingKeySecret);


    // Here goes the result
    size_t unsealedDataLen = 0;
    BYTE* unsealedData = NULL;

    T_OUTCOME res = tspi_interface_unseal_data(
            contextId,
            sealingKeySecret,
            sealingKeySecretLen,
            dataToUnseal,
            dataToUnsealLen,
            sealingKeyBlob,
            sealingKeyBlobLen,
            &unsealedData,
            &unsealedDataLen);


    // Cleanup...

    releaseByteArray(env, jSealedData, dataToUnseal);
    releaseByteArray(env, jSealingKeyBlob, sealingKeyBlob);
    releaseByteArray(env, jKeySecret, sealingKeySecret);

    if (res == SUCCESS && unsealedData != NULL) {
        jbyteArray jUnsealedDataBytes = (*env) -> NewByteArray(env, unsealedDataLen);
        (*env) -> SetByteArrayRegion(env, jUnsealedDataBytes, 0, unsealedDataLen, unsealedData);
        return jUnsealedDataBytes;
    } else {
        throwException(env, "Unable to unseal the given data. Function tspi_interface_unseal_data failed.");
        return NULL;
    }
}

JNIEXPORT jbyteArray JNICALL Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_pcrExtend(
        JNIEnv* env,
        jobject obj,
        jint handle,
        jint jPcr,
        jbyteArray jPcrExtendData) {

    size_t dataLen = 0;
    BYTE* data = NULL;
    if (!extractByteArray(env, jPcrExtendData, &dataLen, &data)) {
        throwException(env, "Data to use to extend the PCR value can not be NULL");
        return NULL;
    }

    size_t currentReadLen = 0;
    BYTE* currentRead = NULL;

    T_OUTCOME res = tspi_interface_extend_pcr(
            handle,
            jPcr,
            data,
            dataLen,
            &currentRead,
            &currentReadLen);

    // Cleanup...

    releaseByteArray(env, jPcrExtendData, data);

    if (res == SUCCESS) {
        jbyteArray ret = (*env) -> NewByteArray(env, currentReadLen);
        (*env) -> SetByteArrayRegion(env, ret, 0, currentReadLen, currentRead);
        return ret;
    } else {
        throwException(env, "Unable to extend the given PCR value.");
        return NULL;
    }

}

JNIEXPORT jbyteArray JNICALL
Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_getRandomBytes(
        JNIEnv* env,
        jobject obj,
        jint contextId,
        jint bytesNum) {
    if (bytesNum <= 0) {
        throwException(env, "Invalid bytes number specified: %i", bytesNum);
        return NULL;
    }


    BYTE* generatedBytes;


    T_OUTCOME res = tspi_interface_get_random_bytes(
            contextId,
            bytesNum,
            &generatedBytes);

    if (res == SUCCESS) {
        jbyteArray ret = (*env) -> NewByteArray(env, bytesNum);
        (*env) -> SetByteArrayRegion(env, ret, 0, bytesNum, generatedBytes);
        return ret;
    } else {
        throwException(env, "Error in generating random bytes.");
        return NULL;
    }

}

/********************************************* ATTESTATION *********************************************/



JNIEXPORT jbyteArray JNICALL
JNICALL Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_generateIdentityRequest(
        JNIEnv* env,
        jobject obj,
        jint contextId,
        jbyteArray jAikSecret,
        jbyteArray jCAx509CertBytes,
        jbyteArray jAikLabel) {

    size_t caX509CertificateBytesLen = 0;
    BYTE* caX509CertificateBytes = NULL;
    X509* caX509Certificate = NULL;
    size_t aikSecretLength = 0;
    BYTE* aikSecretArray = NULL;
    size_t aikLabelLen = 0;
    BYTE* aikLabel = NULL;

    // Certificate field is not optional
    if (!extractByteArray(env, jCAx509CertBytes, &caX509CertificateBytesLen, &caX509CertificateBytes)) {
        throwException(env, "NULL CA certificate provided.");
        return NULL;
    }
    // Get the X509 certificate
    if (!cert_utils_bytes_to_x509_cert(caX509CertificateBytes, caX509CertificateBytesLen, &caX509Certificate)) {
        throwException(env, "Unable to read the given CA certificate in X509 format.");
        return NULL;
    }

    // Get AIK secret (can be NULL)
    extractByteArray(env, jAikSecret, &aikSecretLength, &aikSecretArray);

    // Get the label bytes
    if (!extractByteArray(env, jAikLabel, &aikLabelLen, &aikLabel)) {
        throwException(env, "NULL AIK label provided.");
        return NULL;
    }


    // Results holder
    size_t identityRequestLength = 0;
    BYTE* identityRequest = NULL;

    // invoke creation of an AKI request
    T_OUTCOME res = tspi_interface_create_identity_request(
            (Handle) contextId,
            aikSecretArray,
            aikSecretLength,
            caX509Certificate,
            aikLabel,
            aikLabelLen,
            &identityRequest,
            &identityRequestLength);

    // Cleanup...

    // Release the byte arrays and the X509 cert
    releaseByteArray(env, jAikSecret, aikSecretArray);
    releaseByteArray(env, jAikLabel, aikLabel);
    releaseByteArray(env, jCAx509CertBytes, caX509CertificateBytes);
    X509_free(caX509Certificate);

    // In case of SUCCESS and not empty request
    if (res == SUCCESS && identityRequestLength > 0) {
        jbyteArray ret = (*env) -> NewByteArray(env, identityRequestLength);
        (*env) -> SetByteArrayRegion(env, ret, 0, identityRequestLength, identityRequest);
        return ret;
    } else {
        throwException(env, "Unable to complete AIK request.");
        return NULL;
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_verifyIdentityRequestBinding(
        JNIEnv* env,
        jclass obj,
        jint handle,
        jbyteArray jAikRequest,
        jbyteArray jPrivCABytes,
        jbyteArray jPubCABytes) {
    size_t cacertLen = 0;
    BYTE* cert = NULL;
    X509* x509Cert = NULL;
    size_t aikRequestLen = 0;
    BYTE* aikRequest = NULL;
    size_t privCAKeyBytesLen = 0;
    BYTE* privCAKeyBytes = NULL;

    // Certificate field is not optional
    if (!extractByteArray(env, jPubCABytes, &cacertLen, &cert)) {
        throwException(env, "NULL certificate provided.");
        return NULL;
    }
    // Get the X509 certificate from bytes
    if (!cert_utils_bytes_to_x509_cert(cert, cacertLen, &x509Cert)) {
        throwException(env, "Unable to read the given CA certificate in X509 format.");
        return NULL;
    }
    if (!extractByteArray(env, jAikRequest, &aikRequestLen, &aikRequest)) {
        throwException(env, "Identity request can not be NULL.");
        return NULL;
    }
    if (!extractByteArray(env, jPrivCABytes, &privCAKeyBytesLen, &privCAKeyBytes)) {
        throwException(env, "Privacy CA private key can not be null.");
        return NULL;
    }


    size_t aikResLen = 0;
    BYTE* aikRes = NULL;


    T_OUTCOME res = tspi_interface_validate_identity_binding(
            handle,
            aikRequest,
            aikRequestLen,
            privCAKeyBytes,
            privCAKeyBytesLen,
            x509Cert,
            &aikRes,
            &aikResLen);

    // cleanup
    releaseByteArray(env, jAikRequest, aikRequest);
    releaseByteArray(env, jPrivCABytes, privCAKeyBytes);
    releaseByteArray(env, jPubCABytes, cert);
    // X509_free(x509Cert); 		// NOT NEEDED tspi_interface_validate_identity_binding takes care of it

    if (res != SUCCESS || aikRes == NULL) {
        throwException(env, "Unable to verify Identity Key binding.");
        return NULL;
    } else {
        // Return the public key bytes of the AIK public key
        jbyteArray ret = (*env) -> NewByteArray(env, aikResLen);
        (*env) -> SetByteArrayRegion(env, ret, 0, aikResLen, aikRes);
        // Free the bytes
        free(aikRes);
        return ret;
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_createAttestationResponse(
        JNIEnv* env,
        jclass obj,
        jint handle,
        jbyteArray jEKCertificate,
        jbyteArray jaikBlob,
        jbyteArray jAIKCertificate) {

    size_t aikPublicKeyBlobLen = 0;
    BYTE* aikPublicKeyBlob = NULL;
    size_t eKCertificateBytesLen = 0;
    BYTE* eKCertificateBytes = NULL;
    RSA* ekPub = NULL;
    size_t AIKCertificateBytesLen = 0;
    BYTE* AIKCertificateBytes = NULL;

    extractByteArray(env, jaikBlob, &aikPublicKeyBlobLen, &aikPublicKeyBlob);
    // Read EK certificate
    // Certificate field is not optional
    if (!extractByteArray(env, jEKCertificate, &eKCertificateBytesLen, &eKCertificateBytes)) {
        throwException(env, "NULL EK public key provided.");
        return NULL;
    }
    // Get the RSA pub key from bytes
    if (!cert_utils_rsa_pub_key_from_bytes(eKCertificateBytes, eKCertificateBytesLen, &ekPub)) {
        throwException(env, "Invalid RSA EK public key.");
        return NULL;
    }
    // Read AIK credentials; certificate field is not optional
    if (!extractByteArray(env, jAIKCertificate, &AIKCertificateBytesLen, &AIKCertificateBytes)) {
        throwException(env, "NULL AIK certificate provided.");
        return NULL;
    }

    size_t attestationDataLen = 0;
    BYTE* attestationData = NULL;

    T_OUTCOME res = tspi_interface_create_attestation_data(
            handle,
            aikPublicKeyBlob,
            aikPublicKeyBlobLen,
            ekPub,
            AIKCertificateBytes,
            AIKCertificateBytesLen,
            &attestationData,
            &attestationDataLen);

    releaseByteArray(env, jaikBlob, aikPublicKeyBlob);
    releaseByteArray(env, jEKCertificate, eKCertificateBytes);
    releaseByteArray(env, jAIKCertificate, AIKCertificateBytes);
    // RSA_free(ekPub);		// NOT NEEDED


    if (res == SUCCESS && attestationData != NULL) {
        jbyteArray jattestationData = (*env) -> NewByteArray(env, attestationDataLen);
        (*env) -> SetByteArrayRegion(env, jattestationData, 0, attestationDataLen, attestationData);
        // return the unsealed data
        return jattestationData;
    } else {
        throwException(env, "Unable to create the attestation data.");
        return NULL;
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_activateIdentity(
        JNIEnv* env,
        jclass obj,
        jint handle,
        jbyteArray jAttestationData,
        jbyteArray jAikBytes) {


    BYTE* attestationData = NULL;
    size_t attestationDataLen = 0;

    if (!extractByteArray(env, jAttestationData, &attestationDataLen, &attestationData)) {
        throwException(env, "NULL attestation data.");
        return NULL;
    }


    BYTE* aikBytes = NULL;
    size_t aikBytesLen = 0;

    if (!extractByteArray(env, jAikBytes, &aikBytesLen, &aikBytes)) {
        throwException(env, "NULL aik bytes.");
        return NULL;
    }


    BYTE* aikCredentials = NULL;
    size_t aikCredentialsLen = 0;

    T_OUTCOME res = tspi_interface_install_aik(
            handle,
            attestationData,
            attestationDataLen,
            aikBytes,
            aikBytesLen,
            &aikCredentials,
            &aikCredentialsLen);


    releaseByteArray(env, jAttestationData, attestationData);
    releaseByteArray(env, jAikBytes, aikBytes);



    if (res == SUCCESS && aikCredentialsLen > 0) {
        jbyteArray jaikCredentials = (*env) -> NewByteArray(env, aikCredentialsLen);
        (*env) -> SetByteArrayRegion(env, jaikCredentials, 0, aikCredentialsLen, aikCredentials);
        // return the unsealed data
        return jaikCredentials;
    } else {
        throwException(env, "Unable to activate the given identity.");
        return NULL;
    }

}

JNIEXPORT jbyteArray JNICALL
Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_createCredentials(
        JNIEnv* env,
        jclass obj,
        jint contextHandle,
        jbyteArray jPublicKeyBytes,
        jbyteArray jIssuerPrivateKey,
        jbyteArray jIssuerCertificateBytes) {
    size_t privateKeyLen = 0;
    BYTE* privateKey = NULL;
    // Certificate field is not optional
    if (!extractByteArray(env, jIssuerPrivateKey, &privateKeyLen, &privateKey)) {
        throwException(env, "NULL Private Key provided.");
        return NULL;
    }

    size_t pubKeyLen = 0;
    BYTE* pubKey = NULL;
    // Certificate field is not optional
    if (!extractByteArray(env, jPublicKeyBytes, &pubKeyLen, &pubKey)) {
        throwException(env, "NULL pub Key provided.");
        return NULL;
    }


    size_t issuerCertificateLen = 0;
    BYTE* issuerCertificate = NULL;
    // Certificate field is not optional
    if (!extractByteArray(env, jIssuerCertificateBytes, &issuerCertificateLen, &issuerCertificate)) {
        throwException(env, "NULL issuer certificate provided.");
        return NULL;
    }


    BYTE* certificateBytes;
    size_t certificateBytesLen;

    T_OUTCOME res = cert_utils_create_certificate_from_public_key(
            pubKey,
            pubKeyLen,
            privateKey,
            privateKeyLen,
            issuerCertificate,
            issuerCertificateLen,
            &certificateBytes,
            &certificateBytesLen,
            NULL);


    releaseByteArray(env, jIssuerPrivateKey, privateKey);
    releaseByteArray(env, jIssuerCertificateBytes, issuerCertificate);
    releaseByteArray(env, jPublicKeyBytes, pubKey);


    if (res == SUCCESS) {


        jbyteArray jCredentials = (*env) -> NewByteArray(env, certificateBytesLen);
        (*env) -> SetByteArrayRegion(env, jCredentials, 0, certificateBytesLen, certificateBytes);
        // return the created certificate (PEM format) in bytes
        return jCredentials;
    } else {
        throwException(env, "Unable to create the Certificate.");
        return NULL;
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_quote(
        JNIEnv* env,
        jclass obj,
        jint context,
        jbyteArray jAikBytes,
        jbyteArray jNonce,
        jintArray jPcrs) {

    BYTE* aikBytes = NULL;
    size_t aikBytesLen = 0;
    BYTE* nonce = NULL;
    size_t nonceLen = 0;
    int* pcrs = NULL;
    size_t pcrsNum = 0;


    if (!extractByteArray(env, jAikBytes, &aikBytesLen, &aikBytes)) {
        throwException(env, "NULL AIK provided.");
        return NULL;
    }

    if (!extractByteArray(env, jNonce, &nonceLen, &nonce) || nonceLen != 20) {
        throwException(env, "Invalid nonce provided.");
        return NULL;
    }

    if (!extractIntArray(env, jPcrs, &pcrsNum, &pcrs) || pcrsNum <= 0) {
        throwException(env, "Invalid PCRs.");
        return NULL;
    }

    // Check the range of PCRs
    size_t i;
    for (i = 0; i < pcrsNum; i++) {
        if (pcrs[i] > 23 || pcrs[i] < 0) {
            throwException(env, "Invalid PCR index %i.", pcrs[i]);
            return NULL;
        }
    }


    BYTE* quoteResult = NULL;
    size_t quoteResultLen = 0;

    T_OUTCOME res = tspi_interface_pcr_quote(
            context,
            aikBytes,
            aikBytesLen,
            pcrs,
            pcrsNum,
            nonce,
            &quoteResult,
            &quoteResultLen);


    // cleanup

    releaseByteArray(env, jAikBytes, aikBytes);
    releaseByteArray(env, jNonce, nonce);
    releaseIntArray(env, jPcrs, pcrs);


    if (res == SUCCESS) {


        jbyteArray jQuoteRes = (*env) -> NewByteArray(env, quoteResultLen);
        (*env) -> SetByteArrayRegion(env, jQuoteRes, 0, quoteResultLen, quoteResult);
        // return the created certificate (PEM format) in bytes
        return jQuoteRes;
    } else {
        throwException(env, "Unable to perform the Quote operation.");
        return NULL;
    }

}

JNIEXPORT void JNICALL
Java_com_intel_icecp_node_security_tpm_tpm12_Tss1_12NativeInterface_verifyQuote(
        JNIEnv* env,
        jclass obj,
        jint context,
        jbyteArray jQuoteValue,
        jbyteArray jQuoteInfo,
        jbyteArray jAikCertificate,
        jbyteArray jExpectedValue,
        jbyteArray jNonce) {
    BYTE* quoteValue = NULL;
    size_t quoteValueLen = 0;
    BYTE* quoteInfo = NULL;
    size_t quoteInfoLen = 0;
    BYTE* aikCert = NULL;
    size_t aikCertLen = 0;
    BYTE* expectedValue = NULL;
    size_t expectedValueLen = 0;
    BYTE* nonce = NULL;
    size_t nonceLen = 0;



    if (!extractByteArray(env, jQuoteValue, &quoteValueLen, &quoteValue)) {
        throwException(env, "Invalid quote value provided.");
        return;
    }
    if (!extractByteArray(env, jQuoteInfo, &quoteInfoLen, &quoteInfo)) {
        throwException(env, "Invalid quote info provided.");
        return;
    }
    if (!extractByteArray(env, jAikCertificate, &aikCertLen, &aikCert)) {
        throwException(env, "Invalid expected value provided.");
        return;
    }
    if (!extractByteArray(env, jExpectedValue, &expectedValueLen, &expectedValue) || expectedValueLen != 20) {
        throwException(env, "Invalid expected value provided.");
        return;
    }
    if (!extractByteArray(env, jNonce, &nonceLen, &nonce) || nonceLen != 20) {
        throwException(env, "Invalid nonce provided.");
        return;
    }



    T_OUTCOME res = tspi_interface_verify_quote(
            context,
            aikCert,
            aikCertLen,
            quoteValue,
            quoteValueLen,
            expectedValue, // Always 20 bytes, because SHA-1
            nonce,
            quoteInfo,
            quoteInfoLen);



    // Cleanup
    releaseByteArray(env, jQuoteValue, quoteValue);
    releaseByteArray(env, jQuoteInfo, quoteInfo);
    releaseByteArray(env, jAikCertificate, aikCert);
    releaseByteArray(env, jExpectedValue, expectedValue);
    releaseByteArray(env, jNonce, nonce);


    if (res != SUCCESS) {
        throwException(env, "Invalid quote provided.");
    }

    return;
}


