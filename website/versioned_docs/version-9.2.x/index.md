---
id: index
title: react-native-keychain
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

## Overview

**react-native-keychain** is a library that provides keychain/keystore access for React Native applications. It allows you to securely store and retrieve sensitive information such as passwords, internet credentials, and tokens using native encryption mechanisms provided by iOS and Android.

This library supports various security features such as biometric authentication (Face ID, Touch ID, Fingerprint), secure storage levels, as well as customizable options for accessing and storing data.

## Support

This library supports both iOS and Android platforms. Additionally, it has support for macOS Catalyst and visionOS.
For iOS, the library uses the Keychain Services API, while on Android, it uses Facebook Conceal or the Android Keystore depending on the API level.

Supported platforms and versions:

- **iOS**: Requires iOS 9.0+
- **Android**: API 16+ (uses Facebook Conceal for API levels 16-22, Android Keystore for API 23+)
- **macOS Catalyst**: Supported
- **visionOS**: Supported

## Installation

**react-native-keychain** is on the npm registry! Install it using your favorite Node.js package manager:

<Tabs>
<TabItem value="npm" label="npm">

```bash
npm install react-native-keychain
```

</TabItem>
<TabItem value="yarn" label="Yarn">

```bash
yarn add react-native-keychain
```

</TabItem>
<TabItem value="pnpm" label="pnpm">

```bash
pnpm add react-native-keychain
```

</TabItem>
</Tabs>