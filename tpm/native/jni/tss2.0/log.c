
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
