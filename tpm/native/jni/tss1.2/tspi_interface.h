/*
 * File name: tspi_interface.h
 * 
 * Purpose: Definition of a set of functions that can be used to interact with TrouSerS TSS.
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 *
 *
 */
#ifndef TPSI_INTERFACE_H
#define TPSI_INTERFACE_H

#include <stdio.h>
#include <string.h>

#include <tss/tss_error.h>
#include <tss/platform.h>
#include <tss/tss_defines.h>
#include <tss/tss_typedef.h>
#include <tss/tss_structs.h>
#include <tss/tspi.h>

#include <trousers/trousers.h>
#include <openssl/x509.h>

#include "context_manager.h"
#include "boolean.h"
#include "safe_encode_decode.h"
#include "outcome.h"
#include "log.h"


#define TPM_EK_KEY_ID 0
#define TPM_SRK_KEY_ID  1


/**
 * Verifies the value of "result" and if differs from TSS_SUCCESS
 * sets the value ERROR, and prints an error and goes to "cleanup".
 * 
 * @param result 		Result 
 * @param finalResult 	The variable to be assigned
 * @param funName 		Name of the function that caused the result
 */
#define VERIFY_ERROR_AND_TERMINATE_TSPI(result, finalResult, funName) {\
	if (result != TSS_SUCCESS)\
	{\
		print_err (__LINE__, __func__, " %s returned %i - %s ",funName, result, Trspi_Error_String(result));\
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



/**
 * Creates and initializes a new TPM context
 *
 * @param contextHandle 	Reference of the handle of the new context (if context is created)
 *
 * @return SUCCESS/ERROR
 */
T_OUTCOME
tspi_interface_create_tpm_context(
        Handle* contextHandle);




/**
 * Loads the registered keys from the given storage
 * 
 * @param contextHandle 	Context handle
 * @param storageType		Storage from which retrieving the keys
 */
T_OUTCOME
tspi_interface_load_registered_keys(
        Handle contextHandle,
        UINT32 storageType);




/**
 * Loads the SRK key using a given secret (if not NULL)
 *
 * @param contextHandle 		Context handle
 * @param srkSecret 	Secret (if any)
 * @param srkSecretLen 	Length of the secret
 *
 * @return SUCCESS/ERROR
 */
T_OUTCOME
tspi_interface_load_SRK(
        Handle contextHandle,
        BYTE* srkSecret,
        size_t srkSecretLen);


/**
 * Unregisters a key 
 *
 * @param contextHandle 			context handle
 * @param uuidKeyIndex 				uuid index
 * @param persistentStorageType		type of persistent storage where the key resides
 *
 * @return SUCCESS/ERROR
 */
T_OUTCOME
tspi_interface_unregister_key(
        Handle contextHandle,
        BYTE* uuidBytes,
        UINT32 persistentStorageType);


/**
 * Creates a key with the given parameters 
 *
 * @param contextHandle				context handle
 * @param keyType					key type
 * @param keySize					key size
 * @param keyMigratable				key migration 
 * @param keyAuthorization			key authorization
 * @param keyVolatile				flag for volatile key
 * @param persistentStorageType		persistent storage type
 * @param load						load the key (TRUE/FALSE)
 * @param keySecret					secret for the new key (can be NULL)
 * @param keySecretLen
 * @param uuidBytes					bytes corresponding to new key's UUID (if key is marked as NON_VOLATILE)
 * @param uuidBytesLen
 * @param keyBytes					bytes corresponding to new key's KEY_BLOB
 * @param keyBytesLen				
 * @param newKeyHandle				handle of the new key
 *
 * @return SUCCESS/ERROR
 */
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
        TSS_HKEY* newKeyHandle);



/**
 * Sets TPM owner priviledges, assigning the given secret 
 * to TPM's policy object (can be used also to revoke privileges)
 * by setting an incorrect secret (e.g., NULL) 
 * 
 * @param contextHandle context handle
 * @param ownerSecret owner secret
 * @param ownerSecretLength owner secret length
 * 
 * @return SUCCESS/ERROR
 */
