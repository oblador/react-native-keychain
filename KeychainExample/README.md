# Example App

First run

```
$ yarn
```

in this folder

## iOS

To run the app on iOS starting from the KeychainExample folder execute
the following commands:

```bash
# verify updates: pod install --clean-install --repo-update --deployment
# forced updates: pod install --clean-install --repo-update
#
# Or regular usage:
pod install --project-directory=ios

yarn ios
```

## macOS

To run the app on macOS starting from the KeychainExample folder execute
the following commands:

```bash
yarn add react-native-macos

# verify updates: pod install --clean-install --repo-update --deployment
# forced updates: pod install --clean-install --repo-update
#
# Or regular usage:
pod install --project-directory=macos

react-native run-macos
```

## Android

just run

```bash
yarn android
```
