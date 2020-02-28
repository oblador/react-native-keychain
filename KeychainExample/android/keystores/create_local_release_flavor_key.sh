#!/usr/bin/env bash

export JDK=$JAVA_HOME/bin
export flavor=$1
export KEYSTORE=${flavor}.keystore
export PACKAGE=
export PROPS=${PACKAGE}${flavor}.keystore.properties
export COMPANY=personal
STEPS=4

echo

# expected some parameters
if [[ $# -eq 0 ]] ; then
    echo 'Expected flavor name as input parameter. Example:'
    echo
    echo '  ./create_local_release_flavor_key.sh release'
    echo
    exit 1
fi

# prevent second run and destroy of done things.
if [ ! -f $KEYSTORE ]; then
        echo [-/$STEPS] keystore file not found. Generating...
else
    if [[ "$2" == "--force" ]]; then
        echo [-/$STEPS] Forced generated keystore replacement.
        rm $KEYSTORE
    else
        echo WARNING!
        echo "    keystore file is already generated."
        echo "    delete file $KEYSTORE and run the script again, if you want to re-generate it."
        echo
        echo "      rm $KEYSTORE"
        echo OR
        echo "      ./create_local_release_flavor_key.sh $KEYSTORE --force"
        exit 1
    fi
fi

# verify availability of JDK tools
if [ ! -f $JDK/keytool ]; then
    echo ERROR!
    echo "   keytool JDK utility not found. please check JAVA_HOME variable."
    echo JAVA_HOME=$JAVA_HOME
    exit 2
else
    echo [1/$STEPS] keytool found. JAVA_HOME=$JAVA_HOME
fi

# generate new passwords for keystore
##
## https://www.howtogeek.com/howto/30184/10-ways-to-generate-a-random-password-from-the-command-line/
##
if [[ "debug" == "${flavor}" ]]; then
  KEY=android
  PASS=android
  ALIAS=androiddebugkey
else
  KEY=`openssl rand -base64 32`
  PASS=`openssl rand -base64 32`
  ALIAS=$flavor-key
fi

echo [2/$STEPS] generating new secured storepass and keypass for keystore

# compose the new key
$JDK/keytool -genkey -alias $ALIAS -keyalg RSA -validity 20000 \
    -storepass $PASS -keypass $KEY -keystore $KEYSTORE \
    -dname "CN=$COMPANY.com, OU=$COMPANY security, O=$COMPANY, L=$COMPANY, S=Stockholm, C=SE" \
    >/dev/null 2>&1
#    >output.log 2>&1
echo [3/$STEPS] keystore generated: ./$KEYSTORE

if [[ "debug" != "${flavor}" ]]; then
  # compose properties file with configuration
  echo "# generated at $(date +%Y-%m-%d_%H-%M-%S) / $(date +%s)" >$PROPS
  echo "key.store=${KEYSTORE}" >>$PROPS
  echo "key.alias=${ALIAS}" >>$PROPS
  echo "key.store.password=${PASS}" >>$PROPS
  echo "key.alias.password=${KEY}" >>$PROPS
  echo "" >>$PROPS
  echo [4/$STEPS] composed custom properties file: ./$PROPS
else
  echo [4/$STEPS] reused file: ./$PROPS
fi

echo
echo All done!
echo
