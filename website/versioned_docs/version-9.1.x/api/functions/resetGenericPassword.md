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

[index.ts:113](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/index.ts#L113)
