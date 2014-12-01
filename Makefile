JNI_PATH=/usr/lib/jvm/java-8-oracle/include/

JNI_TSS1_2_INT_DIR		= src/main/jni/tss1.2
JNI_TSS2_0_INT_DIR		= src/main/jni/tss2.0
JNI_CPABE_INT_DIR		= src/main/jni/cpabe/cpabe

JAVA_SRC_DIR			= src/main/java
JAVA_CLASS_DIR			= target/classes

CC						= /usr/bin/gcc
JNI_CC_FLAGS			= -shared -fPIC -Wimplicit-function-declaration
JNI_TSS2_0_CC_I_FLAGS	= -I$(JNI_PATH) -Iinclude
JNI_TSS2_0_CC_LD_FLAGS	= -DTPM_POSIX -L$(JNI_TSS2_0_INT_DIR)/lib -ltse -lcrypto
JNI_TSS2_0_OUTPUT 		= lib/jni/libtss2_0.so


JNI_TSS1_2_CC_LD_FLAGS	= -ltspi
JNI_TSS1_2_OUTPUT		= lib/jni/libtss1_2.so



default: create_lib_folder tss1_2 tss2_0 cpabe_jni

create_lib_folder:
	if [ ! -d lib ]; then mkdir lib; fi
	if [ ! -d lib/jni ]; then mkdir lib/jni; fi

tss1_2: tss1_2_interface
	$(CC) -I$(JNI_PATH) \
	$(JNI_TSS1_2_INT_DIR)/log.c \
	$(JNI_TSS1_2_INT_DIR)/context_manager.c \
	$(JNI_TSS1_2_INT_DIR)/cert_utils.c \
	$(JNI_TSS1_2_INT_DIR)/safe_encode_decode.c \
	$(JNI_TSS1_2_INT_DIR)/tspi_interface_utils.c \
	$(JNI_TSS1_2_INT_DIR)/tspi_interface_key.c \
	$(JNI_TSS1_2_INT_DIR)/tspi_interface_misc.c \
	$(JNI_TSS1_2_INT_DIR)/tspi_interface_sealing.c \
	$(JNI_TSS1_2_INT_DIR)/tspi_interface_attestation.c \
	$(JNI_TSS1_2_INT_DIR)/com_intel_icecp_node_security_tpm_tpm12.Tss1_2NativeInterface.c $(JNI_CC_FLAGS) -o $(JNI_TSS1_2_OUTPUT) $(JNI_TSS1_2_CC_LD_FLAGS)
tss1_2_interface:
	javah -d $(JNI_TSS1_2_INT_DIR) -cp $(JAVA_CLASS_DIR) -jni com.intel.icecp.node.security.tpm.tpm12.Tss1_2NativeInterface


tss2_0: tss2_0interface
	$(CC) $(JNI_TSS2_0_CC_I_FLAGS) \
	$(JNI_TSS2_0_INT_DIR)/log.c \
	$(JNI_TSS2_0_INT_DIR)/object_template.c \
	$(JNI_TSS2_0_INT_DIR)/tss2_0_interface.c \
	$(JNI_TSS2_0_INT_DIR)/com_intel_icecp_node_security_tpm_tpm20_Tss2_0Nativeinterface.c $(JNI_CC_FLAGS) -o $(JNI_TSS2_0_OUTPUT) $(JNI_TSS2_0_CC_LD_FLAGS)
tss2_0interface:
	javah -d $(JNI_TSS2_0_INT_DIR) -cp $(JAVA_CLASS_DIR) -jni com.intel.icecp.node.security.tpm.tpm20.Tss2_0Nativeinterface

clean:
	rm -f -r lib/jni/*.so
