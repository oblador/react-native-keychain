#!/usr/bin/env bash

set -x # Comment for disabling debugging

rm -rf node_modules/react-native-keychain/.gradle
rm -rf node_modules/react-native-keychain/gradle
rm -rf node_modules/react-native-keychain/gradlew
rm -rf node_modules/react-native-keychain/gradlew.bat
rm -rf node_modules/react-native-keychain/.travis.yml
rm -rf node_modules/react-native-keychain/.idea
rm -rf node_modules/react-native-keychain/.git
rm -rf node_modules/react-native-keychain/.vscode
rm -rf node_modules/react-native-keychain/.github
rm -rf node_modules/react-native-keychain/KeychainExample
rm -rf node_modules/react-native-keychain/node_modules
rm -rf node_modules/react-native-keychain/android/build
rm -rf node_modules/react-native-keychain/android/.settings
rm -rf node_modules/react-native-keychain/build
# We install react-native-keychain from file (parent folder). This leads us to copying
# the node_modules folder of the parent folder. When running react-native metro seems
# to resolve dependencies from the copied node_modules folder and this leads to problems
# when running the app. That's why we delete the node_modules here
rm -rf node_modules/react-native-keychain/node_modules

find . -type f -name *.iml -delete

# Force AndroidX for all RN modules
yarn android:x
