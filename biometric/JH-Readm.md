This library is being added so that we can force android to only show the fingerprint prompt.
Without this it will show the facial recognition option but then immediately fail due to the security level not being class 3
Unfortunately at the time of this writing there isn't a newer stable version of the library that has the capaibility of detecting
if facial recognition should be ignored due to it being insecure.

How to build:
From the android directory

./gradlew :biometric:assemble

Next locate the biometric/build/outputs/aar/ aar file (e.g. biometric-release.aar)
Place it in node_modules/react-native-keychain/android/libs (note it should be renamed to biometric.aar)

Rerun patch to get the latest versio