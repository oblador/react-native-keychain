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

[index.ts:170](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/index.ts#L170)
