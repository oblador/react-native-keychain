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

[index.ts:370](https://github.com/oblador/react-native-keychain/blob/4b13041ddd9b9f04560f91e6ce20080796c9fffb/src/index.ts#L370)
