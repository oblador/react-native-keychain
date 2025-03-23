# Function: requestSharedWebCredentials()

> **requestSharedWebCredentials**(): `Promise`\<`false` \| [`SharedWebCredentials`](../type-aliases/SharedWebCredentials.md)\>

Request shared web credentials.

## Returns

`Promise`\<`false` \| [`SharedWebCredentials`](../type-aliases/SharedWebCredentials.md)\>

Resolves to an object containing `server`, `username`, and `password` if approved, or `false` if denied.

## Platform

iOS

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

[index.ts:268](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/index.ts#L268)
