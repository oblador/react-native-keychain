# Enumeration: STORAGE\_TYPE

Enum representing cryptographic storage types for sensitive data.

Security Level Categories:

1. High Security (Biometric Authentication Required):
- AES_GCM: For sensitive local data (passwords, personal info)
- RSA: For asymmetric operations (signatures, key exchange)

2. Medium Security (No Authentication):
- AES_GCM_NO_AUTH: For app-level secrets and cached data

3. Legacy/Deprecated:
- AES_CBC: Outdated, use AES_GCM_NO_AUTH instead
- FB: Archived Facebook Conceal implementation

## Platform

Android

## Enumeration Members

### ~~AES~~

> **AES**: `"KeystoreAES"`

Encryptions without human interaction.

#### Deprecated

Use AES_GCM_NO_AUTH instead.

#### Defined in

[enums.ts:126](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/enums.ts#L126)

***

### ~~AES\_CBC~~

> **AES\_CBC**: `"KeystoreAESCBC"`

AES encryption in CBC (Cipher Block Chaining) mode.
Provides data confidentiality without authentication.

#### Deprecated

Use AES_GCM_NO_AUTH instead.

#### Defined in

[enums.ts:132](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/enums.ts#L132)

***

### AES\_GCM

> **AES\_GCM**: `"KeystoreAESGCM"`

AES-GCM encryption with biometric authentication.
Requires user authentication for both encryption and decryption operations.

#### Defined in

[enums.ts:142](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/enums.ts#L142)

***

### AES\_GCM\_NO\_AUTH

> **AES\_GCM\_NO\_AUTH**: `"KeystoreAESGCM_NoAuth"`

AES encryption in GCM (Galois/Counter Mode).
Provides both data confidentiality and authentication.

#### Defined in

[enums.ts:137](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/enums.ts#L137)

***

### ~~FB~~

> **FB**: `"FacebookConceal"`

Facebook compatibility cipher.

#### Deprecated

Facebook Conceal was deprecated and archived in Mar 3, 2020. https://github.com/facebookarchive/conceal

#### Defined in

[enums.ts:122](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/enums.ts#L122)

***

### RSA

> **RSA**: `"KeystoreRSAECB"`

RSA encryption with biometric authentication.
Uses asymmetric encryption and requires biometric authentication.

#### Defined in

[enums.ts:147](https://github.com/oblador/react-native-keychain/blob/6ec8fdb5b967a106085e74014d8072182c9fca28/src/enums.ts#L147)
