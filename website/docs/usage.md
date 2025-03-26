---
id: usage
title: Usage
---

## Basic Usage

To use `react-native-keychain`, follow the example below to securely store and retrieve credentials.

```tsx
import * as Keychain from 'react-native-keychain';

async () => {
  const username = 'zuck';
  const password = 'poniesRgr8';

  // Store the credentials
  await Keychain.setGenericPassword(username, password, {service: 'service_key'});

  try {
    // Retrieve the credentials
    const credentials = await Keychain.getGenericPassword({service: 'service_key'});
    if (credentials) {
      console.log(
        'Credentials successfully loaded for user ' + credentials.username
      );
    } else {
      console.log('No credentials stored');
    }
  } catch (error) {
    console.error("Failed to access Keychain", error);
  }

  // Reset the stored credentials
  await Keychain.resetGenericPassword({service: 'service_key'});
};
```

See the `KeychainExample` for a fully working project example.

:::note
Both `setGenericPassword` and `setInternetCredentials` only support strings. If you need to store objects, use `JSON.stringify` when storing and `JSON.parse` when retrieving.
:::