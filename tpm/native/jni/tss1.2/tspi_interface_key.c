#include "tspi_interface.h"

T_OUTCOME
tspi_interface_load_registered_keys(
        Handle contextHandle,
        UINT32 storageType) {


    // Load all the registered key UUIDs
    UINT32 hierarchySize = 0;
    TSS_KM_KEYINFO* ppKeyHierarchy = NULL;
    T_OUTCOME outcome = SUCCESS;
    Context* context = NULL;
    TSS_RESULT result = TSS_SUCCESS;

    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), outcome, "cm_get_context");


    // Load all the keys registered in the User storage
    result = Tspi_Context_GetRegisteredKeysByUUID(
            context -> hContext,
            storageType,
            NULL,
            &hierarchySize,
            &ppKeyHierarchy);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Context_GetRegisteredKeysByUUID");

    size_t i = 0;
    for (; i < hierarchySize; i++) {
        // Load the UUID
        // VERIFY_AND_TERMINATE(
        // 	!cm_load_uuid(&ppKeyHierarchy[i].keyUUID),
        // 	outcome, 
        // 	"Tspi_Context_GetRegisteredKeysByUUID");

        cm_load_uuid(&ppKeyHierarchy[i].keyUUID);
    }

cleanup:

    if (ppKeyHierarchy) {
        free(ppKeyHierarchy);
    }


    return outcome;

}

T_OUTCOME
tspi_interface_load_SRK(
        Handle contextHandle,
        BYTE* srkSecret,
        size_t srkSecretLen) {
    Context* context;

    uint8_t functionResult = SUCCESS;
    TSS_RESULT result;
    TSS_HPOLICY hSRKPolicy;
    // Well known UUID of the SRK key
    TSS_UUID SRK_UUID = TSS_UUID_SRK;

    // In case we have an invalid handle, we can stop
    if (!cm_get_context(contextHandle, &context)) {
        return ERROR;
    }


    // Get the SRK handle
    result = Tspi_Context_LoadKeyByUUID(
            context -> hContext,
            TSS_PS_TYPE_SYSTEM,
            SRK_UUID,
            &(context -> hSRK));
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionResult, "Tspi_Context_Connect");

    // Get the SRK policy
    result = Tspi_GetPolicyObject(
            context -> hSRK,
            TSS_POLICY_USAGE,
            &hSRKPolicy);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionResult, "Tspi_GetPolicyObject");

    // Set the SRK policy to be the well known secret
    // TSS_SECRET_MODE_SHA1 tells TSP to NOT hash the 20 bytes.
    // In order for this to work, require a priori initialization of SRK:
    //				sudo tpm_getownership -z
    //
    int mode = TSS_SECRET_MODE_SHA1;
    if (srkSecretLen != 20) {
        mode = TSS_SECRET_MODE_PLAIN;
    }
    result = Tspi_Policy_SetSecret(
            hSRKPolicy,
            mode,
            srkSecretLen,
            srkSecret);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionResult, "Tspi_Policy_SetSecret");

cleanup:

    return functionResult;

}

T_OUTCOME
tspi_interface_unregister_key(
        Handle contextHandle,
        BYTE* uuidBytes,
        UINT32 persistentStorageType) {
    // Result of the function
    uint8_t functionResult = SUCCESS;

    Context* context = NULL;
    TSS_RESULT result;
    TSS_HKEY hKeyToUnregister = 0;
    TSS_UUID uuid;
    UINT64 offset = 0;

    // Try to load the context
    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), functionResult, "cm_get_context");

    Trspi_UnloadBlob_UUID(
            &offset,
            uuidBytes,
            &uuid);

    // Set the UUID free
    VERIFY_AND_TERMINATE(!cm_free_uuid(&uuid), functionResult, "cm_free_uuid");

    // unregister the key
    result = Tspi_Context_UnregisterKey(
            context -> hContext,
            persistentStorageType,
            uuid,
            &hKeyToUnregister);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionResult, "Tspi_Context_UnregisterKey");


cleanup:
    // Free the space used for the bind key
    Tspi_Context_CloseObject(context -> hContext, hKeyToUnregister);


    return functionResult;


}

