#!/usr/bin/env bash

set -x

rm -rf node_modules/react-native-keychain/.gradle
rm -rf node_modules/react-native-keychain/gradle
rm -rf node_modules/react-native-keychain/.idea
rm -rf node_modules/react-native-keychain/.git
rm -rf node_modules/react-native-keychain/KeychainExample
rm -rf node_modules/react-native-keychain/node_modules
rm -rf node_modules/react-native-keychain/android/build
find . -type f -name *.iml -delete

# Force AndroidX for all RN modules
yarn android:x
