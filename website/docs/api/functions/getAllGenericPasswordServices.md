# Function: getAllGenericPasswordServices()

> **getAllGenericPasswordServices**(): `Promise`\<`string`[]\>

Gets all service keys used in generic password keychain entries.

## Returns

`Promise`\<`string`[]\>

Resolves to an array of strings representing service keys.

## Example

```typescript
const services = await Keychain.getAllGenericPasswordServices();
console.log('Services:', services);
```

## Defined in

[index.ts:353](https://github.com/oblador/react-native-keychain/blob/4b13041ddd9b9f04560f91e6ce20080796c9fffb/src/index.ts#L353)
