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

[index.ts:131](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/index.ts#L131)
