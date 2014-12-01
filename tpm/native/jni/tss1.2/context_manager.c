/*
 * File name: context_manager.c
 * 
 * Purpose: Implementation of the TPM context management functionalities exposed by context_manager.h
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 *
 *
 */

#include "context_manager.h"

#include "log.h"


// Array of contexts
Context* contexts[MAX_CONTEXT_NUM];

// Corresponding signatures
Signature signatures[MAX_CONTEXT_NUM];

// Registered keys UUIDs
bool tss_uuids[MAX_UUID_NUM - 2][MAX_UUID_NUM];

// Tells if the random seed has been initialized
int rand_init;

T_OUTCOME
Context_new(Context** context, uint8_t index) {
    // Create a context, initialize all fields to 0
    (*context) = (Context*) malloc(sizeof (Context));
    (*context) -> index = index;
    (*context) -> hContext = 0;
    (*context) -> hTPM = 0;
    (*context) -> hSRK = 0;

    return SUCCESS;
}

/**
 * Utility function that retrieves the rightmost byte from the given handle
 *
 * @param pointer to a handle
 * @param pointer to the rightmost byte holder
 * 
 */
T_OUTCOME
rightmost_byte(Handle h, uint8_t* byte) {
    if (byte) {
        (*byte) = (h) & 0xFF;
        return SUCCESS;
    }
    return ERROR;
}

T_OUTCOME
cm_get_free_context_index(uint8_t* index) {
    int i;
    for (i = 0; i < (sizeof (contexts) / sizeof (contexts[0])); i++) {
        // Check if space i is free
        if (contexts[i] == NULL) {
            (*index) = i;
            return SUCCESS;
        }
    }
    return ERROR;
}

/**
 * Returns a random integer
 */
int
get_random_number() {
    if (!rand_init) {
        srand(time(NULL));
        rand_init = 1;
    }
    return rand();
}

T_OUTCOME
cm_create_context(Handle* h) {

    uint8_t index;

    // Get the next free index if any
    if (cm_get_free_context_index(&index)) {
        // Free spot into the context
        // Keep only the first 24 bits
        (*h) = (get_random_number() & 0xFFFFFF00) + index;

        // Create an instance of a Context data structure
        if (Context_new(&contexts[index], index)) {
            // Set the signature to the leftmost 24 bits
            signatures[index] = (*h) & 0xFFFFFF00;
            // return
            return SUCCESS;
        }

    }

    return ERROR;


}

T_OUTCOME
cm_get_context(Handle h, Context** context) {
    uint8_t index;
    // Retrieve the rightmost byte from the handle
    // corresponding to context's index
    rightmost_byte(h, &index);

    if (0 <= index < (sizeof (contexts) / sizeof (contexts[0])) && contexts[index] != NULL) {
        // Retrieve the context
        Context* c = contexts[index];

        // Retrieve the signature part from the handle
        Signature s = (h) & 0xFFFFFF00;



        if (signatures[index] == s) {
            (*context) = c;
            // Return SUCCESS
            return SUCCESS;
        }
    }

    print_err(__LINE__, __func__, "Unable to load context with handle %i", h);

    return ERROR;
}

T_OUTCOME
cm_delete_context(Handle h) {
    Context* context;
    // Retrieve the corresponding context, if exists,
    // and if the signature is valid
    if (cm_get_context(h, &context)) {

        // Clean up TPM allocated resources
        Tspi_Context_CloseObject(context -> hContext, context -> hTPM);
        Tspi_Context_CloseObject(context -> hContext, context -> hSRK);
        Tspi_Context_Close(context -> hContext);


        // Delete the allocated context data structure
        free(context);

        // Index of the context in the array
        uint8_t index;
        rightmost_byte(h, &index);

        // Free the position occupied by the context
        contexts[index] = NULL;

        // Return SUCCESS code
        return SUCCESS;
    }

    return ERROR;

}

/**
 * Debug functions that outputs the contexts to stdout
 */
void
print_contexts() {
    uint8_t i = 0;
    for (; i < 10; i++) {
        printf("%i\t", (int) contexts[i]);
    }
    printf("\n");
}

