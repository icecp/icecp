/*
 * File name: tspi_interface_sealing.c
 * 
 * Purpose: Implementation of tspi_interface functions for data sealing and unsealing
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 *
 *
 */
#include "tspi_interface.h"

T_OUTCOME
tspi_interface_seal_data(
        Handle contextHandle,
        BYTE* keySecret,
        uint8_t keySecretlen,
        int* pcrs,
        uint8_t pcrsNum,
        BYTE* dataToSeal,
        int dataToSealLen,
        BYTE** resStructure,
        int* resStructureLen) {
    Context* context;
    // Result of Seal exectution
    uint8_t sealResult = SUCCESS;

    TSS_RESULT result;
    // PCR composite handler
    TSS_HPCRS hPcrs;
    // handle for sealed data
    TSS_HENCDATA hSealedData;
    // Key to use for sealing
    TSS_HKEY key;
    TSS_HPOLICY keyPolicy;

    // Try to load the context
    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), sealResult, "cm_get_context");

    if (pcrs != NULL) {
        // Create PCR handle object
        result = Tspi_Context_CreateObject(
                context -> hContext,
                TSS_OBJECT_TYPE_PCRS,
                0,
                &hPcrs);
        VERIFY_ERROR_AND_TERMINATE_TSPI(result, sealResult, "Tspi_Context_CreateObject");

        // Read PCRs values and set them into hPcrs as a PCR composite (hash)
        result = tspi_utils_read_pcrs(
                context,
                pcrs,
                pcrsNum,
                hPcrs);
        VERIFY_ERROR_AND_TERMINATE_TSPI(result, sealResult, "tspi_utils_read_pcrs");
    } else {
        // Set to null, meaning we are not interested in 
        // binding the sealed data to PCR values.
        hPcrs = 0;
    }


    // Create a sealed data object to contain encrypted data
    result = Tspi_Context_CreateObject(
            context -> hContext,
            TSS_OBJECT_TYPE_ENCDATA,
            TSS_ENCDATA_SEAL,
            &hSealedData);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, sealResult, "Tspi_Context_CreateObject");


    // Set the policy on the data to seal
    TSS_HPOLICY dataToSealedPolicy;
    result = tspi_utils_set_object_policy(
            context,
            &dataToSealedPolicy,
            hSealedData,
            TSS_SECRET_MODE_PLAIN,
            TRUE,
            keySecret,
            keySecretlen);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, sealResult, "tspi_utils_set_object_policy");

    // We get the key blob out	
    BYTE* sealKeyInfoBytes;
    UINT32 sealKeyInfoBytesLen;

    // Create a wrap key for sealing under SRK
    result = tspi_interface_create_key(
            contextHandle,
            TSS_KEY_TYPE_STORAGE,
            TSS_KEY_SIZE_2048,
            TSS_KEY_NOT_MIGRATABLE,
            TSS_KEY_AUTHORIZATION,
            TSS_KEY_VOLATILE,
            0, // Not registered
            TRUE, // We want to load the key
            keySecret,
            keySecretlen,
            NULL, // No UUID will be returned
            0, // No UUID will be returned
            &sealKeyInfoBytes, // Get the key blob out
            &sealKeyInfoBytesLen,
            &key); // the key handle will be placed here
    VERIFY_AND_TERMINATE(!result, sealResult, "tspi_interface_create_key");


    // Seal the data with the given key
    result = Tspi_Data_Seal(
            hSealedData,
            key,
            dataToSealLen,
            dataToSeal,
            hPcrs);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, sealResult, "Tspi_Data_Seal");


    BYTE* sealedData;
    UINT32 sealedDataLen;

    // Retrieve the sealed data and write it to sealedData.
    result = Tspi_GetAttribData(
            hSealedData,
            TSS_TSPATTRIB_ENCDATA_BLOB,
            TSS_TSPATTRIB_ENCDATABLOB_BLOB,
            &sealedDataLen,
            &sealedData);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, sealResult, "Tspi_GetAttribData");


    // Return a data structure of the form:
    // 			  		32 bits
    // 	-----------------------------------------
    //	|			TSS KEY INFO SIZE          	|
    //	-----------------------------------------
    // 	|               	...					|
    // 	|               	...					|
    // 						...
    // 	-----------------------------------------
    // 	|			SEALED DATA SIZE          	|
    //	-----------------------------------------
    // 	|               	...					|
    // 	|               	...					|
    // 						...

    // First retrieve Key Info blob 

    UINT64 offset = 0;

    // Compose the structure
    *resStructureLen = sizeof (sealKeyInfoBytesLen) + sealKeyInfoBytesLen + sizeof (sealedDataLen) + sealedDataLen;
    // Allocate resStructureLen bytes
    *resStructure = malloc(*resStructureLen);

    Trspi_LoadBlob_UINT32(
            &offset,
            sealKeyInfoBytesLen,
            *resStructure);

    Trspi_LoadBlob(
            &offset,
            sealKeyInfoBytesLen,
            *resStructure,
            sealKeyInfoBytes);

    Trspi_LoadBlob_UINT32(
            &offset,
            sealedDataLen,
            *resStructure);

    Trspi_LoadBlob(
            &offset,
            sealedDataLen,
            *resStructure,
            sealedData);

