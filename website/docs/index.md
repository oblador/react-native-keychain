---
id: index
title: react-native-keychain
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

## Overview

**react-native-keychain** is a library that provides keychain/keystore access for React Native applications. It allows you to securely store and retrieve sensitive information such as passwords, internet credentials, and tokens using native encryption mechanisms provided by iOS and Android.

This library supports various security features such as biometric authentication (Face ID, Touch ID, Fingerprint), passcode authentication, secure storage levels, as well as customizable options for accessing and storing data.

## Support

This library supports both iOS and Android platforms. Additionally, it has support for macOS Catalyst and visionOS.
For iOS, the library uses the Keychain Services API, while on Android, it uses the Android Keystore.

Supported platforms and versions:

- **iOS**: Requires iOS 15.1+
- **Android**: API 24+
- **macOS Catalyst**: Supported
- **visionOS**: Supported

## Installation

React Native **0.82 or later** with the **New Architecture** is required for the upcoming 11.x release. Use 10.x for older React Native versions or the Old Architecture. The iOS minimum is **15.1** and Android minimum is **API 24**. Development requires **Node.js 20.19.4+**, **JDK 17**, and **Xcode 16.1+**.

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
