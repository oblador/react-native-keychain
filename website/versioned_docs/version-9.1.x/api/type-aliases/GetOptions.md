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

[types.ts:67](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/types.ts#L67)
