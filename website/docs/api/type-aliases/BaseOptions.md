# Type Alias: BaseOptions

> **BaseOptions**: `object`

Base options for keychain functions.

## Type declaration

### accessControl?

> `optional` **accessControl**: [`ACCESS_CONTROL`](../enumerations/ACCESS_CONTROL.md)

The access control policy to use for the keychain item.

### accessGroup?

> `optional` **accessGroup**: `string`

The access group to share keychain items between apps (iOS and visionOS only).

### accessible?

> `optional` **accessible**: [`ACCESSIBLE`](../enumerations/ACCESSIBLE.md)

Specifies when a keychain item is accessible (iOS and visionOS only).

### authenticationType?

> `optional` **authenticationType**: [`AUTHENTICATION_TYPE`](../enumerations/AUTHENTICATION_TYPE.md)

Authentication type for retrieving keychain item (iOS and visionOS only).

### rules?

> `optional` **rules**: [`SECURITY_RULES`](../enumerations/SECURITY_RULES.md)

The security rules to apply when storing the keychain item (Android only).

### securityLevel?

> `optional` **securityLevel**: [`SECURITY_LEVEL`](../enumerations/SECURITY_LEVEL.md)

The desired security level of the keychain item.

### service?

> `optional` **service**: `string`

The service name to associate with the keychain item.

### storage?

> `optional` **storage**: [`STORAGE_TYPE`](../enumerations/STORAGE_TYPE.md)

The storage type (Android only).

## Defined in

[index.ts:124](https://github.com/oblador/react-native-keychain/blob/4b13041ddd9b9f04560f91e6ce20080796c9fffb/src/index.ts#L124)
