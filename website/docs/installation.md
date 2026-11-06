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

### Swift Package Manager (preview)

For iOS apps using React Native 0.88 or newer, this library also ships a `Package.swift` for React Native's experimental SwiftPM autolinking. Install the npm package as above, then run from your app's iOS directory:

```sh
npx react-native spm add --deintegrate --yes
```

`--deintegrate` removes the app's CocoaPods integration; omit it for an app that has never used CocoaPods. For an app already using SwiftPM, run `npx react-native spm update` after adding this library. Open the app's `.xcodeproj` and rebuild.

The manifest relies on React Native's generated packages and app-specific Codegen output. Adding this repository directly through Xcode's **Add Package Dependencies** is not supported. Other native dependencies in your app also need SwiftPM support. See [React Native's SwiftPM guide](https://github.com/react/react-native/blob/v0.88.1/packages/react-native/scripts/spm/__docs__/spm-scripts.md) for setup and limitations.

SwiftPM support here is iOS-only. CocoaPods remains supported, including for the other Apple platforms. The Face ID configuration above applies to either setup.

## Android setup

React Native autolinking connects the native module. Rebuild the Android app after installing the package.

## Expo projects

This library includes native code and needs a development build or a native app build. It cannot run inside Expo Go. See [Expo’s native-code guide](https://docs.expo.dev/workflow/customizing/). Apply the iOS configuration to your generated native project or through your project's Expo configuration before rebuilding.

## Verify the setup

Follow [Your first credential](./index.md#your-first-credential) on a simulator or device. If the native module cannot be found, check that the package is installed, the New Architecture is enabled, and you rebuilt the native app rather than only reloading JavaScript.
