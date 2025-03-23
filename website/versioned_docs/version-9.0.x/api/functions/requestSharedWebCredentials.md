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

[index.ts:472](https://github.com/oblador/react-native-keychain/blob/06824b340311076cce81e80bceb3c34da22ca810/src/index.ts#L472)