T_OUTCOME
tspi_set_tpm_owner_privileges(
        Handle contextHandle,
        BYTE* ownerSecret,
        uint8_t ownerSecretLength);


/**
 * Flushes TPM owner privileges.
 * 
 * @param contextHandle 
 */
T_OUTCOME
tspi_flush_tpm_owner_privileges(
        Handle contextHandle);


/**
 * Retrieves the public key of the given key.
 * If keyId == 0, returns EK's public key (if possible)
 * 
 * @param context 			Context Handle
 * @param keyId 			ID of the key
 * @param keySecretLen 		Length of the Key Secret	(may be 0)
 * @param publicKeyBytes 	PUB_KEY blob				out
 * @param publicKeyBytesLen PUB_KEY blob size			out
 *
 * @return SUCCESS/ERROR
 */
T_OUTCOME
tspi_interface_get_public_key(
        Handle contextHandle,
        int keyId, // in
        BYTE* keyUUUIDBytes, // in
        BYTE** publicKeyBytes, // out
        size_t* publicKeyBytesLen); // out



/**
 * Retrieves the public key from the given PUBLIC_KEY blob.
 * No need for key secret since we are loading just the public part
 * 
 * @param contextHandle [description]
 * @param keyId [description]
 * @param publicKeyBytes [description]
 * @param publicKeyBytesLen [description]
 * 
 * @return SUCCESS/ERROR
 */
T_OUTCOME
tspi_interface_get_public_key_from_public_key_blob(
        Handle contextHandle,
        BYTE* publicKeyBlob,
        size_t publicKeyBlobLen,
        BYTE** publicKeyBytes,
        size_t* publicKeyBytesLen);


/**
 * Retrieves the public key from the given KEY_BLOB blob.
 * No need for key secret since we are loading just the public part
 * 
 * 
 */
T_OUTCOME
tspi_interface_get_public_key_from_key_blob(
        Handle contextHandle,
        BYTE* keyBlob,
        size_t keyBlobLen,
        BYTE** publicKeyBytes,
        size_t* publicKeyBytesLen);


/**
 * Seals the given data, binding them using a given sealing key id, and 
 * PCR values. Key ID is needed to retrieve key's UUID.
 *
 * @param context handle
 * @param identity key secret
 * @param identity key length
 * @param certificate of the Privacy CA
 * @param pointer to the pointer of the the request bytes
 * @param length in bytes of the request
 *
 * @return SUCCESS/ERROR
 */
T_OUTCOME
tspi_interface_seal_data(
        Handle contextHandle,
        BYTE* keySecret,
        uint8_t keySecretlen,
        int* pcrs,
        uint8_t pcrsNum,
        BYTE* dataToSeal,
        int dataToSealLen,
        BYTE** sealedData,
        int* sealedDataLen);


/**
 * Unseals the given sealed data, usign the gien key
 * 
 * @param contextHandle			Handle to a context
 * @param keySecret				Unsealing key secret
 * @param keySecretlen			
 * @param dataToUnseal			Data to be unsealed
 * @param dataToUnsealLen
 * @param sealingKeyBlob		Key Blob of the key to use to unseal
 * @param sealingKeyBlobLen
 * @param unsealedData			Resulting unsealed bytes
 * @param unsealedDataLen
 * 
 * @return SUCCESS/ERROR
 */
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
        size_t* unsealedDataLen);

/**
 * Extends the value of the given PCR, using the given data as input
 * 
 * @param contextHandle 	Handle to the context
 * @param pcr 				The PCR to extend
 * @param data 				Data to use for extension 
 * @param dataLen 			Length of the data to use
 * @param finalPCRvalue 	Resulting value
 * @param finalPCRvalueLen  Resulting value length
 *
 * @return SUCCESS/ERROR
 */
T_OUTCOME
tspi_interface_extend_pcr(
        Handle contextHandle,
        int pcr,
        BYTE* data,
        int dataLen,
        BYTE** finalPCRvalue,
        UINT32* finalPCRvalueLen);

