# Function: setSharedWebCredentials()

> **setSharedWebCredentials**(`server`, `username`, `password`?): `Promise`\<`void`\>

Sets shared web credentials (iOS and visionOS only).

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

[index.ts:512](https://github.com/oblador/react-native-keychain/blob/4b13041ddd9b9f04560f91e6ce20080796c9fffb/src/index.ts#L512)
