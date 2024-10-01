# Function: setGenericPassword()

> **setGenericPassword**(`username`, `password`, `serviceOrOptions`?): `Promise`\<`false` \| [`Result`](../type-aliases/Result.md)\>

Saves the `username` and `password` combination for the given service.

## Parameters

• **username**: `string`

The username or e-mail to be saved.

• **password**: `string`

The password to be saved.

• **serviceOrOptions?**: `string` \| `Partial`\<`object` & [`BaseOptions`](../type-aliases/BaseOptions.md)\>

A keychain options object or a service name string. Passing a service name as a string is deprecated.

## Returns

`Promise`\<`false` \| [`Result`](../type-aliases/Result.md)\>

Resolves to an object containing `service` and `storage` when successful, or `false` on failure.

## Example

```typescript
await Keychain.setGenericPassword('username', 'password');
```

## Defined in

[index.ts:265](https://github.com/oblador/react-native-keychain/blob/4b13041ddd9b9f04560f91e6ce20080796c9fffb/src/index.ts#L265)