/**
 * Generates a given number of random bytes
 * 
 * @param contextHandle 	Handle to the context
 * @param bytesToGenerate 	Number of bytes to generate
 * @param generatedBytes 	Output bytes
 * 
 * @return SUCCESS/ERROR
 */
T_OUTCOME
tspi_interface_get_random_bytes(
        Handle contextHandle,
        size_t bytesToGenerate,
        BYTE** generatedBytes);



/**
 * Creates an owner delegation blob (should be of type TPM_DELEGATE_OWNER_BLOB)
 * and returns it insie the given location.
 * 
 * @param context [description]
 * @param ownerDelegationBlob [description]
 * @param ownerDelegationBlobLen [description]
 */
T_OUTCOME
tspi_interface_create_owner_delagation(
        Handle context,
        BYTE** ownerDelegationBlob,
        size_t* ownerDelegationBlobLen);



T_OUTCOME
tspi_interface_load_owner_delagation(
        Handle contextHandle,
        BYTE* ownerDelegationBlob,
        size_t ownerDelegationBlobLen);



// ************************************* PRIVACY CA ATTESTATION ********************************* 

/**
 * Client function: Creates a new identity request (for attestation purposes)
 *
 * @param contextHandle context handle
 * @param keySecret identity key secret
 * @param keySecretLength identity key length
 * @param privacyCaCert certificate of the Privacy CA
 * @param identityRequestBytes pointer to the pointer of the the request bytes
 * @param identityRequestLength length in bytes of the request
 *
 * @return SUCCESS/ERROR
 */
T_OUTCOME
tspi_interface_create_identity_request(
        Handle contextHandle,
        BYTE* keySecret,
        uint8_t keySecretLength,
        X509* privacyCaCert,
        BYTE* labelString,
        size_t labelStringLen,
        BYTE** identityRequestBytes,
        UINT32* identityRequestLength);

/**
 * Privacy CA function: Valdates the identity binding field inside the identity request. If correct, 
 * sets AIK TCPA_PUBKEY into aikPublicKey. Note that this function does not 
 * verify the identity of the EK, but just the binding between AIK and the specific request.
 * 
 * 
 * @param contextHandle
 * @param identityReqBlob
 * @param identityReqBlobLen
 * @param privCAsecretKey
 * @param privCAsecretKeyLen
 * @param privacyCaCert
 * @param aikPublicKey
 * @param aikPublicKeyLen
 * 
 * @return SUCCESS/ERROR
 */
T_OUTCOME
tspi_interface_validate_identity_binding(
        Handle contextHandle,
        BYTE* identityReqBlob,
        size_t identityReqBlobLen,
        BYTE* privCAsecretKey,
        size_t privCAsecretKeyLen,
        X509* privacyCaCert,
        BYTE** aikPublicKey,
        size_t* aikPublicKeyLen);


/**
 * Privacy CA function: creates the attestation structures to be sent to
 * the client requesting the AIK credentials. This function should be 
 * called after verification of client's identity
 *
 * @param contextHandle 				Handle for the context
 * @param aikPublicKeyBlob		Blob of AIK's TCPA_PUBKEY, extracted from client's request
 * @param aikPublicKeyBlobLen	Length of AIK's TCPA_PUBKEY blob
 * @param ekPublicKey			EK's RSA public key of the client
 * @param aikCredentials		Serialized X509 Certificate
 * @param aikCredentialsLen		Serialized X509 Certificate length
 * @param attestationData		output structure
 * @param attestationDataLen	output structure length
 *
 * @return SUCCESS/ERROR
 */
T_OUTCOME
tspi_interface_create_attestation_data(
        Handle contextHandle,
        BYTE* aikPublicKeyBlob,
        size_t aikPublicKeyBlobLen,
        RSA* ekPublicKey,
        BYTE* aikCredentials,
        size_t aikCredentialsLen,
        BYTE** attestationData,
        size_t* attestationDataLen);


/**
 * Once received the response from the CA, loads the received AIK credentials
 * into the TPM.
 * 
 * @param contextHandle 	Context handle
 * @param caRespBlob 		Bytes of CA's response
 * @param caRespBlobLen 	Length of CA's resp
 * @param aikCertificate 	output credentials
 * @param aikCertificateLen 
 * 
 * @return SUCCESS/ERROR
 */
