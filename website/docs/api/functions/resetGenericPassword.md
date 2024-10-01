# Function: resetGenericPassword()

> **resetGenericPassword**(`serviceOrOptions`?): `Promise`\<`boolean`\>

Deletes all generic password keychain entries for the given service.

## Parameters

• **serviceOrOptions?**: `string` \| `Partial`\<`object` & [`BaseOptions`](../type-aliases/BaseOptions.md)\>

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

[index.ts:335](https://github.com/oblador/react-native-keychain/blob/4b13041ddd9b9f04560f91e6ce20080796c9fffb/src/index.ts#L335)