T_OUTCOME
tspi_interface_create_key(
        Handle contextHandle,
        UINT32 keyType,
        UINT32 keySize,
        UINT32 keyMigratable,
        UINT32 keyAuthorization,
        UINT32 keyVolatile,
        UINT32 persistentStorageType,
        bool load,
        BYTE* keySecret,
        uint8_t keySecretLength,
        BYTE** uuidBytes,
        size_t* uuidBytesLen,
        BYTE** keyBytes,
        size_t* keyBytesLen,
        TSS_HKEY* newKeyHandle) {

    Context* context;

    T_OUTCOME functionOutcome = SUCCESS;

    TSS_RESULT result;

    // Handle for the Key
    TSS_HKEY keyHandle;

    // Handle for the associated key policy (if (keyAuthorization == TSS_KEY_AUTHORIZATION))
    TSS_HPOLICY keyPolicy;

    // UUID of the newly created key 
    TSS_UUID* keyUuid;

    // UUID of the new key, if NON_VOLATILE
    TSS_UUID* newKeyUUID = NULL;

    // Well known UUID of the SRK key
    TSS_UUID SRK_UUID = TSS_UUID_SRK;


    // Try to load the context
    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), functionOutcome, "cm_get_context");


    // Check if the key is volatile or not. If not volatile, we need to register it
    if (keyVolatile == TSS_KEY_NON_VOLATILE) {
        // Try to retrieve a free UUID
        VERIFY_AND_TERMINATE(!cm_get_available_uuid(&newKeyUUID), functionOutcome, "cm_get_available_uuid");
    }
    // Here, we either have an UUID, or the key is volatile.


    // Flags for key intialization
    TSS_FLAG initFlags = keyType | keySize | keyAuthorization | keyMigratable | keyVolatile;

    // TSS_Context_CreateObject creates and initializes an empty object of the specified type and returns 
    // a handle addressing that object. The object is bound to an already opened context hContext. 
    result = Tspi_Context_CreateObject(
            context -> hContext,
            TSS_OBJECT_TYPE_RSAKEY,
            initFlags,
            &keyHandle);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionOutcome, "Tspi_Context_CreateObject");

    // If key requires authorization, we create and set a Policy
    if (keyAuthorization == TSS_KEY_AUTHORIZATION) {
        // keySecret MUST NOT be null in this case!
        VERIFY_AND_TERMINATE(keySecret == NULL || keySecretLength <= 0, functionOutcome, "keySecret NULL check");

        // Set the key policy with the given secret
        result = tspi_utils_set_object_policy(
                context,
                &keyPolicy,
                keyHandle,
                TSS_SECRET_MODE_PLAIN,
                TRUE,
                keySecret,
                keySecretLength);
        VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionOutcome, "tspi_utils_set_object_policy");
    }

    // Ask the TPM to create the KEY as child of the SRK
    result = Tspi_Key_CreateKey(
            keyHandle,
            context -> hSRK,
            0);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionOutcome, "Tspi_Key_CreateKey");

    // If the key is non volatile, try to persist it in the specified persistent storage
    if (keyVolatile == TSS_KEY_NON_VOLATILE) {
        result = Tspi_Context_RegisterKey(
                context -> hContext,
                keyHandle,
                persistentStorageType,
                *newKeyUUID,
                TSS_PS_TYPE_SYSTEM,
                SRK_UUID);
        VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionOutcome, "Tspi_Context_RegisterKey");

        // UUID blob is of 16 bytes
        *uuidBytes = malloc(16);
        *uuidBytesLen = 16;

        UINT64 offset = 0;
        Trspi_LoadBlob_UUID(
                &offset,
                *uuidBytes,
                *newKeyUUID);

    } else // We assume that if not registered, the user wants the key blob out
    {
        // We extract the encrypted key bytes
        result = Tspi_GetAttribData(
                keyHandle,
                TSS_TSPATTRIB_KEY_BLOB,
                TSS_TSPATTRIB_KEYBLOB_BLOB,
                keyBytesLen,
                keyBytes);
        VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionOutcome, "Tspi_GetAttribData - key blob");
    }

    // Check if we should load the key
    if (load) {
        // Load the newly created key for use
        result = Tspi_Key_LoadKey(
                keyHandle,
                context -> hSRK);
        VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionOutcome, "Tspi_Key_LoadKey");
        if (newKeyHandle) {
            *newKeyHandle = keyHandle;
        }
    }


cleanup:
    // Finally, we can delete the handle
    if (!load) {
        Tspi_Context_CloseObject(context -> hContext, keyHandle);
    }

    if (newKeyUUID) {
        free(newKeyUUID);
    }

    return functionOutcome;
}

T_OUTCOME
load_key_by_uuid(
        Context* context,
        Handle contextHandle,
        BYTE* uuidBytes,
        BYTE* keySecret,
        size_t keySecretlen,
        TSS_HKEY* key) {

    TSS_RESULT result;
    TSS_UUID uuid;
    TSS_HPOLICY keyPolicy;
    uint8_t functionOutcome = SUCCESS;

    UINT64 offset = 0;

    Trspi_UnloadBlob_UUID(
            &offset,
            uuidBytes,
            &uuid);


    print_log(__LINE__, __func__, "Loading key with UUID {0,0,0,0,%i,%i}", uuid.rgbNode[4], uuid.rgbNode[5]);

    // Once retrived the UUID, we can load the key
    result = Tspi_Context_LoadKeyByUUID(
            context -> hContext,
            TSS_PS_TYPE_USER,
            uuid,
            key);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionOutcome, "Tspi_Context_LoadKeyByUUID");

    // Set the secret if not null
    if (keySecret != NULL) {
        print_log(__LINE__, __func__, "Setting policy to the key");

        result = tspi_utils_set_object_policy(
                context,
                &keyPolicy,
                *key,
                TSS_SECRET_MODE_PLAIN,
                TRUE,
                keySecret,
                keySecretlen);
        VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionOutcome, "tspi_utils_set_object_policy");
    }

