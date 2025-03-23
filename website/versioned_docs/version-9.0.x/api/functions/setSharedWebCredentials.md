# Function: setSharedWebCredentials()

> **setSharedWebCredentials**(`server`, `username`, `password`?): `Promise`\<`void`\>

Sets shared web credentials (iOS only).

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

## Example

```typescript
await Keychain.setSharedWebCredentials('https://example.com', 'username', 'password');
console.log('Shared web credentials set');
```

## Defined in

[index.ts:500](https://github.com/oblador/react-native-keychain/blob/06824b340311076cce81e80bceb3c34da22ca810/src/index.ts#L500)
