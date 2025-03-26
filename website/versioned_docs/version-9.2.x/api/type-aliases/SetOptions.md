# Type Alias: SetOptions

> **SetOptions**: `object` & [`BaseOptions`](BaseOptions.md) & [`AccessControlOption`](AccessControlOption.md)

Base options for keychain functions.

## Type declaration

### accessGroup?

> `optional` **accessGroup**: `string`

The access group to share keychain items between apps.

#### Platform

iOS, visionOS

### accessible?

> `optional` **accessible**: [`ACCESSIBLE`](../enumerations/ACCESSIBLE.md)

Specifies when a keychain item is accessible.

#### Platform

iOS, visionOS

#### Default

```ts
ACCESSIBLE.AFTER_FIRST_UNLOCK
```

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

### securityLevel?

> `optional` **securityLevel**: [`SECURITY_LEVEL`](../enumerations/SECURITY_LEVEL.md)

The desired security level of the keychain item.

#### Platform

Android

### storage?

> `optional` **storage**: [`STORAGE_TYPE`](../enumerations/STORAGE_TYPE.md)

The storage type.

#### Platform

Android

#### Default

```ts
'Best available storage'
```

## Defined in

[types.ts:44](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/types.ts#L44)
