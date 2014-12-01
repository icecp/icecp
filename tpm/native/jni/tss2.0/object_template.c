/*
 * File name: object_template.c
 * 
 * Purpose: Provides a set of functions to create TPM 2.0 object templates
 * Functions code is taken from the create.c utitlity provided by the ibmtss387
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */
#include "object_template.h"

void asymPublicTemplate(Create_In *in, int keyType, TPMI_ALG_PUBLIC algPublic, TPMI_ECC_CURVE curveID, TPMI_ALG_HASH nalg, TPMI_ALG_HASH halg) {
    /* Table 185 - TPM2B_PUBLIC inPublic */
    /* Table 184 - TPMT_PUBLIC publicArea */
    in->inPublic.t.publicArea.type = algPublic;
    in->inPublic.t.publicArea.nameAlg = nalg;

    /* Table 32 - TPMA_OBJECT objectAttributes */
    in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_SENSITIVEDATAORIGIN;
    in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_USERWITHAUTH;
    in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_ADMINWITHPOLICY;

    switch (keyType) {
        case TYPE_DEN:
        case TYPE_DEO:
            in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_SIGN;
            in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_DECRYPT;
            in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_RESTRICTED;
            break;
        case TYPE_ST:
            in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_SIGN;
            in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_DECRYPT;
            in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_RESTRICTED;
            break;
        case TYPE_SI:
            in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_SIGN;
            in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_DECRYPT;
            in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_RESTRICTED;
            break;
        case TYPE_SIR:
            in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_SIGN;
            in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_DECRYPT;
            in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_RESTRICTED;
            break;
        case TYPE_GP:
            in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_SIGN;
            in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_DECRYPT;
            in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_RESTRICTED;
            break;
    }

    /* Table 72 -  TPM2B_DIGEST authPolicy */
    /* policy set separately */

    /* Table 182 - Definition of TPMU_PUBLIC_PARMS parameters */
    if (algPublic == TPM_ALG_RSA) {
        /* Table 180 - Definition of {RSA} TPMS_RSA_PARMS rsaDetail */
        /* Table 129 - Definition of TPMT_SYM_DEF_OBJECT Structure symmetric */
        switch (keyType) {
            case TYPE_DEN:
            case TYPE_DEO:
            case TYPE_SI:
            case TYPE_SIR:
            case TYPE_GP:
                /* Non-storage keys must have TPM_ALG_NULL for the symmetric algorithm */
                in->inPublic.t.publicArea.parameters.rsaDetail.symmetric.algorithm = TPM_ALG_NULL;
                break;
            case TYPE_ST:
                in->inPublic.t.publicArea.parameters.rsaDetail.symmetric.algorithm = TPM_ALG_AES;
                /* Table 125 - TPMU_SYM_KEY_BITS keyBits */
                in->inPublic.t.publicArea.parameters.rsaDetail.symmetric.keyBits.aes = 128;
                /* Table 126 - TPMU_SYM_MODE mode */
                in->inPublic.t.publicArea.parameters.rsaDetail.symmetric.mode.aes = TPM_ALG_CBC;
                break;
        }

        /* Table 155 - Definition of {RSA} TPMT_RSA_SCHEME scheme */
        switch (keyType) {
            case TYPE_DEN:
            case TYPE_GP:
            case TYPE_ST:
            case TYPE_SI:
                in->inPublic.t.publicArea.parameters.rsaDetail.scheme.scheme = TPM_ALG_NULL;
                break;
            case TYPE_DEO:
                in->inPublic.t.publicArea.parameters.rsaDetail.scheme.scheme = TPM_ALG_OAEP;
                /* Table 152 - Definition of TPMU_ASYM_SCHEME details */
                /* Table 152 - Definition of TPMU_ASYM_SCHEME rsassa */
                /* Table 142 - Definition of {RSA} Types for RSA Signature Schemes */
                /* Table 135 - Definition of TPMS_SCHEME_HASH hashAlg */
                in->inPublic.t.publicArea.parameters.rsaDetail.scheme.details.oaep.hashAlg = halg;
                break;
            case TYPE_SIR:
                in->inPublic.t.publicArea.parameters.rsaDetail.scheme.scheme = TPM_ALG_RSASSA;
                /* Table 152 - Definition of TPMU_ASYM_SCHEME details */
                /* Table 152 - Definition of TPMU_ASYM_SCHEME rsassa */
                /* Table 142 - Definition of {RSA} Types for RSA Signature Schemes */
                /* Table 135 - Definition of TPMS_SCHEME_HASH hashAlg */
                in->inPublic.t.publicArea.parameters.rsaDetail.scheme.details.rsassa.hashAlg = halg;
                break;
        }

        /* Table 159 - Definition of {RSA} (TPM_KEY_BITS) TPMI_RSA_KEY_BITS Type keyBits */
        in->inPublic.t.publicArea.parameters.rsaDetail.keyBits = 2048;
        in->inPublic.t.publicArea.parameters.rsaDetail.exponent = 0;
        /* Table 177 - TPMU_PUBLIC_ID unique */
        /* Table 177 - Definition of TPMU_PUBLIC_ID */
        in->inPublic.t.publicArea.unique.rsa.t.size = 0;
    } else /* algPublic == TPM_ALG_ECC */ {
        /* Table 181 - Definition of {ECC} TPMS_ECC_PARMS Structure eccDetail */
        /* Table 129 - Definition of TPMT_SYM_DEF_OBJECT Structure symmetric */
        switch (keyType) {
            case TYPE_DEN:
            case TYPE_DEO:
            case TYPE_SI:
            case TYPE_SIR:
            case TYPE_GP:
                /* Non-storage keys must have TPM_ALG_NULL for the symmetric algorithm */
                in->inPublic.t.publicArea.parameters.eccDetail.symmetric.algorithm = TPM_ALG_NULL;
                break;
            case TYPE_ST:
                in->inPublic.t.publicArea.parameters.eccDetail.symmetric.algorithm = TPM_ALG_AES;
                /* Table 125 - TPMU_SYM_KEY_BITS keyBits */
                in->inPublic.t.publicArea.parameters.eccDetail.symmetric.keyBits.aes = 128;
                /* Table 126 - TPMU_SYM_MODE mode */
                in->inPublic.t.publicArea.parameters.eccDetail.symmetric.mode.aes = TPM_ALG_CBC;
                break;
        }
        /* Table 166 - Definition of (TPMT_SIG_SCHEME) {ECC} TPMT_ECC_SCHEME Structure scheme */
        /* Table 164 - Definition of (TPM_ALG_ID) {ECC} TPMI_ALG_ECC_SCHEME Type scheme */
        switch (keyType) {
            case TYPE_GP:
            case TYPE_SI:
                in->inPublic.t.publicArea.parameters.eccDetail.scheme.scheme = TPM_ALG_NULL;
                /* Table 165 - Definition of {ECC} (TPM_ECC_CURVE) TPMI_ECC_CURVE Type */
                /* Table 10 - Definition of (UINT16) {ECC} TPM_ECC_CURVE Constants <IN/OUT, S> curveID */
                in->inPublic.t.publicArea.parameters.eccDetail.curveID = curveID;
                /* Table 150 - Definition of TPMT_KDF_SCHEME Structure kdf */
                /* Table 64 - Definition of (TPM_ALG_ID) TPMI_ALG_KDF Type */
                in->inPublic.t.publicArea.parameters.eccDetail.kdf.scheme = TPM_ALG_NULL;
                break;
            case TYPE_SIR:
                in->inPublic.t.publicArea.parameters.eccDetail.scheme.scheme = TPM_ALG_ECDSA;
                /* Table 152 - Definition of TPMU_ASYM_SCHEME details */
                /* Table 143 - Definition of {ECC} Types for ECC Signature Schemes */
                in->inPublic.t.publicArea.parameters.eccDetail.scheme.details.ecdsa.hashAlg = halg;
                /* Table 165 - Definition of {ECC} (TPM_ECC_CURVE) TPMI_ECC_CURVE Type */
                /* Table 10 - Definition of (UINT16) {ECC} TPM_ECC_CURVE Constants <IN/OUT, S> curveID */
                in->inPublic.t.publicArea.parameters.eccDetail.curveID = curveID;
                /* Table 150 - Definition of TPMT_KDF_SCHEME Structure kdf */
                /* Table 64 - Definition of (TPM_ALG_ID) TPMI_ALG_KDF Type */
                in->inPublic.t.publicArea.parameters.eccDetail.kdf.scheme = TPM_ALG_NULL;
                /* Table 149 - Definition of TPMU_KDF_SCHEME Union <IN/OUT, S> */
                /* Table 148 - Definition of Types for KDF Schemes, hash-based key- or mask-generation functions */
                /* Table 135 - Definition of TPMS_SCHEME_HASH Structure hashAlg */
                in->inPublic.t.publicArea.parameters.eccDetail.kdf.details.mgf1.hashAlg = halg;
                break;
            case TYPE_DEN:
            case TYPE_DEO:
                /* FIXME keys other than signing are wrong, not implemented yet */
                in->inPublic.t.publicArea.parameters.rsaDetail.scheme.scheme = TPM_ALG_NULL;
                /* Table 152 - Definition of TPMU_ASYM_SCHEME details */
                break;
            case TYPE_ST:
                /* FIXME keys other than signing are wrong, not implemented yet */
                in->inPublic.t.publicArea.parameters.rsaDetail.scheme.scheme = TPM_ALG_NULL;
                break;
        }
        /* Table 177 - TPMU_PUBLIC_ID unique */
        /* Table 177 - Definition of TPMU_PUBLIC_ID */
        in->inPublic.t.publicArea.unique.ecc.x.t.size = 0;
        in->inPublic.t.publicArea.unique.ecc.y.t.size = 0;
    }
}

