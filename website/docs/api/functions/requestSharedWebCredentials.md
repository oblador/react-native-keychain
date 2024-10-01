# Function: requestSharedWebCredentials()

> **requestSharedWebCredentials**(): `Promise`\<`false` \| [`SharedWebCredentials`](../type-aliases/SharedWebCredentials.md)\>

Request shared web credentials (iOS only).

## Returns

`Promise`\<`false` \| [`SharedWebCredentials`](../type-aliases/SharedWebCredentials.md)\>

Resolves to an object containing `server`, `username`, and `password` if approved, or `false` if denied.

## Example

```typescript
const credentials = await Keychain.requestSharedWebCredentials();
if (credentials) {
  console.log('Shared credentials retrieved:', credentials);
} else {
  console.log('No shared credentials available');
}
```

## Defined in

[index.ts:484](https://github.com/oblador/react-native-keychain/blob/4b13041ddd9b9f04560f91e6ce20080796c9fffb/src/index.ts#L484)
