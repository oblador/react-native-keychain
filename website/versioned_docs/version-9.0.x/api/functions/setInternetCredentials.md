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

• **options?**: `Partial`\<`object` & [`BaseOptions`](../type-aliases/BaseOptions.md)\>

A keychain options object.

## Returns

`Promise`\<`false` \| [`Result`](../type-aliases/Result.md)\>

Resolves to an object containing `service` and `storage` when successful, or `false` on failure.

## Example

```typescript
await Keychain.setInternetCredentials('https://example.com', 'username', 'password');
```

## Defined in

[index.ts:379](https://github.com/oblador/react-native-keychain/blob/06824b340311076cce81e80bceb3c34da22ca810/src/index.ts#L379)
