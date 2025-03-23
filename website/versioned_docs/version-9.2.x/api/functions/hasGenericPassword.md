# Function: hasGenericPassword()

> **hasGenericPassword**(`serviceOrOptions`?): `Promise`\<`boolean`\>

Checks if generic password exists for the given service.

## Parameters

• **serviceOrOptions?**: `string` \| [`BaseOptions`](../type-aliases/BaseOptions.md)

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

[index.ts:93](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/index.ts#L93)