cleanup:

    return functionOutcome;
}

T_OUTCOME
tspi_interface_get_public_key(
        Handle contextHandle,
        int keyId, // in
        BYTE* keyUUUIDBytes, // in
        BYTE** publicKeyBytes, // out
        size_t* publicKeyBytesLen) // out
{
    T_OUTCOME functionOutcome = SUCCESS;
    TSS_RESULT result;
    Context* context = NULL;
    TSS_HKEY keyHandle;

    // Try to load the context
    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), functionOutcome, "cm_get_context");

    // If keyId is 0, we want the public part of EK
    switch (keyId) {
        case TPM_EK_KEY_ID:
            // Get the EK key
            result = Tspi_TPM_GetPubEndorsementKey(
                    context -> hTPM,
                    TRUE, // TRUE -> needs owner privileges
                    NULL, // No nonce provided
                    &keyHandle);
            VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionOutcome, "Tspi_TPM_GetPubEndorsementKey");
            break;

        case TPM_SRK_KEY_ID:
            // SRK needs to be loaded before doing this!
            keyHandle = context -> hSRK;
            break;

        default:

            // keyUUIDBytes must not be null in this case
            VERIFY_AND_TERMINATE(keyUUUIDBytes == NULL, functionOutcome, "keyUUUIDBytes NULL check");

            // Retrieve the UUID for the given key, if exists
            result = load_key_by_uuid(
                    context,
                    contextHandle,
                    keyUUUIDBytes,
                    NULL, //keySecret,
                    0, //keySecretlen,
                    &keyHandle);
            // Try to load the key by id
            VERIFY_AND_TERMINATE(!result, functionOutcome, "load_key_by_uuid");
    }


    // Get the key from the handle
    functionOutcome = tspi_utils_public_key_bytes_from_handle(
            context,
            keyHandle,
            publicKeyBytes,
            publicKeyBytesLen);


cleanup:
    // Close the handle we used for the key
    if (keyId != TPM_SRK_KEY_ID) {
        Tspi_Context_CloseObject(context -> hContext, keyHandle);
    }


    return functionOutcome;
}

T_OUTCOME
tspi_interface_get_public_key_from_public_key_blob(
        Handle contextHandle,
        BYTE* publicKeyBlob,
        size_t publicKeyBlobLen,
        BYTE** publicKeyBytes,
        size_t* publicKeyBytesLen) {

    T_OUTCOME outcome = SUCCESS;
    TSS_RESULT result;
    Context* context = NULL;
    TSS_HKEY keyHandle;

    // Try to load the context
    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), outcome, "cm_get_context");


    result = tspi_utils_load_public_key_blob(
            context,
            &keyHandle,
            publicKeyBlobLen,
            TSS_KEY_TYPE_SIGNING | TSS_KEY_SIZE_2048 | TSS_KEY_NO_AUTHORIZATION | TSS_KEY_NOT_MIGRATABLE,
            publicKeyBlob);
    // Try to load the context
    VERIFY_AND_TERMINATE(result != TSS_SUCCESS, outcome, "tspi_utils_load_public_key_blob");


    // Get the bytes from handle
    outcome = tspi_utils_public_key_bytes_from_handle(
            context,
            keyHandle,
            publicKeyBytes,
            publicKeyBytesLen);


cleanup:

    return outcome;

}

T_OUTCOME
tspi_interface_get_public_key_from_key_blob(
        Handle contextHandle,
        BYTE* keyBlob,
        size_t keyBlobLen,
        BYTE** publicKeyBytes,
        size_t* publicKeyBytesLen) {

    T_OUTCOME outcome = SUCCESS;
    TSS_RESULT result;
    Context* context = NULL;
    TSS_HKEY keyHandle;

    // Try to load the context
    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), outcome, "cm_get_context");


    // Try to load the key specs blob
    result = Tspi_Context_LoadKeyByBlob(
            context -> hContext,
            context -> hSRK,
            keyBlobLen,
            keyBlob,
            &keyHandle);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Context_LoadKeyByBlob");


    // Get the bytes from handle
    outcome = tspi_utils_public_key_bytes_from_handle(
            context,
            keyHandle,
            publicKeyBytes,
            publicKeyBytesLen);


cleanup:

    return outcome;

}