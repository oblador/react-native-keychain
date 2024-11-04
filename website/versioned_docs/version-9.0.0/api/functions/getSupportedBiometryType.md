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

[index.ts:449](https://github.com/oblador/react-native-keychain/blob/06824b340311076cce81e80bceb3c34da22ca810/src/index.ts#L449)
