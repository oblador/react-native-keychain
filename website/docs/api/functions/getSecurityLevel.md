# Function: getSecurityLevel()

> **getSecurityLevel**(`options`?): `Promise`\<`null` \| [`SECURITY_LEVEL`](../enumerations/SECURITY_LEVEL.md)\>

Returns the security level supported by the library on the current device (Android only).

## Parameters

• **options?**: `Partial`\<`object` & [`BaseOptions`](../type-aliases/BaseOptions.md)\>

A keychain options object.

## Returns

`Promise`\<`null` \| [`SECURITY_LEVEL`](../enumerations/SECURITY_LEVEL.md)\>

Resolves to a `SECURITY_LEVEL` when supported, otherwise `null`.

## Example

```typescript
const securityLevel = await Keychain.getSecurityLevel();
console.log('Security Level:', securityLevel);
```

## Defined in

[index.ts:564](https://github.com/oblador/react-native-keychain/blob/4b13041ddd9b9f04560f91e6ce20080796c9fffb/src/index.ts#L564)
