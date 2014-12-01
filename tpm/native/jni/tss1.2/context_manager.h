/*
 * File name: context_manager.h
 * 
 * Purpose: Declaration of a set of functions that handle TPM context
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 *
 *
 */
#ifndef CONTEXT_MANAGER_H
#define CONTEXT_MANAGER_H


#include <math.h>
#include <stdlib.h>
#include <time.h>
#include <stdio.h>

#include <tss/tss_error.h>
#include <tss/platform.h>
#include <tss/tss_defines.h>
#include <tss/tss_typedef.h>
#include <tss/tss_structs.h>
#include <tss/tspi.h>

#include <trousers/trousers.h>


#include "outcome.h"
#include "boolean.h"


#define  MAX_CONTEXT_NUM  256
#define  MAX_UUID_NUM  256


extern int rand_init;


/*********** TYPES DECLARATION **********/

/**
 *
 * Context Data structure
 *
 */
typedef struct context_t {
    // index of the context
    uint8_t index;
    // Handle for the context
    TSS_HCONTEXT hContext;
    // TPM Handle
    TSS_HTPM hTPM;
    // SRK Handle
    TSS_HKEY hSRK;

} Context;


/** 
 * Handle that uniquely identifies a context
 */
typedef uint32_t Handle;

/**
 * Signature:
 *
 *		 24 bits   8 bits
 *    |..........|00000000|
 *
 */
typedef uint32_t Signature;



/*********** CONTEXT FUNCTIONS **********/


/**
 * Constructor for an empty Context
 *
 * @param pointer to the location of the pointer of the new Context 
 */
T_OUTCOME
Context_new(Context** context, uint8_t);


/*********** ARRAYS DECLARATION **********/


// Array of contexts
extern Context* contexts[MAX_CONTEXT_NUM];



// Array of corresponding signatures
extern Signature signatures[MAX_CONTEXT_NUM];


// Array that keeps track of all the registered UUIDs,
// Which are of the form: {0,0,0,0,0,0,{0,0,0,i,j}}
// Where 2 <= i < MAX_UUID_NUM and 0 <= j < MAX_UUID_NUM
extern bool tss_uuids[MAX_UUID_NUM - 2][MAX_UUID_NUM];



/*********** CONTEXT FUNCTIONS **********/

int
get_random_number();


/**
 * Finds the first available free context space in the array
 * 
 * @param index of the next available position for the context
 */
T_OUTCOME
cm_get_free_context_index(uint8_t*);



/**
 * Creates a context and adds it to the contexts array, if there is space;
 * writes the handle for the given context inside the given parameter
 *
 * @param pointer of the handle pointing to the new created context
 *
 */
T_OUTCOME
cm_create_context(Handle*);


/**
 * 
 * Retrieves a Context, given a handle
 *
 * @param pointer to the handle to use
 * @param pointer to the location where writing context's pointer 
 *
 */
T_OUTCOME
cm_get_context(Handle, Context**);


/**
 * Deals with Context deallocation
 *
 * @param pointer to the context Handle
 */
T_OUTCOME
cm_delete_context(Handle);


/************* UUID MANAGEMENT **********/

T_OUTCOME
cm_get_available_uuid(TSS_UUID** uuid);


T_OUTCOME
cm_free_uuid(TSS_UUID* uuid);



T_OUTCOME
cm_load_uuid(TSS_UUID* uuid);



#endif
