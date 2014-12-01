#!/bin/bash  
#  
# This script shows the command to be executed in order to import a certificate + private key (in PEM format) into a Java Keystore
#

# Certificate file
CERT=$1	
# Private Key file
PRIV_KEY=$2	
# Destination
DEST_KEYSTORE=$3
# Destination password
DEST_PASSWORD=$4

DEST_ALIAS=$5

if [ -z $1 ] || [ -z $2 ] || [ -z $3 ] || [ -z $4 ] || [ -z $5 ]
then
	echo -e "ERROR! Correct usage is:
		./PEM_to_PKCS12.sh [CERT_FILE] [PRIV_KEY_FILE] [DEST_KEYSTORE] [DEST_PASSWORD] [DEST_ALIAS]
Where:
	- CERT_FILE: 		X509 Certificate File (PEM)				
	- PRIV_KEY_FILE: 	Corresponding private key file (PEM)	
	- DEST_KEYSTORE: 	Destination keystore
	- DEST_PASSWORD: 	Destination keystore password
	- DEST_ALIAS: 		Alias of the key pair in the destinatio keystore
	"
	exit -1
fi


openssl pkcs12 -export -out tmp.pfx -inkey $PRIV_KEY -in $CERT -passin pass:password -passout pass:password && \
keytool -importkeystore -srckeystore tmp.pfx -destkeystore $DEST_KEYSTORE -srcstorepass password -deststorepass $DEST_PASSWORD -srcalias 1 -destalias $DEST_ALIAS -deststoretype JCEKS && \
# cleanup
rm -f tmp.pfx && \
keytool -list -keystore $DEST_KEYSTORE -storepass $DEST_PASSWORD -storetype JCEKS
