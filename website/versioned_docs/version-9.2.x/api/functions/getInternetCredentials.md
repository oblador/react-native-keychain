# Function: getInternetCredentials()

> **getInternetCredentials**(`server`, `options`?): `Promise`\<`false` \| [`UserCredentials`](../type-aliases/UserCredentials.md)\>

Fetches the internet credentials for the given server.

## Parameters

• **server**: `string`

The server URL.

• **options?**: [`GetOptions`](../type-aliases/GetOptions.md)

A keychain options object.

## Returns

`Promise`\<`false` \| [`UserCredentials`](../type-aliases/UserCredentials.md)\>

Resolves to an object containing `server`, `username`, `password`, and `storage` when successful, or `false` on failure.

## Example

```typescript
const credentials = await Keychain.getInternetCredentials('https://example.com');
if (credentials) {
  console.log('Credentials loaded for user ' + credentials.username);
} else {
  console.log('No credentials stored for server');
}
```

## Defined in

[index.ts:202](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/index.ts#L202)
