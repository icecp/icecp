#include <stdio.h>
#include "../log.h"
#include "../boolean.h"

#include "../context_manager.h"

#include <stdarg.h>

void
print_error_and_terminate(const char* pattern, ...) {
    if (!DEBUG) {
        return;
    }

    va_list args;

    va_start(args, pattern);
    printf("TEST ERROR: (Line %d, %s) ", __LINE__, __func__);
    vfprintf(stdout, pattern, args);
    fputc('\n', stdout);
    va_end(args);

    exit(-1);
}

void
cm_get_free_context_index_test(uint8_t expected_value) {
    bool result = TRUE;

    uint8_t index = 0;

    int j = 0;

    cm_get_free_context_index(&index);
    if (index != expected_value) {
        print_error_and_terminate("Invalid index, received %i instead of %i.", (index && 0xFF), expected_value);
    }
}


Handle handles[256];

void
cm_create_and_delete_context_test() {
    int j = 0;
    // Create 256 contexts, and for each 
    // we verify that the handle is correct
    for (; j < 256; j++) {
        cm_get_free_context_index_test(j);
        uint8_t result = cm_create_context(&handles[j]);

        if (result == ERROR || (handles[j] & 0xFF) != j) {
            print_error_and_terminate("Invalid handle, last byte value is %i instead of %i.", (handles[j] & 0xFF), j);
        }
    }

    // We now should not be able to create other contexts
    uint8_t result = cm_create_context(&handles[j]);

    if (result == SUCCESS) {
        print_error_and_terminate("Created 257th context.");
    }
}

void
cm_get_context_test() {
    int j;
    Context* context;
    for (j = 0; j < 256; j++) {
        context = NULL;
        int res = cm_get_context(handles[j], &context);
        if (res != SUCCESS || context == NULL) {
            print_error_and_terminate("Unable to retrieve context with handle %i.", handles[j]);
        }
    }

    // Test access with fake handle
    Handle fake_handle = rand() & 0xFFFFFF00;
    fake_handle = fake_handle + 10;

    context = NULL;
    int res = cm_get_context(handles[j], &context);
    if (res == SUCCESS || context != NULL) {
        print_error_and_terminate("Invalid access to context at index %i.", (fake_handle & 0xFF));
    }
}

void
cm_delete_context_test() {
    int j;
    for (j = 0; j < 256; j++) {
        cm_delete_context(handles[j]);
    }

    // The expected first free content index should be 0
    cm_get_free_context_index_test(0);
}

void
create_and_delete_uuid_test() {
    TSS_UUID* uuid = NULL;
    cm_get_available_uuid(&uuid);

    if (uuid == NULL) {
        print_error_and_terminate("NULL UUID returned!");
    }


    int i, j;

    i = uuid -> rgbNode[4];
    j = uuid -> rgbNode[5];

    // Now delete the UUID
    cm_free_uuid(uuid);
    free(uuid);

    cm_get_available_uuid(&uuid);

    if (i != uuid -> rgbNode[4] || j != uuid -> rgbNode[5]) {
        print_error_and_terminate("Invalid UUID created!");
    }

    // Now try to load a UUID

    TSS_UUID uuid2 = {0, 0, 0, 0, 0,
        {0, 0, 0, 0, 3, 55}};

    cm_load_uuid(&uuid2);


    if (!tss_uuids[3 - 2][55]) {
        print_error_and_terminate("UUID not loaded properly!");
    }

    cm_free_uuid(&uuid2);


    if (tss_uuids[3 - 2][55]) {
        print_error_and_terminate("UUID not deleted properly!");
    }

}

int main(int argc, char* argv) {

    // Test context creation
    cm_create_and_delete_context_test();

    cm_get_context_test();

    cm_delete_context_test();

    create_and_delete_uuid_test();

    return 0;
}