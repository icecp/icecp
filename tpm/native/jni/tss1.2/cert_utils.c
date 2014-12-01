/*
 * File name: cert_utils.c
 *  
 * Purpose: Implementation of the functions declared in cert_utils.h
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 *
 *
 */
#include <openssl/bio.h>
#include <openssl/rsa.h>
#include <openssl/pem.h>
#include <openssl/x509v3.h>
#include <openssl/x509.h>

#include "cert_utils.h"

#include "context_manager.h"

T_OUTCOME
cert_utils_bytes_to_x509_cert(
        unsigned char* cert_bytes,
        size_t cer_bytes_len,
        X509** cert) {
    BIO* bio = NULL;
    T_OUTCOME res = SUCCESS;
    // Create a read-only BIO backed by the supplied memory buffer
    bio = BIO_new_mem_buf((void*) cert_bytes, cer_bytes_len);

    if (!bio) {
        return ERROR;
    }

    // If fails, the result will be ERROR
    if (!PEM_read_bio_X509(bio, cert, NULL, NULL)) {
        res = ERROR;
    }

    // Cleanup	

    BIO_free(bio);

    return res;
}

T_OUTCOME
cert_utils_rsa_from_x509_cert(
        X509* cert,
        RSA** rsa) {

    EVP_PKEY* pcaKey;

    pcaKey = X509_get_pubkey(cert);
    if (!pcaKey) {
        return ERROR;
    }


    (*rsa) = EVP_PKEY_get1_RSA(pcaKey);

    if (!(*rsa)) {
        return ERROR;
    }

    return SUCCESS;
}

T_OUTCOME
cert_utils_rsa_pub_key_from_bytes(
        unsigned char* keyBytes,
        size_t keyBytesLen,
        RSA** rsa) {

    T_OUTCOME res = SUCCESS;
    BIO* bufio = BIO_new_mem_buf((void*) keyBytes, keyBytesLen);

    if (!bufio) {
        return ERROR;
    }

    // If fails, the result will be ERROR
    if (!PEM_read_bio_RSA_PUBKEY(bufio, rsa, 0, NULL)) {
        res = ERROR;
    }

    // Cleanup	

    BIO_free(bufio);

    return res;
}

T_OUTCOME
cert_utils_rsa_priv_key_from_bytes(
        unsigned char* keyBytes,
        size_t keyBytesLen,
        RSA** rsa) {

    T_OUTCOME res = SUCCESS;
    BIO* bufio = BIO_new_mem_buf((void*) keyBytes, keyBytesLen);

    if (!bufio) {
        return ERROR;
    }


    // If fails, the result will be ERROR
    if (!PEM_read_bio_RSAPrivateKey(bufio, rsa, 0, NULL)) {
        res = ERROR;
    }

    // Cleanup	

    BIO_free(bufio);


    return res;

}

