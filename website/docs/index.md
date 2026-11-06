---
title: Getting started
description: Store and retrieve passwords and tokens with Apple Keychain and Android Keystore in React Native.
---

# Getting started

Keep passwords and tokens in Apple Keychain and Android Keystore, with one API for your React Native app.

`react-native-keychain` stores a username and a secret together. Use it for a session token, saved sign-in, or credentials tied to a server. You can also require biometrics or a device passcode before access.

## Install

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

Install the iOS pods and rebuild your app before calling the library. See [Installation](./installation.md) for requirements, native setup, and Face ID configuration.

## Your first credential

Use the same `service` when saving, reading, and removing a credential. The `password` field can hold a token as well as a password.

```ts
import * as Keychain from 'react-native-keychain';

const options = { service: 'com.example.session' };

// Save after signing in. Replace this sample with your session token.
await Keychain.setGenericPassword('session', 'example-token', options);

// Read when you need the token.
const credentials = await Keychain.getGenericPassword(options);
if (credentials) {
  const token = credentials.password;
  // Use the token in your authenticated request.
}

// Remove when signing out.
await Keychain.resetGenericPassword(options);
```

A read returns `false` when no matching credential exists. Authentication and storage failures reject the promise: handle those separately from a missing credential. See [Store and retrieve](./usage.md) for a complete example with error handling.

## Choose a credential API

| You want to store         | Use                      | Identify the entry with |
| ------------------------- | ------------------------ | ----------------------- |
| A token or an app sign-in | `setGenericPassword`     | `service`               |
| Credentials for a server  | `setInternetCredentials` | `server`                |

On iOS, these use different Keychain item classes. On Android, they share a namespace, so use different identifiers for unrelated entries.

## Next steps

- [Store and retrieve](./usage.md) — overwrite entries, store multiple tokens, and work with servers.
- [Authentication](./authentication.md) — require biometrics or a passcode and handle failures.
- [Example app](./example-app.md) — try save, load, and reset on a device.
- [API reference](./api/index.md) — find every function, option, and enum.
