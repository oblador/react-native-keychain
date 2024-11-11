# Function: hasInternetCredentials()

> **hasInternetCredentials**(`serverOrOptions`): `Promise`\<`boolean`\>

Checks if internet credentials exist for the given server.

## Parameters

• **serverOrOptions**: `string` \| [`BaseOptions`](../type-aliases/BaseOptions.md)

A keychain options object or a server name string.

## Returns

`Promise`\<`boolean`\>

Resolves to `true` if internet credentials exist, otherwise `false`.

## Example

```typescript
const hasCredentials = await Keychain.hasInternetCredentials('https://example.com');
console.log('Internet credentials exist:', hasCredentials);
```

## Defined in

[index.ts:148](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/index.ts#L148)