int
add_extension(X509 *issuer, X509 *subject, int nid, char *value) {
    X509_EXTENSION *ex;
    X509V3_CTX ctx;

    // This sets the 'context' of the extensions.
    X509V3_set_ctx_nodb(&ctx);

    // Issuer and subject certificates, no request and no CRL
    X509V3_set_ctx(&ctx, issuer, subject, NULL, NULL, 0);
    ex = X509V3_EXT_conf_nid(NULL, &ctx, nid, value);
    if (!ex) {
        return 0;
    }

    X509_add_ext(subject, ex, -1);
    X509_EXTENSION_free(ex);
    return 1;
}

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
        EKCertData* ekCertData) // Can be NULL
{

    RSA* publicKey = NULL;
    RSA* signingKey = NULL;
    X509* issuerCert = NULL;

    // Get the RSA objects
    cert_utils_rsa_priv_key_from_bytes(issuerPrivateKey, issuerPrivateKeyLen, &signingKey);
    cert_utils_rsa_pub_key_from_bytes(publicKeyBytes, publicKeyBytesLen, &publicKey);
    cert_utils_bytes_to_x509_cert(issuerCertificateBytes, issuerCertificateBytesLen, &issuerCert);

    if (publicKey == NULL || signingKey == NULL || issuerCert == NULL) {
        return ERROR;
    }

    // use a EVP_PKEY structure
    EVP_PKEY* pkey = EVP_PKEY_new();
    EVP_PKEY_set1_RSA(pkey, publicKey);
    EVP_PKEY* signingKeyEvp = EVP_PKEY_new();
    EVP_PKEY_set1_RSA(signingKeyEvp, signingKey);

    // Free RSA structures
    RSA_free(publicKey);
    RSA_free(signingKey);

    // The certificate
    X509* x509;
    x509 = X509_new();

    // Serial number
    ASN1_INTEGER_set(X509_get_serialNumber(x509), get_random_number());

    // Set version and duration
    X509_set_version(x509, 2);

    // @TODO: Duration is hard coded, replace with parameter
    X509_gmtime_adj(X509_get_notBefore(x509), 0);
    X509_gmtime_adj(X509_get_notAfter(x509), 31536000L);

    // Set the public ket of the certificate to be the one passed in as parameter
    X509_set_pubkey(x509, pkey);


    // @TODO: THIS MUST BE EMPTY FOR BOTH EK AND AIK accordong to TPM 1.2
    // Section 3.2.6 of Credential Profiles v1.2 level 2 - revision 8

    X509_NAME * subjectName;
    subjectName = X509_get_subject_name(x509);

    // ************* @TODO: Fill this with meaningful info...
    X509_NAME_add_entry_by_txt(subjectName, "C", MBSTRING_ASC,
            (unsigned char *) "CA", -1, -1, 0);
    X509_NAME_add_entry_by_txt(subjectName, "O", MBSTRING_ASC,
            (unsigned char *) "MyCompany Inc.", -1, -1, 0);
    X509_NAME_add_entry_by_txt(subjectName, "CN", MBSTRING_ASC,
            (unsigned char *) "localhost", -1, -1, 0);


    X509_set_subject_name(x509, subjectName);





    // Set the issuerName
    X509_NAME* issuerName;

    // Copy the issuer name from the issuing certificate
    issuerName = X509_get_issuer_name(issuerCert);
    X509_set_issuer_name(x509, issuerName);


    // Basic constraints
    // For EK:
    //		- critical:TRUE
    //		- ca:FALSE
    int res = add_extension(issuerCert, x509, NID_basic_constraints, "critical,CA:FALSE");


    // @TODO: NOT COMPLETE...

    // if (ekCertData != NULL)
    // {
    // This should not be included in EK cert
    // res = add_extension(issuerCert, x509, NID_subject_key_identifier, "hash");
    // ** EXTENSION: certificatePolicies
    // For EK:
    // 		- critical 			: TRUE
    //		- policyIdentifier 	: 1.3.234.ADF.1234...   (for example)
    // 		- CPS uri 			: HTTP URL where to find the plain text version of EK's policy
    //		- userNotice 		: "TCPA Trusted Platform Module Endorsement"
    //res = add_extension(issuerCert, x509, NID_certificate_policies,"1.3.6.1.4.1.22");

    // ** EXTENSION: subjectAltNames
    // For EK:
    //		- critical:TRUE,
    //		- family:"1.1"
    //		- level:...
    //		- revision:...
    //res = add_extension(issuerCert, x509, NID_subject_alt_name, "critical");	
    //printf("%i\n", res);
    // }

    // Sign with the private key of the issuer
    X509_sign(x509, signingKeyEvp, EVP_sha1());

    // Write the certificate bytes in PEM into the given parameters
    BIO *bio = BIO_new(BIO_s_mem());
    PEM_write_bio_X509(bio, x509);
    BIO_flush(bio);
    *createdCertificateBytesLen = BIO_pending(bio);
    *createdCertificateBytes = calloc((*createdCertificateBytesLen) + 1, 1);


    // Load the bytes into the given locations
    BIO_read(bio, *createdCertificateBytes, *createdCertificateBytesLen);



    // cleanup


    BIO_free(bio);
    X509_free(issuerCert);


    return SUCCESS;

}

char*
encodeBase64String(
        const char* message,
        size_t messageLen) {
    BIO *bio, *b64;
    char* buffer;
    size_t bufferLen;
    FILE* stream;
    bufferLen = 4 * ceil((double) messageLen / 3);
    buffer = (char *) malloc(bufferLen + 1);

    stream = fmemopen(buffer, bufferLen + 1, "w");
    b64 = BIO_new(BIO_f_base64());
    bio = BIO_new_fp(stream, BIO_NOCLOSE);
    bio = BIO_push(b64, bio);
    BIO_set_flags(bio, BIO_FLAGS_BASE64_NO_NL); //Ignore newlines - write everything in one line
    BIO_write(bio, message, messageLen);
    BIO_flush(bio);
    BIO_free_all(bio);
    fclose(stream);

    return buffer; //success
}
