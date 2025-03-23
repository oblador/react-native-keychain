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

[index.ts:43](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/index.ts#L43)
