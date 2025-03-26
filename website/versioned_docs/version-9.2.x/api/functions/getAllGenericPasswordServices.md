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

[index.ts:131](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/index.ts#L131)
