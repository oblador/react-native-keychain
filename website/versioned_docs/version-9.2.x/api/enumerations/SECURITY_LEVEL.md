# Enumeration: SECURITY\_LEVEL

Enum representing security levels.

## Platform

Android

## Enumeration Members

### ANY

> **ANY**: `number`

No security guarantees needed (default value). Credentials can be stored in FB Secure Storage.

#### Defined in

[enums.ts:67](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/enums.ts#L67)

***

### SECURE\_HARDWARE

> **SECURE\_HARDWARE**: `number`

Requires for the key to be stored on a secure hardware (Trusted Execution Environment or Secure Environment).
Read this article for more information: https://developer.android.com/privacy-and-security/keystore#ExtractionPrevention

#### Defined in

[enums.ts:64](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/enums.ts#L64)

***

### SECURE\_SOFTWARE

> **SECURE\_SOFTWARE**: `number`

Requires for the key to be stored in the Android Keystore, separate from the encrypted data.

#### Defined in

[enums.ts:59](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/enums.ts#L59)
