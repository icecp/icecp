/*
 * File name: tspi_interface_misc.c
 * 
 * Purpose: Implementation of misc tspi_interface functions
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 *
 *
 */
#include "tspi_interface.h"

#include <openssl/rsa.h> // For RSA
#include <unistd.h>   // For 'write'

#include "cert_utils.h"

T_OUTCOME
tspi_interface_create_tpm_context(Handle* contextHandle) {
    // Holds the return value of this function
    uint8_t functionResult = SUCCESS;


    // Try to create a Context
    if (cm_create_context(contextHandle)) {
        // Result of the execution of each command
        TSS_RESULT result = TSS_SUCCESS;
        Context* context = NULL;
        // get the Context we have just created (we are sure it will not fail)
        cm_get_context(*contextHandle, &context);

        // Create a TPM context
        result = Tspi_Context_Create(&(context -> hContext));
        VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionResult, "Tspi_Context_Create");

        // Select the system TPM (use NULL)
        result = Tspi_Context_Connect(context -> hContext, NULL);
        VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionResult, "Tspi_Context_Connect");

        // Obtain an handle to the TPM
        result = Tspi_Context_GetTpmObject(context -> hContext, &(context -> hTPM));
        VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionResult, "Tspi_Context_GetTpmObject");

    }

cleanup:

    if (functionResult == ERROR) {
        // Free the allocated context space before returning an error
        cm_delete_context(*contextHandle);
    }

    return functionResult;
}

/**
 * Creates a policy object for the TPM and adds the given owner secret
 *  
 */
T_OUTCOME
tspi_set_tpm_owner_privileges(
        Handle contextHandle,
        BYTE* ownerSecret,
        uint8_t ownerSecretLength) {
    Context* context;
    uint8_t functionOutcome = SUCCESS;
    TSS_RESULT result;
    TSS_HPOLICY hTPMPolicy;

    // Try to load the context
    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), functionOutcome, "cm_get_context");

    // Owner secret must be not NULL
    VERIFY_AND_TERMINATE((ownerSecret == NULL), functionOutcome, "Check owner secret not NULL");

    // Set the object policy
    result = tspi_utils_set_object_policy(
            context,
            &hTPMPolicy,
            context -> hTPM,
            TSS_SECRET_MODE_PLAIN,
            TRUE,
            ownerSecret,
            ownerSecretLength);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, functionOutcome, "tspi_utils_set_object_policy");

cleanup:

    return functionOutcome;
}

T_OUTCOME
tspi_flush_tpm_owner_privileges(
        Handle contextHandle) {

    Context* context;
    T_OUTCOME outcome = SUCCESS;
    TSS_HPOLICY hTPMpolicy = 0;
    TSS_RESULT result;

    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), outcome, "cm_get_context");

    // Get the handle to the TPM policy
    result = Tspi_GetPolicyObject(
            context -> hTPM,
            TSS_POLICY_USAGE,
            &hTPMpolicy);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_GetPolicyObject");

    // Flush the secret
    Tspi_Policy_FlushSecret(hTPMpolicy);

cleanup:
    return outcome;
}

T_OUTCOME
tspi_interface_extend_pcr(
        Handle contextHandle,
        int pcr,
        BYTE* data,
        int dataLen,
        BYTE** Final_PCR_Value,
        UINT32* PCR_result_length) {

    uint8_t extendResult = SUCCESS;

    Context* context;
    // Try to load the context
    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), extendResult, "cm_get_context");

    // Simply call the corresponding function
    TSS_RESULT result;
    result = Tspi_TPM_PcrExtend(
            context -> hTPM,
            pcr,
            dataLen,
            data,
            NULL,
            PCR_result_length,
            Final_PCR_Value);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, extendResult, "Tspi_TPM_PcrExtend");

cleanup:

    return extendResult;
}

T_OUTCOME
tspi_interface_get_random_bytes(
        Handle contextHandle,
        size_t bytesToGenerate,
        BYTE** generatedBytes) {
    Context* context;
    T_OUTCOME outcome = SUCCESS;
    TSS_RESULT result;

    // Try to load the context
    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), outcome, "cm_get_context");


    result = Tspi_TPM_GetRandom(
            context -> hTPM,
            bytesToGenerate,
            generatedBytes);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_TPM_GetRandom");


cleanup:

    return outcome;

}

