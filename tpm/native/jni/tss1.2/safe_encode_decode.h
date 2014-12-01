/*
 * File name: safe_encode_decode.h
 * 
 * Purpose: Set of utility functions to unblob data structures 
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 *
 *
 */
#ifndef SAFE_ENCODE_DECODE_H
#define SAFE_ENCODE_DECODE_H


#include <tss/tss_error.h>
#include <tss/platform.h>
#include <tss/tss_defines.h>
#include <tss/tss_typedef.h>
#include <tss/tss_structs.h>
#include <tss/tspi.h>
#include <trousers/trousers.h>

#include "outcome.h"


T_OUTCOME
unblob_UINT32(UINT64* offset, UINT32* out, BYTE* blob, UINT64 blobSize);

T_OUTCOME
unblob_UINT16(UINT64* offset, UINT16* out, BYTE* blob, UINT64 blobSize);

T_OUTCOME
unblob_Blob(UINT64 *offset, size_t size, BYTE *from, BYTE *to, UINT64 fromSize);

T_OUTCOME
unblob_IDENTITY_REQ(UINT64 *offset, BYTE *blob, TCPA_IDENTITY_REQ *req, UINT64 blobSize);

T_OUTCOME
unblob_SYMMETRIC_KEY(UINT64 *offset, BYTE *blob, TCPA_SYMMETRIC_KEY *key, UINT64 blobSize);

T_OUTCOME
unblob_PUBKEY(UINT64 *offset, BYTE *blob, TCPA_PUBKEY *pubKey, UINT64 blobSize);

T_OUTCOME
unblob_KEY_PARMS(UINT64 *offset, BYTE *blob, TCPA_KEY_PARMS *keyParms, UINT64 blobSize);

T_OUTCOME
unblob_STORE_PUBKEY(UINT64 *offset, BYTE *blob, TCPA_STORE_PUBKEY *store, UINT64 blobSize);


#endif