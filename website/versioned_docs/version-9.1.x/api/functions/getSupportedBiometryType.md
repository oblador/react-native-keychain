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

[index.ts:243](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/index.ts#L243)
