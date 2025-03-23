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

[index.ts:148](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/index.ts#L148)
