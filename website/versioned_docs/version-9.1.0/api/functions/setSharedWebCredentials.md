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

[index.ts:298](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/index.ts#L298)
