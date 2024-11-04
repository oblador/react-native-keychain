# Function: getGenericPassword()

> **getGenericPassword**(`serviceOrOptions`?): `Promise`\<`false` \| [`UserCredentials`](../type-aliases/UserCredentials.md)\>

Fetches the `username` and `password` combination for the given service.

## Parameters

• **serviceOrOptions?**: `string` \| `Partial`\<`object` & [`BaseOptions`](../type-aliases/BaseOptions.md)\>

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

[index.ts:283](https://github.com/oblador/react-native-keychain/blob/06824b340311076cce81e80bceb3c34da22ca810/src/index.ts#L283)