void symmetricCipherTemplate(Create_In *in, TPMI_ALG_HASH nalg, int keySize, int rev116) {
    /* Table 185 - TPM2B_PUBLIC inPublic */
    /* Table 184 - TPMT_PUBLIC publicArea */
    {
        in->inPublic.t.publicArea.type = TPM_ALG_SYMCIPHER;
        in->inPublic.t.publicArea.nameAlg = nalg;
        /* Table 32 - TPMA_OBJECT objectAttributes */
        /* rev 116 used DECRYPT for both decrypt and encrypt.  After 116, encrypt required SIGN */
        if (!rev116) {
            in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_SIGN; /* actually encrypt */
        }
        in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_DECRYPT;
        in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_RESTRICTED;
        in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_SENSITIVEDATAORIGIN;
        in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_USERWITHAUTH;
        in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_ADMINWITHPOLICY;
        /* Table 72 -  TPM2B_DIGEST authPolicy */
        /* policy set separately */
        /* Table 182 - Definition of TPMU_PUBLIC_PARMS parameters */
        {
            /* Table 131 - Definition of TPMS_SYMCIPHER_PARMS symDetail */
            {
                /* Table 129 - Definition of TPMT_SYM_DEF_OBJECT sym */
                /* Table 62 - Definition of (TPM_ALG_ID) TPMI_ALG_SYM_OBJECT Type */
                in->inPublic.t.publicArea.parameters.symDetail.sym.algorithm = TPM_ALG_AES;
                /* Table 125 - Definition of TPMU_SYM_KEY_BITS Union */
                in->inPublic.t.publicArea.parameters.symDetail.sym.keyBits.aes = keySize;
                /* Table 126 - Definition of TPMU_SYM_MODE Union */
                in->inPublic.t.publicArea.parameters.symDetail.sym.mode.aes = TPM_ALG_CBC;
            }
        }
        /* Table 177 - TPMU_PUBLIC_ID unique */
        /* Table 72 - Definition of TPM2B_DIGEST Structure */
        in->inPublic.t.publicArea.unique.sym.t.size = 0;
    }
    return;
}

