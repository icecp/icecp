/*
 * File name: tss2_0_interface.c
 * 
 * Purpose: Provides an interface to the TPM tss code.
 *  
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */
#ifdef TPM_WINDOWS
#include <winsock2.h>     // for simulator startup 
#endif

#include "include/tse.h"
#include "include/tseresponsecode.h"
#include "include/tsttransmit.h"  // for simulator power up 
#include "include/TpmTcpProtocol.h"  // for simulator power up 


#include "object_template.h"   // utility functions for object templates creation
#include "log.h"      // utility functions for logging


#include "tss2_0_interface.h"   // Implemented interface



#define SIMULATOR_IP_ADDRESS "134.134.161.13" // @FIXME: temporary define to connect remotely with the TPM simulator

/**
 * Verifies the value of "result" and if differs from TPM_RC_SUCCESS
 * sets the value ERROR, and prints an error and goes to "cleanup".
 * 
 * @param result 		Result 
 * @param finalResult 	The variable to be assigned
 * @param funName 		Name of the function that caused the result
 */
#define VERIFY_TSS_ERROR_AND_TERMINATE(result, finalResult, funName) {\
	if (result != TPM_RC_SUCCESS)\
	{\
		const char *msg;\
		const char *submsg;\
		const char *num;\
		TSEResponseCode_toString(&msg, &submsg, &num, result);\
		print_err (__LINE__, __func__, " %s returned %i - %s %s %s",funName, result, msg, submsg, num);\
		finalResult = ERROR;\
		goto cleanup;\
	}\
}


/**
 * Verifies the value of 'condition'. If true, execute termination commands.
 * 
 * @param condition 	Condition to verify
 * @param finalResult 	The variable to be assigned
 * @param funName 		Name of the function that caused the result
 */
#define VERIFY_AND_TERMINATE(condition, finalResult, funName) {\
	if (condition)\
	{\
		print_err (__LINE__, __func__, " %s failed.", funName);\
		finalResult = ERROR;\
		goto cleanup;\
	}\
}

T_OUTCOME
tss_intialize_tpm() {
    TSE_SetProperty(TPM_SERVER_NAME, SIMULATOR_IP_ADDRESS);
    TPM_RC responseCode = 0;

    Startup_In in;
    TPM_SU startupType = TPM_SU_CLEAR;

    T_OUTCOME outcome = SUCCESS;

    // responseCode = TST_TransmitPlatform(TPM_SIGNAL_POWER_OFF, "TPM2_PowerOffPlatform");
    responseCode = TST_TransmitPlatform(TPM_SIGNAL_POWER_ON, "TPM2_PowerOnPlatform");
    responseCode = TST_TransmitPlatform(TPM_SIGNAL_NV_ON, "TPM2_NvOnPlatform");
    in.startupType = startupType;

    responseCode = TSE_Execute(
            NULL, // No output expected
            (COMMAND_PARAMETERS *) & in,
            NULL,
            TPM_CC_Startup,
            TPM_RH_NULL, NULL, 0);
    // VERIFY_TSS_ERROR_AND_TERMINATE(responseCode, outcome, "TPM_CC_Startup");

cleanup:

    return outcome;
}

T_OUTCOME
tss_create_primary_key(UINT64 primaryHandle, const char* password) {
    TSE_SetProperty(TPM_SERVER_NAME, SIMULATOR_IP_ADDRESS);
    TPM_RC responseCode = 0;
    // input parameter 
    CreatePrimary_In in;
    CreatePrimary_Out out;
    TPMI_SH_AUTH_SESSION sessionHandle0 = TPM_RS_PW;
    TPMI_SH_AUTH_SESSION sessionHandle1 = TPM_RH_NULL;
    TPMI_SH_AUTH_SESSION sessionHandle2 = TPM_RH_NULL;
    unsigned int sessionAttributes0 = 0;
    unsigned int sessionAttributes1 = 0;
    unsigned int sessionAttributes2 = 0;
    T_OUTCOME outcome = SUCCESS;

    // Check the selected hierarchy
    switch (primaryHandle) {
        case TPM_RH_ENDORSEMENT:
            break;
        case TPM_RH_OWNER: // a.k.a., STORAGE
            break;
        case TPM_RH_PLATFORM:
            break;
        case TPM_RH_NULL:
            break;
        default:
            // Unsupported option
            VERIFY_AND_TERMINATE(FALSE, outcome, "Primary Handle type check");
    }

    // Set the well known handle for this key (since primary key for the hierarchy)
    in.primaryHandle = primaryHandle;

    // For now, ignore the password 
    in.inSensitive.t.sensitive.userAuth.t.size = 0;

    // rc = TPM2B_StringCopy(&in.inSensitive.t.sensitive.userAuth.b,
    //       keyPassword, sizeof(TPMU_HA));

    in.inSensitive.t.sensitive.data.t.size = 0;


    in.inPublic.t.publicArea.type = TPM_ALG_RSA;
    in.inPublic.t.publicArea.nameAlg = TPM_ALG_SHA256;
    // Set the attribute value. objectAttributes is a UINT32, we write bit per bit
    in.inPublic.t.publicArea.objectAttributes = 0;
    in.inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_FIXEDTPM;
    in.inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_FIXEDPARENT;
    in.inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_SENSITIVEDATAORIGIN;
    in.inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_USERWITHAUTH;
    in.inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_ADMINWITHPOLICY;
    in.inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_NODA;
    in.inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_RESTRICTED;
    in.inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_DECRYPT;
    in.inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_SIGN;

    // No authentication policy
    in.inPublic.t.publicArea.authPolicy.t.size = 0;

    // Set the RSA parameters
    // Set AES-CBC-256
    in.inPublic.t.publicArea.parameters.rsaDetail.symmetric.algorithm = TPM_ALG_AES;
    in.inPublic.t.publicArea.parameters.rsaDetail.symmetric.keyBits.aes = 128;
    in.inPublic.t.publicArea.parameters.rsaDetail.symmetric.mode.aes = TPM_ALG_CBC;

    // RSA key size
    in.inPublic.t.publicArea.parameters.rsaDetail.scheme.scheme = TPM_ALG_NULL;
    in.inPublic.t.publicArea.parameters.rsaDetail.keyBits = 2048;
    in.inPublic.t.publicArea.parameters.rsaDetail.exponent = 0;

    // @Moreno: No idea what this is...
    in.inPublic.t.publicArea.unique.rsa.t.size = 0;


    in.outsideInfo.t.size = 0;
    in.creationPCR.count = 0;


    // Execute the command
    responseCode = TSE_Execute(
            (RESPONSE_PARAMETERS *) & out,
            (COMMAND_PARAMETERS *) & in,
            NULL, // No extra params
            TPM_CC_CreatePrimary,
            sessionHandle0, NULL, sessionAttributes0, // No parent password: (TPM_RS_PW, NULL, 0)
            sessionHandle1, NULL, sessionAttributes1,
            sessionHandle2, NULL, sessionAttributes2,
            TPM_RH_NULL, NULL, 0);
    VERIFY_TSS_ERROR_AND_TERMINATE(responseCode, outcome, "TPM_CC_CreatePrimary");


    print_log(__LINE__, __func__, "Handle is %u", out.objectHandle);




cleanup:

    return outcome;

}

T_OUTCOME
tss_create() {

}