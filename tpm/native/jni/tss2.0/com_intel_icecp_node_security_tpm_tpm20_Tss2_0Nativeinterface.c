// Implement this interface
#include "com_intel_icecp_node_security_tpm_tpm20_Tss2_0Nativeinterface.h"


#include <stdlib.h>
#include <stdio.h>


#include "log.h"
#include "outcome.h"
#include "tss2_0_interface.h"


#define  JAVA_TPM_EXCEPTION "com/intel/icecp/core/security/tpm/exception/TpmOperationError"

/**
 * Throws an exception for the Java code, of type JAVA_TPM_EXCEPTION
 * 
 * @param env 		JNI environment
 * @param message 	Template of the message to output
 */
void
throwException(
        JNIEnv* env,
        const char* message, ...) {

    char* buff;
    va_list args;
    va_start(args, message);

    // Determine the size of the buffer
    int size = snprintf(NULL, 0, message, args);
    // If size is 0, return 
    if (!size)
        return;
    // Allocate the buffer
    buff = (char*) malloc(sizeof (char)*size + 1);
    // Compose message string
    vsprintf(buff, message, args);

    jclass exceptionClass = (*env) -> FindClass(
            env,
            JAVA_TPM_EXCEPTION);

    if (exceptionClass) {
        (*env) -> ThrowNew(
                env,
                exceptionClass,
                buff);
    }

    free(buff);

    va_end(args);

}

JNIEXPORT void JNICALL
Java_com_intel_icecp_node_security_tpm_tpm20_Tss2_10Nativeinterface_InitTpm(
        JNIEnv* env,
        jobject obj) {

    T_OUTCOME res = tss_intialize_tpm();

    if (res != SUCCESS) {
        throwException(env, "Unable to initialize the TPM");
    }
}

JNIEXPORT void JNICALL
Java_com_intel_icecp_node_security_tpm_tpm20_Tss2_10Nativeinterface_CreatePrimaryKey(
        JNIEnv* env,
        jobject obj,
        jint jHierarchyType) {
    T_OUTCOME res = tss_create_primary_key(jHierarchyType, "password");

    if (res != SUCCESS) {
        throwException(env, "Unable to initialize hierarchy %i", jHierarchyType);
    }

}


