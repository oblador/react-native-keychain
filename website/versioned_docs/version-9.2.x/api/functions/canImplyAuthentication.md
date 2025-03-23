# Function: canImplyAuthentication()

> **canImplyAuthentication**(`options`?): `Promise`\<`boolean`\>

Checks if the current device supports the specified authentication policy.

## Parameters

• **options?**: [`AuthenticationTypeOption`](../type-aliases/AuthenticationTypeOption.md)

A keychain options object.

## Returns

`Promise`\<`boolean`\>

Resolves to `true` when supported, otherwise `false`.

## Platform

iOS

## Example

```typescript
const canAuthenticate = await Keychain.canImplyAuthentication();
console.log('Can imply authentication:', canAuthenticate);
```

## Defined in

[index.ts:332](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/index.ts#L332)
