# Function: getSecurityLevel()

> **getSecurityLevel**(`options`?): `Promise`\<`null` \| [`SECURITY_LEVEL`](../enumerations/SECURITY_LEVEL.md)\>

Returns the security level supported by the library on the current device.

## Parameters

• **options?**: [`AccessControlOption`](../type-aliases/AccessControlOption.md)

A keychain options object.

## Returns

`Promise`\<`null` \| [`SECURITY_LEVEL`](../enumerations/SECURITY_LEVEL.md)\>

Resolves to a `SECURITY_LEVEL` when supported, otherwise `null`.

## Platform

Android

## Example

```typescript
const securityLevel = await Keychain.getSecurityLevel();
console.log('Security Level:', securityLevel);
```

## Defined in

[index.ts:356](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/index.ts#L356)
