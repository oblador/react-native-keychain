# Function: setSharedWebCredentials()

> **setSharedWebCredentials**(`server`, `username`, `password`?): `Promise`\<`void`\>

Sets shared web credentials.

## Parameters

• **server**: `string`

The server URL.

• **username**: `string`

The username or e-mail to be saved.

• **password?**: `string`

The password to be saved.

## Returns

`Promise`\<`void`\>

Resolves when the operation is completed.

## Platform

iOS

## Example

```typescript
await Keychain.setSharedWebCredentials('https://example.com', 'username', 'password');
console.log('Shared web credentials set');
```

## Defined in

[index.ts:298](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/index.ts#L298)
