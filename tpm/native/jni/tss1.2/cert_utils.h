/*
 * File name: cert_utils.h 
 *  
 * Purpose: Delaration of some utility functions that allows working with X509 certificates and RSA keys
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 *
 *
 */
#ifndef CERT_UTILS_H
#define CERT_UTILS_H


#include <openssl/rsa.h>

#include "outcome.h"


// Basic constraints
// For EK:
//		- critical:TRUE
//		- ca:FALSE
// ** EXTENSION: certificatePolicies
// For EK:
// 		- critical : TRUE
//		- policyIdentifier : 1.3.234.ADF.1234...   (for example)
// 		- CPS uri (NID_id_qt_cps) : HTTP URL where to find the plain text version of EK's policy
//		- userNotice (NID_id_qt_unotice):"TCPA Trusted Platform Module Endorsement"
// For EK:
//		- critical:TRUE,
//		- family:"1.1"
//		- level:...
//		- revision:...

typedef struct tEKCertData {
    const char* basicConstraints;
    const char* certificatePolicies;
    const char* subjectAltNames;
} EKCertData;



T_OUTCOME
cert_utils_bytes_to_x509_cert(
        unsigned char*,
        size_t,
        X509**);


T_OUTCOME
cert_utils_rsa_from_x509_cert(
        X509*,
        RSA**);


T_OUTCOME
cert_utils_rsa_pub_key_from_bytes(
        unsigned char* keyBytes,
        size_t keyBytesLen,
        RSA** rsa);


T_OUTCOME
cert_utils_rsa_priv_key_from_bytes(
        unsigned char* keyBytes,
        size_t keyBytesLen,
        RSA** rsa);



T_OUTCOME
cert_utils_create_certificate_from_public_key(
        unsigned char* publicKeyBytes,
        size_t publicKeyBytesLen,
        unsigned char* issuerPrivateKey,
        size_t issuerPrivateKeyLen,
        unsigned char* issuerCertificateBytes,
        size_t issuerCertificateBytesLen,
        unsigned char** createdCertificateBytes,
        size_t* createdCertificateBytesLen,
        EKCertData* ekCertData);



char*
encodeBase64String(
        const char* message,
        size_t messageLen);



#endif