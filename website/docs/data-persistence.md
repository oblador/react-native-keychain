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

Earlier releases incorrectly treated explicit `cloudSync: false` as `true` on
iOS. Upgrading does not automatically change or delete items that were already
marked for synchronization. After the fix, a lookup with `false` will no longer
find those items.

If your app used explicit `false` and needs to retain those credentials without
iCloud synchronization:

1. Read the affected entry with `cloudSync: true`, using the original service or
   server and access group, if any.
2. Save its credentials with `cloudSync: false`, preserving the intended access
   restrictions. Check for an existing local entry first so you do not overwrite
   newer credentials, and verify that the local save succeeded.
3. Only after retaining the credentials successfully, reset the synchronized
   entry with `cloudSync: true`. Check that deletion succeeded before considering
   the migration complete.

Apply this migration only to entries your app intended to keep non-synchronizable.
If you only want to remove an affected entry, reset it with `cloudSync: true`.
Disabling iCloud synchronization is separate from the `accessible` setting and
its `ThisDeviceOnly` restrictions on backup migration.
