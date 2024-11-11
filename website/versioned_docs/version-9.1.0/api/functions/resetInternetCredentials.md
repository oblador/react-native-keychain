# Function: resetInternetCredentials()

> **resetInternetCredentials**(`serverOrOptions`): `Promise`\<`void`\>

Deletes all internet password keychain entries for the given server.

## Parameters

• **serverOrOptions**: `string` \| [`BaseOptions`](../type-aliases/BaseOptions.md)

## Returns

`Promise`\<`void`\>

Resolves when the operation is completed.

## Example

```typescript
await Keychain.resetInternetCredentials('https://example.com');
console.log('Credentials reset for server');
```

## Defined in

[index.ts:225](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/index.ts#L225)
