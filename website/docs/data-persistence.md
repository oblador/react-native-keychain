---
id: data-persistence
title: Data persistence
---

`react-native-keychain` provides a persistent data storage solution that survives app restarts and updates. However, it should not be relied upon as the sole source of truth for irreplaceable or critical data. Data stored using `react-native-keychain` is erased when the app is uninstalled. The exception is iOS, where stored data can persist across app reinstalls due to the way iOS handles keychain storage

To check if data is available in the keychain/keystore, you can use `hasGenericPassword` and `hasInternetCredentials`:  

```typescript
import Keychain from 'react-native-keychain';

const isGenericPasswordAvailable = await Keychain.hasGenericPassword({
  service: 'service_key'
});

const isInternetCredentialAvailable = await Keychain.hasInternetCredentials({
  server: 'https://google.com'
});
```