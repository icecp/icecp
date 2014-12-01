/*
 * File name: log.h
 * 
 * Purpose: Declaration of logging functions
 * 
 * © Copyright Intel Corporation. All rights reserved.
 * Intel Corporation, 2200 Mission College Boulevard,
 * Santa Clara, CA 95052-8119, USA
 *
 *
 */
#ifndef LOG_H
#define LOG_H

#include <stdarg.h>

#define DEBUG 1

void
print_log(int line, const char* function, const char* pattern, ...);

void
print_err(int line, const char* function, const char* pattern, ...);


#endif