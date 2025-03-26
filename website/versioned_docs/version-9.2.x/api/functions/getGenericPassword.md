# Function: getGenericPassword()

> **getGenericPassword**(`serviceOrOptions`?): `Promise`\<`false` \| [`UserCredentials`](../type-aliases/UserCredentials.md)\>

Fetches the `username` and `password` combination for the given service.

## Parameters

• **serviceOrOptions?**: `string` \| [`GetOptions`](../type-aliases/GetOptions.md)

A keychain options object or a service name string.

## Returns

`Promise`\<`false` \| [`UserCredentials`](../type-aliases/UserCredentials.md)\>

Resolves to an object containing `service`, `username`, `password`, and `storage` when successful, or `false` on failure.

## Example

```typescript
const credentials = await Keychain.getGenericPassword();
if (credentials) {
  console.log('Credentials successfully loaded for user ' + credentials.username);
} else {
  console.log('No credentials stored');
}
```

## Defined in

[index.ts:73](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/index.ts#L73)
