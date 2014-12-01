/*
 * File name: log.c
 * 
 * Purpose: Implementation of the logging functions exposed by log.h
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 *
 *
 */
#include "log.h"
#include <stdio.h>

void
print_log(int line, const char* function, const char* pattern, ...) {

    if (!DEBUG) {
        return;
    }

    va_list args;

    va_start(args, pattern);
    printf("Debug: (Line %d, %s) ", line, function);
    vfprintf(stdout, pattern, args);
    fputc('\n', stdout);
    va_end(args);
}

void
print_err(int line, const char* function, const char* pattern, ...) {
    if (!DEBUG) {
        return;
    }

    va_list args;

    va_start(args, pattern);
    printf("Error: (Line %d, %s) ", line, function);
    vfprintf(stdout, pattern, args);
    fputc('\n', stdout);
    va_end(args);
}
