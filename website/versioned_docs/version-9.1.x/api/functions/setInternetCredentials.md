# Function: setInternetCredentials()

> **setInternetCredentials**(`server`, `username`, `password`, `options`?): `Promise`\<`false` \| [`Result`](../type-aliases/Result.md)\>

Saves the internet credentials for the given server.

## Parameters

• **server**: `string`

The server URL.

• **username**: `string`

The username or e-mail to be saved.

• **password**: `string`

The password to be saved.

• **options?**: [`SetOptions`](../type-aliases/SetOptions.md)

A keychain options object.

## Returns

`Promise`\<`false` \| [`Result`](../type-aliases/Result.md)\>

Resolves to an object containing `service` and `storage` when successful, or `false` on failure.

## Example

```typescript
await Keychain.setInternetCredentials('https://example.com', 'username', 'password');
```

## Defined in

[index.ts:170](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/index.ts#L170)