T_OUTCOME
tspi_interface_install_aik(
        Handle contextHandle,
        BYTE* caRespBlob,
        size_t caRespBlobLen,
        BYTE* aiksBytes,
        size_t aikBytesLen,
        BYTE** aikCertificate,
        size_t* aikCertificateLen);




T_OUTCOME
tspi_interface_pcr_quote(
        Handle contextHandle,
        BYTE* aikKeyBytes,
        size_t aikKeyBytesLen,
        int* pcrs,
        size_t pcrsNum,
        BYTE* nonce,
        BYTE** quoteRes,
        size_t* quoteResLen);



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
        size_t tpmQuoteInfoLen);







/***************************************** UTILS FUNCTIONS ****************************************/


/**
 * Utility function that loads an public key given a blob
 *
 * @param context 				Valid context pointer
 * @param hKey 					Pointer to the handle of the key
 * @param identityKeyBlobSize 	Size of the key blob
 * @param identityKeyBlob 		Key blob
 *
 * @return the TSS_RESULT result 
 * 
 */
TSS_RESULT
tspi_utils_load_public_key_blob(
        Context* context,
        TSS_HKEY* hKey,
        int initFlags,
        size_t identityKeyBlobSize,
        BYTE* identityKeyBlob);



/**
 * Reads the values of the given PCRs, and produces a composite value
 * 
 * @param context 		Context
 * @param pcrs 			Array of PCRs indexes to consider
 * @param pcrsNum		Size of the array
 * @param pcrComposite	Composite value
 * @param ulPcrLen		Length of the read value
 * @param hPcts			handle to which the value is written
 * 
 * @return the TSS_RESULT corresponding to the executed operations
 * 
 */
TSS_RESULT
tspi_utils_read_pcrs(
        Context* context,
        int* pcrs,
        uint8_t pcrsNum,
        TSS_HPCRS hPcrs);


/**
 * Sets a policy to the given object, using the secret in input
 * 
 * @param context		Context
 * @param policy 		Reference to the policy handle
 * @param objectHandle	Handle to the object on which applying the policy
 * @param secretMode	Mode (TSS_SECRET_MODE_PLAIN|TSS_SECRET_MODE_SHA1|...)
 * @param assign		Tells wether tot assign the policy to the object
 * @param secret		The bytes of the secret to use
 * @param secretLen		Length of the secret in bytes
 * 
 * @return the TSS_RESULT codes returned by the invoked commands (TSS_SUCCESS if everything went fine)
 * 
 */
TSS_RESULT
tspi_utils_set_object_policy(
        Context* context,
        TSS_HPOLICY* policy,
        TSS_HOBJECT objectHandle,
        UINT32 secretMode,
        bool assign,
        BYTE* secret,
        uint8_t secretLen);



/**
 * Extracts the Public Key portion of a given key (given by handle)
 * and places it into the given location, in PEM format.
 * 
 * @param context [description]
 * @param keyHandle [description]
 * @param publicKey [description]
 * @param publicKeyLen [description]
 * 
 * @return SUCCESS/ERROR
 * 
 */
T_OUTCOME
tspi_utils_public_key_bytes_from_handle(
        Context* context,
        TSS_HKEY keyHandle,
        BYTE** publicKey,
        size_t* publicKeyLen);





T_OUTCOME
tspi_utils_create_EK_credentials(
        Handle contextHandle,
        BYTE* publicKeyBytes,
        size_t publicKeyBytesLen,
        BYTE* issuerPrivateKey,
        size_t issuerPrivateKeyLen,
        BYTE* issuerCertificateBytes,
        size_t issuerCertificateBytesLen,
        BYTE** createdCertificateBytes,
        size_t* createdCertificateBytesLen);




T_OUTCOME
tspi_utils_hash(
        Handle contextHandle,
        BYTE* dataToHash,
        size_t dataToHashLen,
        BYTE** hashValue,
        size_t* hashValueLen); // TPM 1.2 supports only SHA-1 so this should be always 20








#endif