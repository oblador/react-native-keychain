---
id: data-persistence
title: Data persistence
---

`react-native-keychain` provides a persistent data storage solution that survives app restarts and updates. However, it should not be relied upon as the sole source of truth for irreplaceable or critical data. Data stored using `react-native-keychain` is erased when the app is uninstalled. The exception is iOS, where stored data can persist across app reinstalls due to the way iOS handles keychain storage

To check if data is available in the keychain/keystore, you can use `hasGenericPassword` and `hasInternetCredentials`:

```typescript
import Keychain from 'react-native-keychain';

const isGenericPasswordAvailable = await Keychain.hasGenericPassword({
  service: 'service_key',
});

const isInternetCredentialAvailable = await Keychain.hasInternetCredentials({
  server: 'https://google.com',
});
```

## iCloud synchronization on iOS

`cloudSync` defaults to `false`. Omitting it or passing `false` selects
non-synchronizable keychain items; `true` selects synchronizable items. Use the
same value when storing, reading, checking, and resetting credentials. These
queries match separate items, even when the service or server is the same.
See Apple's [`kSecAttrSynchronizable` documentation](https://developer.apple.com/documentation/security/ksecattrsynchronizable).
