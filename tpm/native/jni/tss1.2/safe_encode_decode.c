/*
 * File name: safe_encode_decode.c
 * 
 * Purpose: Set of utility functions to unblob data structures 
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 *
 *
 */
#include <stdio.h>
#include <stdlib.h>

#include "safe_encode_decode.h"

T_OUTCOME
unblob_UINT32(UINT64* offset, UINT32* out, BYTE* blob, UINT64 blobSize) {
    // Check if we have 4 bytes to read
    if (blobSize - (*offset + sizeof (UINT32)) < 0) {
        return ERROR;
    }

    Trspi_UnloadBlob_UINT32(offset, out, blob);

    return SUCCESS;
}

T_OUTCOME
unblob_UINT16(UINT64* offset, UINT16* out, BYTE* blob, UINT64 blobSize) {
    // Check if we have 2 bytes to read
    if (blobSize - (*offset + sizeof (UINT16)) < 0) {
        return ERROR;
    }

    Trspi_UnloadBlob_UINT16(offset, out, blob);

    return SUCCESS;
}

T_OUTCOME
unblob_Blob(UINT64 *offset, size_t size, BYTE *from, BYTE *to, UINT64 fromSize) {

    if (fromSize - ((*offset) + size) < 0) {
        return ERROR;
    }

    Trspi_UnloadBlob(offset, size, from, to);

    return SUCCESS;
}

T_OUTCOME
unblob_KEY_PARMS(UINT64 *offset, BYTE *blob, TCPA_KEY_PARMS *keyParms, UINT64 blobSize) {
    if (!keyParms) {
        UINT32 parmSize;

        unblob_UINT32(offset, NULL, blob, blobSize);
        unblob_UINT16(offset, NULL, blob, blobSize);
        unblob_UINT16(offset, NULL, blob, blobSize);
        unblob_UINT32(offset, &parmSize, blob, blobSize);

        (*offset) += parmSize;

        return SUCCESS;
    }

    unblob_UINT32(offset, &keyParms->algorithmID, blob, blobSize);
    unblob_UINT16(offset, &keyParms->encScheme, blob, blobSize);
    unblob_UINT16(offset, &keyParms->sigScheme, blob, blobSize);
    unblob_UINT32(offset, &keyParms->parmSize, blob, blobSize);

    if (keyParms->parmSize > 0) {

        // Check if we have enough space left inside the blob
        if (blobSize - (*offset + keyParms->parmSize) < 0) {
            return ERROR;
        }

        keyParms->parms = malloc(keyParms->parmSize);
        if (keyParms->parms == NULL) {
            return ERROR;
        }
        unblob_Blob(offset, keyParms->parmSize, blob, keyParms->parms, blobSize);
    } else {
        keyParms->parms = NULL;
    }

    return SUCCESS;
}

T_OUTCOME
unblob_STORE_PUBKEY(UINT64 *offset, BYTE *blob, TCPA_STORE_PUBKEY *store, UINT64 blobSize) {
    if (!store) {
        UINT32 keyLength;

        if (!unblob_UINT32(offset, &keyLength, blob, blobSize)) {
            return ERROR;
        }

        if (blobSize - (*offset + keyLength) < 0) {
            return ERROR;
        }

        unblob_Blob(offset, keyLength, blob, NULL, blobSize);

        return SUCCESS;
    }


    if (!unblob_UINT32(offset, &store->keyLength, blob, blobSize)) {
        return ERROR;
    }

    if (store->keyLength > 0) {

        if (blobSize - (*offset + store->keyLength) < 0) {
            return ERROR;
        }

        store->key = malloc(store->keyLength);
        if (store->key == NULL) {
            return ERROR;
        }
        unblob_Blob(offset, store->keyLength, blob, store->key, blobSize);
    } else {
        store->key = NULL;
    }

    return SUCCESS;
}

