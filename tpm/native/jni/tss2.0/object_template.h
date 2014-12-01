/*
 * File name: object_template.h
 * 
 * Purpose: 
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */

#ifndef OBJECT_TEMPLATE_H
#define OBJECT_TEMPLATE_H


#include "include/tse.h"
#include "include/tseresponsecode.h"

/**
 * Object types
 */
#define TYPE_BL  1   // Encrypted Blob (e.g., for sealing)
#define TYPE_ST  2   // Storage key
#define TYPE_DEN 3   // Decryption Key
#define TYPE_DEO 4   // Decryption Key
#define TYPE_SI  5   // Signature RSA
#define TYPE_SIR 6   // Signature Restricted RSA
#define TYPE_GP  7   // General Purpose RSA
#define TYPE_DES 8   // Encryption Decryption with AES
#define TYPE_KH  9   // HMAC


/** 
 * Creates a template for an ECC or RSA 2048 key (side effect on parameter {in}). 
 * If restricted, it uses the RSASSA padding scheme
 * 
 * @param in			The Create_In object pointer
 * @param keyType		Key type						: TYPE_ST|TYPE_DEN|TYPE_DEO|TYPE_SI|TYPE_SIR|TYPE_GP
 * @param algPublic		Type of algorithm				: TPM_ALG_RSA|TPM_ALG_ECC
 * @param curveID		(if algPublic == TPM_ALG_ECC)	: TPM_ECC_BN_P256|TPM_ECC_NIST_P256|TPM_ECC_NIST_P384
 * @param nalg			Name hash algorithm				: TPM_ALG_SHA1|TPM_ALG_SHA256|TPM_ALG_SHA384
 * @param halg			Scheme hash algorithm			: TPM_ALG_SHA1|TPM_ALG_SHA256|TPM_ALG_SHA384
 * 
 */
void asymPublicTemplate(Create_In *in, int keyType, TPMI_ALG_PUBLIC algPublic, TPMI_ECC_CURVE curveID, TPMI_ALG_HASH nalg, TPMI_ALG_HASH halg);

/**
 * Creates a template for an AES 128 CBC key (side effect on parameter {in}).
 * 
 * @param in 			The Create_In object pointer
 * @param nalg			Name hash algorithm				: TPM_ALG_SHA1|TPM_ALG_SHA256|TPM_ALG_SHA384	
 * @param rev116 		
 * 
 */
void symmetricCipherTemplate(Create_In *in, TPMI_ALG_HASH nalg, int keySize, int rev116);

/** 
 * Creates a template for a HMAC key (side effect on parameter {in}).
 *
 * The name alg is SHA-256, but the key is not restricted
 * 
 * @param in			The Create_In object pointer
 * @param nalg			Name hash algorithm				: TPM_ALG_SHA1|TPM_ALG_SHA256|TPM_ALG_SHA384
 * @param halg			Scheme hash algorithm			: TPM_ALG_SHA1|TPM_ALG_SHA256|TPM_ALG_SHA384
 * 
 */
void keyedHashPublicTemplate(Create_In *in, TPMI_ALG_HASH nalg, TPMI_ALG_HASH halg);

/** 
 * Creates a template for a sealed data blob (side effect on parameter {in}).
 *
 * @param in			The Create_In object pointer
 * @param nalg			Name hash algorithm				: TPM_ALG_SHA1|TPM_ALG_SHA256|TPM_ALG_SHA384
 * 
 */
void blPublicTemplate(Create_In* in, TPMI_ALG_HASH nalg);


#endif
//OBJECT_TEMPLATE_H