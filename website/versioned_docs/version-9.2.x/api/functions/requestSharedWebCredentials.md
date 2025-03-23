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

[index.ts:268](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/index.ts#L268)
