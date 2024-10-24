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

[index.ts:303](https://github.com/oblador/react-native-keychain/blob/06824b340311076cce81e80bceb3c34da22ca810/src/index.ts#L303)