T_OUTCOME
uuid_new(
        TSS_UUID** uuid) {
    (*uuid) = (TSS_UUID*) malloc(sizeof (TSS_UUID));
    (*uuid) -> ulTimeLow = 0;
    (*uuid) -> usTimeMid = 0;
    (*uuid) -> usTimeHigh = 0;
    (*uuid) -> bClockSeqHigh = 0;
    (*uuid) -> bClockSeqLow = 0;
    (*uuid) -> rgbNode[0] = 0;
    (*uuid) -> rgbNode[1] = 0;
    (*uuid) -> rgbNode[2] = 0;
    (*uuid) -> rgbNode[3] = 0;

    return SUCCESS;
}

T_OUTCOME
uuid_copy(
        TSS_UUID** uuid,
        TSS_UUID* to_copy) {
    (*uuid) = (TSS_UUID*) malloc(sizeof (TSS_UUID));
    (*uuid) -> ulTimeLow = to_copy -> ulTimeLow;
    (*uuid) -> usTimeMid = to_copy -> usTimeMid;
    (*uuid) -> usTimeHigh = to_copy -> usTimeHigh;
    (*uuid) -> bClockSeqHigh = to_copy -> bClockSeqHigh;
    (*uuid) -> bClockSeqLow = to_copy -> bClockSeqLow;
    (*uuid) -> rgbNode[0] = to_copy -> rgbNode[0];
    (*uuid) -> rgbNode[1] = to_copy -> rgbNode[1];
    (*uuid) -> rgbNode[2] = to_copy -> rgbNode[2];
    (*uuid) -> rgbNode[3] = to_copy -> rgbNode[3];
    (*uuid) -> rgbNode[4] = to_copy -> rgbNode[4];
    (*uuid) -> rgbNode[5] = to_copy -> rgbNode[5];

    return SUCCESS;
}

T_OUTCOME
cm_load_uuid(TSS_UUID* uuid) {
    if (!uuid) {
        return ERROR;
    }

    // Set the UUID as occupied
    if (uuid -> rgbNode[4] >= 2) {
        tss_uuids[uuid -> rgbNode[4] - 2][uuid -> rgbNode[5]] = TRUE;
        print_log(__LINE__, __func__, "Registered UUID: {0,0,0,0,%i,%i}", uuid -> rgbNode[4], uuid -> rgbNode[5]);
        return SUCCESS;
    }
    return ERROR;
}

T_OUTCOME
cm_get_available_uuid(TSS_UUID** uuid) {

    print_log(__LINE__, __func__, "Looking for an availeble UUID");


    // look for an available uuid
    int i, j;
    for (i = 2; i < (sizeof (tss_uuids[0]) / sizeof (tss_uuids[0][0])); i++) {
        for (j = 0; j < (sizeof (tss_uuids[0]) / sizeof (tss_uuids[0][0])); j++) {
            if (tss_uuids[i - 2][j] == FALSE) {
                // We've found a free UUID: {0,0,0,0,i,j}
                print_log(__LINE__, __func__, "Found an availeble UUID: {0,0,0,0,%i,%i}", i, j);

                // Set the UUID as used
                tss_uuids[i - 2][j] = TRUE;
                // Allocate a new UUID
                uuid_new(uuid);

                (*uuid) -> rgbNode[4] = i;
                (*uuid) -> rgbNode[5] = j;


                return SUCCESS;
            }
        }

    }

    print_err(__LINE__, __func__, "No UUID available");

    return ERROR;
}

T_OUTCOME
cm_free_uuid(TSS_UUID* uuid) {

    if (!uuid) {
        return ERROR;
    }

    if (tss_uuids[uuid -> rgbNode[4] - 2][uuid -> rgbNode[5]]) {
        // Set it free on the global array
        tss_uuids[uuid -> rgbNode[4] - 2][uuid -> rgbNode[5]] = FALSE;
        print_log(__LINE__, __func__, "Freed UUID: {0,0,0,0,%i,%i}", uuid -> rgbNode[4], uuid -> rgbNode[5]);

        return SUCCESS;
    }

    return ERROR;

}