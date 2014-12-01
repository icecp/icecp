#ifndef LOG_H
#define LOG_H

#include <stdarg.h>

#define DEBUG 1

void
print_log(int line, const char* function, const char* pattern, ...);

void
print_err(int line, const char* function, const char* pattern, ...);


#endif