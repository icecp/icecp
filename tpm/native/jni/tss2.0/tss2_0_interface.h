/*
 * File name: tss2_0_interface.h
 * 
 * Purpose: Provides an interface to the TPM tss code.
 *  
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 */
#ifndef TSS2_0_INTERFACE_H
#define TSS2_0_INTERFACE_H

#include "include/BaseTypes.h"  // for UINT64 and other base TSS types

#include "outcome.h"    // for T_OUTCOME


T_OUTCOME
tss_intialize_tpm();



T_OUTCOME
tss_create_primary_key(UINT64 primaryHandle, const char* password);




#endif


