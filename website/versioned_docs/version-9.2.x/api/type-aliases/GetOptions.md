# Type Alias: GetOptions

> **GetOptions**: `object` & [`BaseOptions`](BaseOptions.md) & [`AccessControlOption`](AccessControlOption.md)

Base options for keychain functions.

## Type declaration

### accessControl?

> `optional` **accessControl**: [`ACCESS_CONTROL`](../enumerations/ACCESS_CONTROL.md)

The access control policy to use for the keychain item.

### authenticationPrompt?

> `optional` **authenticationPrompt**: `string` \| [`AuthenticationPrompt`](AuthenticationPrompt.md)

Authentication prompt details or a title string.

#### Default

```json
{
  "title": "Authenticate to retrieve secret",
  "cancel": "Cancel"
}
```

### rules?

> `optional` **rules**: [`SECURITY_RULES`](../enumerations/SECURITY_RULES.md)

The security rules to apply when storing the keychain item.

#### Platform

Android

#### Default

```ts
SECURITY_RULES.AUTOMATIC_UPGRADE
```

## Defined in

[types.ts:78](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/types.ts#L78)