void keyedHashPublicTemplate(Create_In *in, TPMI_ALG_HASH nalg, TPMI_ALG_HASH halg) {
    /* Table 185 - TPM2B_PUBLIC inPublic */
    /* Table 184 - TPMT_PUBLIC publicArea */
    {
        /* Table 176 - Definition of (TPM_ALG_ID) TPMI_ALG_PUBLIC Type */
        in->inPublic.t.publicArea.type = TPM_ALG_KEYEDHASH;
        /* Table 59 - Definition of (TPM_ALG_ID) TPMI_ALG_HASH Type  */
        in->inPublic.t.publicArea.nameAlg = nalg;
        /* Table 32 - TPMA_OBJECT objectAttributes */
        in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_SIGN;
        in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_DECRYPT;
        in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_RESTRICTED;
        in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_SENSITIVEDATAORIGIN;
        in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_USERWITHAUTH;
        in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_ADMINWITHPOLICY;
        /* Table 72 -  TPM2B_DIGEST authPolicy */
        /* policy set separately */
        {
            /* Table 182 - Definition of TPMU_PUBLIC_PARMS Union <IN/OUT, S> */
            /* Table 178 - Definition of TPMS_KEYEDHASH_PARMS Structure */
            /* Table 141 - Definition of TPMT_KEYEDHASH_SCHEME Structure */
            /* Table 137 - Definition of (TPM_ALG_ID) TPMI_ALG_KEYEDHASH_SCHEME Type */
            in->inPublic.t.publicArea.parameters.keyedHashDetail.scheme.scheme = TPM_ALG_HMAC;
            /* Table 140 - Definition of TPMU_SCHEME_KEYEDHASH Union <IN/OUT, S> */
            /* Table 138 - Definition of Types for HMAC_SIG_SCHEME */
            /* Table 135 - Definition of TPMS_SCHEME_HASH Structure */
            in->inPublic.t.publicArea.parameters.keyedHashDetail.scheme.details.hmac.hashAlg = halg;
        }
        /* Table 177 - TPMU_PUBLIC_ID unique */
        /* Table 72 - Definition of TPM2B_DIGEST Structure */
        in->inPublic.t.publicArea.unique.sym.t.size = 0;
    }
}