T_OUTCOME
unblob_SYMMETRIC_KEY(UINT64 *offset, BYTE *blob, TCPA_SYMMETRIC_KEY *key, UINT64 blobSize) {
    if (!key) {

        UINT16 size;

        if (!unblob_UINT32(offset, NULL, blob, blobSize)) {
            return ERROR;
        }
        if (!unblob_UINT16(offset, NULL, blob, blobSize)) {
            return ERROR;
        }
        if (!unblob_UINT16(offset, &size, blob, blobSize)) {
            return ERROR;
        }

        (*offset) += size;

        return SUCCESS;
    }


    if (!unblob_UINT32(offset, &key->algId, blob, blobSize)) {
        return ERROR;
    }
    if (!unblob_UINT16(offset, &key->encScheme, blob, blobSize)) {
        return ERROR;
    }
    if (!unblob_UINT16(offset, &key->size, blob, blobSize)) {
        return ERROR;
    }


    if (key->size > 0) {
        key->data = malloc(key->size);
        if (key->data == NULL) {
            key->size = 0;
            return ERROR;
        }
        if (!unblob_Blob(offset, key->size, blob, key->data, blobSize)) {

            free(key->data);
            key->data = NULL;
            key->size = 0;
            return ERROR;
        }
    } else {
        key->data = NULL;
    }

    return SUCCESS;
}

T_OUTCOME
unblob_PUBKEY(UINT64 *offset, BYTE *blob, TCPA_PUBKEY *pubKey, UINT64 blobSize) {
    if (!pubKey) {
        if (!unblob_KEY_PARMS(offset, blob, NULL, blobSize)) {
            return ERROR;
        }
        if (!unblob_STORE_PUBKEY(offset, blob, NULL, blobSize)) {
            return ERROR;
        }
        return SUCCESS;
    }

    if (!unblob_KEY_PARMS(offset, blob, &pubKey->algorithmParms, blobSize)) {
        return ERROR;
    }

    if (!unblob_STORE_PUBKEY(offset, blob, &pubKey->pubKey, blobSize)) {
        free(pubKey->pubKey.key);
        free(pubKey->algorithmParms.parms);
        pubKey->pubKey.key = NULL;
        pubKey->pubKey.keyLength = 0;
        pubKey->algorithmParms.parms = NULL;
        pubKey->algorithmParms.parmSize = 0;
        return ERROR;
    }

    return SUCCESS;
}

T_OUTCOME
unblob_IDENTITY_REQ(UINT64 *offset, BYTE *blob, TCPA_IDENTITY_REQ *req, UINT64 blobSize) {
    if (!req) {
        return ERROR;
    }

    if (!unblob_UINT32(offset, &req->asymSize, blob, blobSize)) {
        return ERROR;
    }

    if (!unblob_UINT32(offset, &req->symSize, blob, blobSize)) {
        return ERROR;
    }

    if (!unblob_KEY_PARMS(offset, blob, &req->asymAlgorithm, blobSize)) {
        return ERROR;
    }
    if (!unblob_KEY_PARMS(offset, blob, &req->symAlgorithm, blobSize)) {
        free(req->asymAlgorithm.parms);
        req->asymAlgorithm.parmSize = 0;
        return ERROR;
    }

    if (req->asymSize > 0) {

        // Check if we have enough space left inside the blob
        if (blobSize - (*offset + req->asymSize) < 0) {
            req->asymSize = req->asymAlgorithm.parmSize = req->symAlgorithm.parmSize = 0;
            free(req->asymAlgorithm.parms);
            free(req->symAlgorithm.parms);
            return ERROR;
        }

        if ((req->asymBlob = malloc(req->asymSize)) == NULL) {
            req->asymSize = req->asymAlgorithm.parmSize = req->symAlgorithm.parmSize = 0;
            free(req->asymAlgorithm.parms);
            free(req->symAlgorithm.parms);
            return ERROR;
        }
        unblob_Blob(offset, req->asymSize, blob, req->asymBlob, blobSize);
    } else {
        req->asymBlob = NULL;
    }

    if (req->symSize > 0) {

        // Check if we have enough space left inside the blob
        if (blobSize - (*offset + req->symSize) < 0) {
            req->symSize = req->asymSize = req->asymAlgorithm.parmSize = req->symAlgorithm.parmSize = 0;
            free(req->asymBlob);
            req->asymBlob = NULL;   
            free(req->asymAlgorithm.parms);
            free(req->symAlgorithm.parms);
            return ERROR;
        }

        if ((req->symBlob = malloc(req->symSize)) == NULL) {
            req->symSize = req->asymSize = req->asymAlgorithm.parmSize = req->symAlgorithm.parmSize = 0;
            free(req->asymBlob);
            req->asymBlob = NULL;
            free(req->asymAlgorithm.parms);
            free(req->symAlgorithm.parms);
            return ERROR;
        }
        unblob_Blob(offset, req->symSize, blob, req->symBlob, blobSize);
    } else {
        req->symBlob = NULL;
    }

    return SUCCESS;
}