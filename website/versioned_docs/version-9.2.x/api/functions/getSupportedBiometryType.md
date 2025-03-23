# Function: getSupportedBiometryType()

> **getSupportedBiometryType**(): `Promise`\<`null` \| [`BIOMETRY_TYPE`](../enumerations/BIOMETRY_TYPE.md)\>

Gets the type of biometric authentication supported by the device.

## Returns

`Promise`\<`null` \| [`BIOMETRY_TYPE`](../enumerations/BIOMETRY_TYPE.md)\>

Resolves to a `BIOMETRY_TYPE` when supported, otherwise `null`.

## Example

```typescript
const biometryType = await Keychain.getSupportedBiometryType();
console.log('Supported Biometry Type:', biometryType);
```

## Defined in

[index.ts:243](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/index.ts#L243)
