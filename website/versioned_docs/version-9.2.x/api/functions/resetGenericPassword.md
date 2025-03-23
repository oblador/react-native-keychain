# Function: resetGenericPassword()

> **resetGenericPassword**(`serviceOrOptions`?): `Promise`\<`boolean`\>

Deletes all generic password keychain entries for the given service.

## Parameters

• **serviceOrOptions?**: `string` \| [`BaseOptions`](../type-aliases/BaseOptions.md)

A keychain options object or a service name string.

## Returns

`Promise`\<`boolean`\>

Resolves to `true` when successful, otherwise `false`.

## Example

```typescript
const success = await Keychain.resetGenericPassword();
console.log('Password reset successful:', success);
```

## Defined in

[index.ts:113](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/index.ts#L113)
