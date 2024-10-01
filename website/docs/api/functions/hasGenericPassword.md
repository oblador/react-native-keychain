# Function: hasGenericPassword()

> **hasGenericPassword**(`serviceOrOptions`?): `Promise`\<`boolean`\>

Checks if generic password exists for the given service.

## Parameters

• **serviceOrOptions?**: `string` \| `Partial`\<`object` & [`BaseOptions`](../type-aliases/BaseOptions.md)\>

A keychain options object or a service name string.

## Returns

`Promise`\<`boolean`\>

Resolves to `true` if a password exists, otherwise `false`.

## Example

```typescript
const hasPassword = await Keychain.hasGenericPassword();
console.log('Password exists:', hasPassword);
```

## Defined in

[index.ts:315](https://github.com/oblador/react-native-keychain/blob/4b13041ddd9b9f04560f91e6ce20080796c9fffb/src/index.ts#L315)
