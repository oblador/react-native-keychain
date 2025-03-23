# Function: hasInternetCredentials()

> **hasInternetCredentials**(`server`): `Promise`\<`false` \| [`Result`](../type-aliases/Result.md)\>

Checks if internet credentials exist for the given server.

## Parameters

• **server**: `string`

The server URL.

## Returns

`Promise`\<`false` \| [`Result`](../type-aliases/Result.md)\>

Resolves to an object containing `service` and `storage` when successful, or `false` if not found.

## Example

```typescript
const hasCredentials = await Keychain.hasInternetCredentials('https://example.com');
console.log('Internet credentials exist:', hasCredentials);
```

## Defined in

[index.ts:358](https://github.com/oblador/react-native-keychain/blob/06824b340311076cce81e80bceb3c34da22ca810/src/index.ts#L358)
