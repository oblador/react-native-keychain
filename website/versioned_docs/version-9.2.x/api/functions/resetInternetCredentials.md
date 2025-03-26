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

[index.ts:225](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/index.ts#L225)
