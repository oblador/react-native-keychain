# Function: resetInternetCredentials()

> **resetInternetCredentials**(`server`): `Promise`\<`void`\>

Deletes all internet password keychain entries for the given server.

## Parameters

• **server**: `string`

The server URL.

## Returns

`Promise`\<`void`\>

Resolves when the operation is completed.

## Example

```typescript
await Keychain.resetInternetCredentials('https://example.com');
console.log('Credentials reset for server');
```

## Defined in

[index.ts:446](https://github.com/oblador/react-native-keychain/blob/4b13041ddd9b9f04560f91e6ce20080796c9fffb/src/index.ts#L446)
