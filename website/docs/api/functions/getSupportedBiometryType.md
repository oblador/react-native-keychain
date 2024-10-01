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

[index.ts:461](https://github.com/oblador/react-native-keychain/blob/4b13041ddd9b9f04560f91e6ce20080796c9fffb/src/index.ts#L461)
