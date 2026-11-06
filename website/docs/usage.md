# Store and retrieve

Save credentials under a stable identifier, then use that identifier for every read, check, and reset.

## Save, read, and remove

```ts
import * as Keychain from 'react-native-keychain';

const options = { service: 'com.example.session' };

// Save after a successful sign-in.
await Keychain.setGenericPassword('session', 'example-token', options);

try {
  const credentials = await Keychain.getGenericPassword(options);
  if (credentials) {
    // Use credentials.password in an authenticated request.
  } else {
    // No credential exists for this service. Offer sign-in.
  }
} catch {
  // Access failed or authentication was cancelled. Offer retry or sign-in.
}

// Check existence without reading the secret.
const exists = await Keychain.hasGenericPassword(options);

// Remove the credential when signing out.
await Keychain.resetGenericPassword(options);
```

Handle rejected promises from save, existence checks, and reset in your app too. Saving to an existing service replaces that entry within the selected scope.

::: info Strings only
Both credential APIs accept strings. Use `JSON.stringify` when storing an object and `JSON.parse` after reading it. Keep large application data in a separate store.
:::

## Credentials for a server

Pass the server as the first argument when saving or reading, and inside the options object when checking or removing:

```ts
import * as Keychain from 'react-native-keychain';

const server = 'https://api.example.com';

await Keychain.setInternetCredentials(
  server,
  'example-user',
  'example-password'
);
const credentials = await Keychain.getInternetCredentials(server);
const exists = await Keychain.hasInternetCredentials({ server });
await Keychain.resetInternetCredentials({ server });
```

On Android, service and server identifiers share one namespace. Use distinct identifiers for entries you want to keep separate. On iOS, generic and internet credentials use different Keychain classes.

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
