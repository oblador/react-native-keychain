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

[types.ts:44](https://github.com/oblador/react-native-keychain/blob/7eaf30e4858d9a03afd4c8e017b83a96fbc4e982/src/types.ts#L44)
