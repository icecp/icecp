STORENAME=$1
STOREPASS=$2
KEYALIAS=$3
KEYTYPE=$4
KEYSIZE=$5
KEYPASS=$6



printusage() {

	echo "ERROR, CORRECT USAGE: sh add_secret_key.sh [STORENAME] [STOREPASS] [KEYALIAS] [KEYTYPE] [KEYSIZE] [KEYPASS]"

}


if [ -z $1 ] || [ -z $2 ] || [ -z $3 ] || [ -z $4 ] || [ -z $5 ] || [ -z $6 ]
then
	printusage;
	exit -1;
fi


keytool -genseckey -keystore $STORENAME -alias $KEYALIAS -keyalg $KEYTYPE -keysize $KEYSIZE -keypass $KEYPASS -storepass $STOREPASS -storetype JCEKS