cleanup:

    Tspi_Context_CloseObject(context -> hContext, hPcrs);

    Tspi_Context_CloseObject(context -> hContext, hSealedData);
    Tspi_Context_CloseObject(context -> hContext, key);
    Tspi_Policy_FlushSecret(dataToSealedPolicy);

    return sealResult;

}

T_OUTCOME
tspi_interface_unseal_data(
        Handle contextHandle,
        BYTE* keySecret,
        size_t keySecretlen,
        BYTE* dataToUnseal,
        size_t dataToUnsealLen,
        BYTE* sealingKeyBlob,
        size_t sealingKeyBlobLen,
        BYTE** unsealedData,
        size_t* unsealedDataLen) {
    // Result of function execution
    uint8_t unsealResult = SUCCESS;

    Context* context;

    TSS_RESULT result;
    // length in bytes of the PCR composite value
    UINT32 ulPcrLen;
    // handle for unsealed data
    TSS_HENCDATA hUnsealedData;
    TSS_HKEY keyToUseToUnseal;

    // Try to load the context
    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), unsealResult, "cm_get_context");

    // Create a sealed data object to contain encrypted data
    result = Tspi_Context_CreateObject(
            context -> hContext,
            TSS_OBJECT_TYPE_ENCDATA,
            TSS_ENCDATA_SEAL,
            &hUnsealedData);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, unsealResult, "Tspi_Context_CreateObject");

    // Load the sealed blob into the created object
    result = Tspi_SetAttribData(
            hUnsealedData,
            TSS_TSPATTRIB_ENCDATA_BLOB,
            TSS_TSPATTRIB_ENCDATABLOB_BLOB,
            dataToUnsealLen,
            dataToUnseal);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, unsealResult, "Tspi_SetAttribData");

    // Set the policy on the data to unseal
    TSS_HPOLICY encDataPolicy;
    result = tspi_utils_set_object_policy(
            context,
            &encDataPolicy,
            hUnsealedData,
            TSS_SECRET_MODE_PLAIN,
            TRUE,
            keySecret,
            keySecretlen);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, unsealResult, "tspi_utils_set_object_policy for unsealed data");

    // Try to load the key specs blob
    result = Tspi_Context_LoadKeyByBlob(
            context -> hContext,
            context -> hSRK,
            sealingKeyBlobLen,
            sealingKeyBlob,
            &keyToUseToUnseal);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, unsealResult, "Tspi_Context_LoadKeyByBlob");

    // Now set the policy
    TSS_HPOLICY keyPolicy;
    result = tspi_utils_set_object_policy(
            context,
            &keyPolicy,
            keyToUseToUnseal,
            TSS_SECRET_MODE_PLAIN,
            TRUE,
            keySecret,
            keySecretlen);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, unsealResult, "tspi_utils_set_object_policy for unsealing key");


    // Unseal the data with the given key
    result = Tspi_Data_Unseal(
            hUnsealedData,
            keyToUseToUnseal,
            unsealedDataLen,
            unsealedData);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, unsealResult, "Tspi_Data_Unseal");

cleanup:

    Tspi_Context_CloseObject(context -> hContext, hUnsealedData);
    Tspi_Policy_FlushSecret(keyPolicy);
    Tspi_Policy_FlushSecret(encDataPolicy);


    return unsealResult;
}