void blPublicTemplate(Create_In *in, TPMI_ALG_HASH nalg) {
    /* Table 185 - TPM2B_PUBLIC inPublic */
    /* Table 184 - TPMT_PUBLIC publicArea */
    {
        /* Table 176 - Definition of (TPM_ALG_ID) TPMI_ALG_PUBLIC Type */
        in->inPublic.t.publicArea.type = TPM_ALG_KEYEDHASH;
        /* Table 59 - Definition of (TPM_ALG_ID) TPMI_ALG_HASH Type  */
        in->inPublic.t.publicArea.nameAlg = nalg;
        /* Table 32 - TPMA_OBJECT objectAttributes */
        in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_SIGN;
        in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_DECRYPT;
        in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_RESTRICTED;
        in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_SENSITIVEDATAORIGIN;
        in->inPublic.t.publicArea.objectAttributes |= TPMA_OBJECT_USERWITHAUTH;
        in->inPublic.t.publicArea.objectAttributes &= ~TPMA_OBJECT_ADMINWITHPOLICY;
        /* Table 72 -  TPM2B_DIGEST authPolicy */
        /* policy set separately */
        {
            /* Table 182 - Definition of TPMU_PUBLIC_PARMS Union <IN/OUT, S> */
            /* Table 178 - Definition of TPMS_KEYEDHASH_PARMS Structure */
            /* Table 141 - Definition of TPMT_KEYEDHASH_SCHEME Structure */
            /* Table 137 - Definition of (TPM_ALG_ID) TPMI_ALG_KEYEDHASH_SCHEME Type */
            in->inPublic.t.publicArea.parameters.keyedHashDetail.scheme.scheme = TPM_ALG_NULL;
            /* Table 140 - Definition of TPMU_SCHEME_KEYEDHASH Union <IN/OUT, S> */
        }
    }
    /* Table 177 - TPMU_PUBLIC_ID unique */
    /* Table 72 - Definition of TPM2B_DIGEST Structure */
    in->inPublic.t.publicArea.unique.sym.t.size = 0;
}