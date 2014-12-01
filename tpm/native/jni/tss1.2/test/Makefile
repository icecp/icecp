JNI_PATH=..

default: context_manager tspi_interface

context_manager:
	gcc -I$(JNI_PATH) \
	$(JNI_PATH)/log.c \
	$(JNI_PATH)/context_manager.c \
	$(JNI_PATH)/test/context_manager_test.c -o context_manager_test -Wimplicit-function-declaration -ltspi -lcrypto
tspi_interface:
	gcc -I$(JNI_PATH) \
	$(JNI_PATH)/log.c \
	$(JNI_PATH)/cert_utils.c \
	$(JNI_PATH)/context_manager.c \
	$(JNI_PATH)/safe_encode_decode.c \
	$(JNI_PATH)/tspi_interface_utils.c \
	$(JNI_PATH)/tspi_interface_key.c \
	$(JNI_PATH)/tspi_interface_misc.c \
	$(JNI_PATH)/tspi_interface_sealing.c \
	$(JNI_PATH)/tspi_interface_attestation.c \
	$(JNI_PATH)/test/tspi_interface_test.c -o tspi_interface_test -Wimplicit-function-declaration -ltspi -lcrypto -lm
tspi_interface_64:
	gcc -m64 -march=x86-64 -I/home/moreno/Desktop/openssl/include/ -I$(JNI_PATH) \
	$(JNI_PATH)/log.c \
	$(JNI_PATH)/cert_utils.c \
	$(JNI_PATH)/context_manager.c \
	$(JNI_PATH)/safe_encode_decode.c \
	$(JNI_PATH)/tspi_interface_utils.c \
	$(JNI_PATH)/tspi_interface_key.c \
	$(JNI_PATH)/tspi_interface_misc.c \
	$(JNI_PATH)/tspi_interface_sealing.c \
	$(JNI_PATH)/tspi_interface_attestation.c \
	$(JNI_PATH)/test/tspi_interface_test.c -o tspi_interface_test -Wimplicit-function-declaration \
	-L/home/moreno/Desktop/openssl -L/home/moreno/Desktop/TPMSCode/trousers-0.3.13/trousers/lib -ltspi -lcrypto -lm
clean:
	rm -f context_manager_test
	rm -f tspi_interface_test
