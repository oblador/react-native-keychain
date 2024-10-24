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

[index.ts:434](https://github.com/oblador/react-native-keychain/blob/06824b340311076cce81e80bceb3c34da22ca810/src/index.ts#L434)
