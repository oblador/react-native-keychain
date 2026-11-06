# Installation

Add the package, install native dependencies, and rebuild your app.

## Requirements

These docs follow the current source, which requires **React Native 0.82+** and the **New Architecture**. For an older React Native release or the Old Architecture, check the [release notes](https://github.com/oblador/react-native-keychain/releases) for a compatible library version.

| Target  | Library minimum |
| ------- | --------------- |
| iOS     | 15.1            |
| Android | API 24          |

Apple support also includes macOS, Mac Catalyst, and visionOS. Available authentication methods vary by platform and device. Your React Native version or app may require a higher deployment target than the library itself.

## Add the package

::: code-group

```sh [npm]
npm install react-native-keychain
```

```sh [Yarn]
yarn add react-native-keychain
```

```sh [pnpm]
pnpm add react-native-keychain
```

:::

## iOS setup

Install pods from your app's iOS directory. If your project uses Bundler:

```sh
cd ios
bundle exec pod install
```

For Face ID, add a purpose string to the app's `Info.plist`:

```xml
<key>NSFaceIDUsageDescription</key>
<string>Use Face ID to unlock your saved sign-in.</string>
```

Rebuild the iOS app after installing the package or changing native configuration.

## Android setup

React Native autolinking connects the native module. Rebuild the Android app after installing the package.

## Expo projects

This library includes native code and needs a development build or a native app build. It cannot run inside Expo Go. See [Expo’s native-code guide](https://docs.expo.dev/workflow/customizing/). Apply the iOS configuration to your generated native project or through your project's Expo configuration before rebuilding.

## Verify the setup

Follow [Your first credential](./index.md#your-first-credential) on a simulator or device. If the native module cannot be found, check that the package is installed, the New Architecture is enabled, and you rebuilt the native app rather than only reloading JavaScript.
