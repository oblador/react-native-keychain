# Function: canImplyAuthentication()

> **canImplyAuthentication**(`options`?): `Promise`\<`boolean`\>

Checks if the current device supports the specified authentication policy (iOS only).

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

[index.ts:532](https://github.com/oblador/react-native-keychain/blob/06824b340311076cce81e80bceb3c34da22ca810/src/index.ts#L532)
