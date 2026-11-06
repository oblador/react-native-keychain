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
  await Keychain.setGenericPassword(username, password, {
    service: 'service_key',
  });

  try {
    // Retrieve the credentials
    const credentials = await Keychain.getGenericPassword({
      service: 'service_key',
    });
    if (credentials) {
      console.log(
        'Credentials successfully loaded for user ' + credentials.username
      );
    } else {
      // `false` means no credentials were found for the requested service.
      console.log('No credentials stored');
    }
  } catch (error) {
    console.error('Failed to access Keychain', error);
  }

  // Reset the stored credentials
  await Keychain.resetGenericPassword({ service: 'service_key' });
};
```

See the `KeychainExample` for a fully working project example.

:::note
Both `setGenericPassword` and `setInternetCredentials` only support strings. If you need to store objects, use `JSON.stringify` when storing and `JSON.parse` when retrieving.
:::

## Store multiple values

Use a different `service` for each value you want to store independently. Pass the
same `service` when reading or resetting that entry. Changing only the `username`
does not create a separate entry; saving to the same service replaces its
credentials within the selected storage scope.

```typescript
import * as Keychain from 'react-native-keychain';

await Keychain.setGenericPassword('session', 'example-access-token', {
  service: 'com.example.access-token',
});
await Keychain.setGenericPassword('session', 'example-refresh-token', {
  service: 'com.example.refresh-token',
});

const accessCredentials = await Keychain.getGenericPassword({
  service: 'com.example.access-token',
});
const refreshCredentials = await Keychain.getGenericPassword({
  service: 'com.example.refresh-token',
});

// Remove only the access token; the refresh token remains stored.
await Keychain.resetGenericPassword({ service: 'com.example.access-token' });
```

Each read returns `false` if that entry is missing; otherwise, its `password` field
contains the stored token. These operations can run in separate functions or at
different times as long as the service names match. On iOS, also use the same
`accessGroup` and `cloudSync` options when writing, reading, and resetting an entry.

## List stored internet credential servers

```typescript
const servers = await Keychain.getAllInternetCredentialServers();
for (const server of servers) {
  // Read only the credentials you need, or reset an entry using { server }.
  console.log(server);
}
```

This returns distinct server identifiers, without returning usernames or passwords.
An empty selection returns `[]`; ordering is unspecified.

On iOS, pass the same `cloudSync` and `accessGroup` used when saving credentials.
Omitted `cloudSync` and `false` select local entries; `true` selects synchronizable
entries. Items requiring authentication are skipped by default. Set
`skipUIAuth: false` to allow authentication UI.

On Android, new writes record whether an entry contains generic or internet
credentials. Only entries recorded as internet credentials are listed by default.
For an upgrade, `includeLegacy: true` also lists older unclassified records, which
may include generic password services. Re-save a known internet entry with
`setInternetCredentials` to classify it. Both credential APIs still share the same
Android namespace: using the same service/server replaces the existing entry.
