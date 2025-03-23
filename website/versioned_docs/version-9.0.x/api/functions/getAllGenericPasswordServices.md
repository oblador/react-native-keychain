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

[index.ts:341](https://github.com/oblador/react-native-keychain/blob/06824b340311076cce81e80bceb3c34da22ca810/src/index.ts#L341)