T_OUTCOME
tspi_interface_load_owner_delagation(
        Handle contextHandle,
        BYTE* ownerDelegationBlob,
        size_t ownerDelegationBlobLen) {

    Context* context;
    T_OUTCOME outcome = SUCCESS;
    TSS_RESULT result;

    TSS_HPOLICY hDelegation = 0;
    TSS_HDELFAMILY hFamily = 0;

    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), outcome, "cm_get_context");


    // (A) Create a delegation object (i.e., a policy)
    result = Tspi_Context_CreateObject(
            context -> hContext,
            TSS_OBJECT_TYPE_POLICY,
            TSS_POLICY_USAGE,
            &hDelegation);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Context_CreateObject");


    // (B) Load delegation
    result = Tspi_SetAttribData(
            hDelegation,
            TSS_TSPATTRIB_POLICY_DELEGATION_INFO,
            TSS_TSPATTRIB_POLDEL_OWNERBLOB,
            ownerDelegationBlobLen,
            ownerDelegationBlob);


    // (C) Set delegation secret
    result = Tspi_Policy_SetSecret(
            hDelegation,
            TSS_SECRET_MODE_PLAIN,
            strlen("secret"), // @TODO: Move it as parameter
            "secret");
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Policy_SetSecret");


    // Set the policy; we can not assign the policy until we have the handle
    result = Tspi_Policy_AssignToObject(
            hDelegation,
            context -> hTPM);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Policy_AssignToObject");


cleanup:

    // For now invalidate the family
    // if (hFamily != 0)
    // 	Tspi_TPM_Delegate_InvalidateFamily(context -> hTPM, hFamily);
    return outcome;


}

/**
 * PRECONDITION: we have owner privileges (i.e., tspi_set_tpm_owner_privileges was execued successfully)
 */
T_OUTCOME
tspi_interface_create_owner_delagation(
        Handle contextHandle,
        BYTE** ownerDelegationBlob,
        size_t* ownerDelegationBlobLen) {

    Context* context;
    T_OUTCOME outcome = SUCCESS;
    TSS_RESULT result;

    TSS_HPOLICY hDelegation = 0;
    TSS_HDELFAMILY hFamily = 0;

    VERIFY_AND_TERMINATE(!cm_get_context(contextHandle, &context), outcome, "cm_get_context");


    // (A) Create a delegation object (i.e., a policy)
    result = Tspi_Context_CreateObject(
            context -> hContext,
            TSS_OBJECT_TYPE_POLICY,
            TSS_POLICY_USAGE,
            &hDelegation);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Context_CreateObject");


    // (B) Set delegation secret
    result = Tspi_Policy_SetSecret(
            hDelegation,
            TSS_SECRET_MODE_PLAIN,
            strlen("secret"), // @TODO: Move it as parameter
            "secret");
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_Policy_SetSecret");


    // (C) Set the type of delegation to OWNER
    result = Tspi_SetAttribUint32(
            hDelegation,
            TSS_TSPATTRIB_POLICY_DELEGATION_INFO,
            TSS_TSPATTRIB_POLDEL_TYPE,
            TSS_DELEGATIONTYPE_OWNER);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_SetAttribUint32 - POLDEL_TYPE");


    // (D) Set parameters PER1 and PER2 (@TODO: specify a set of commands, now all of them)
    result = Tspi_SetAttribUint32(
            hDelegation,
            TSS_TSPATTRIB_POLICY_DELEGATION_INFO,
            TSS_TSPATTRIB_POLDEL_PER1,
            0);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_SetAttribUint32 - PER1");

    result = Tspi_SetAttribUint32(
            hDelegation,
            TSS_TSPATTRIB_POLICY_DELEGATION_INFO,
            TSS_TSPATTRIB_POLDEL_PER2,
            0);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_SetAttribUint32 - PER2");


    // (E) Create a Family
    result = Tspi_TPM_Delegate_AddFamily(
            context -> hTPM,
            'a',
            &hFamily);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_TPM_Delegate_AddFamily");


    result = Tspi_TPM_Delegate_CreateDelegation(
            context -> hTPM,
            'b',
            0, // No flags specified (e.g., no increment counter)
            0, // Not bounded to any PCR value
            hFamily,
            hDelegation);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_TPM_Delegate_CreateDelegation");


    result = Tspi_SetAttribUint32(
            hFamily,
            TSS_TSPATTRIB_DELFAMILY_STATE,
            TSS_TSPATTRIB_DELFAMILYSTATE_ENABLED,
            TRUE);
    VERIFY_ERROR_AND_TERMINATE_TSPI(result, outcome, "Tspi_SetAttribUint32 - FAMILY STATE");


    // (F) Here, we should extract the delegation bytes
    result = Tspi_GetAttribData(
            hDelegation,
            TSS_TSPATTRIB_POLICY_DELEGATION_INFO,
            TSS_TSPATTRIB_POLDEL_OWNERBLOB,
            ownerDelegationBlobLen,
            ownerDelegationBlob);






    // // Cache the delegation
    // result = Tspi_TPM_Delegate_CacheOwnerDelegation(
    // 	context -> hTPM, 
    // 	hDelegation, 
    // 	0,
    // 	TSS_DELEGATE_CACHEOWNERDELEGATION_OVERWRITEEXISTING);


cleanup:

    // For now invalidate the family
    // if (hFamily != 0)
    // 	Tspi_TPM_Delegate_InvalidateFamily(context -> hTPM, hFamily);


    return outcome;


}





