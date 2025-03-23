# Function: setGenericPassword()

> **setGenericPassword**(`username`, `password`, `serviceOrOptions`?): `Promise`\<`false` \| [`Result`](../type-aliases/Result.md)\>

Saves the `username` and `password` combination for the given service.

## Parameters

• **username**: `string`

The username or e-mail to be saved.

• **password**: `string`

The password to be saved.

• **serviceOrOptions?**: `string` \| [`SetOptions`](../type-aliases/SetOptions.md)

A keychain options object or a service name string. Passing a service name as a string is deprecated.

## Returns

`Promise`\<`false` \| [`Result`](../type-aliases/Result.md)\>

Resolves to an object containing `service` and `storage` when successful, or `false` on failure.

## Example

```typescript
await Keychain.setGenericPassword('username', 'password');
```

## Defined in

[index.ts:43](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/index.ts#L43)
