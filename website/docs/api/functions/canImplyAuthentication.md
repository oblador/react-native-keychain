# Function: canImplyAuthentication()

> **canImplyAuthentication**(`options`?): `Promise`\<`boolean`\>

Checks if the current device supports the specified authentication policy.

## Parameters

• **options?**: `Partial`\<`object` & [`BaseOptions`](../type-aliases/BaseOptions.md)\>

A keychain options object.

## Returns

`Promise`\<`boolean`\>

Resolves to `true` when supported, otherwise `false`.

## Example

```typescript
const canAuthenticate = await Keychain.canImplyAuthentication();
console.log('Can imply authentication:', canAuthenticate);
```

## Defined in

[index.ts:544](https://github.com/oblador/react-native-keychain/blob/4b13041ddd9b9f04560f91e6ce20080796c9fffb/src/index.ts#L544)
