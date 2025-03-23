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

[index.ts:356](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/index.ts#L356)
