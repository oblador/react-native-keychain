---
id: platform-value-storage
title: Platform value storage
---

## Android

On Android, values are stored in `Jetpack DataStore`, encrypted with Android's Keystore system.
The Keystore supports various encryption algorithms, such as AES and RSA. Securing sensitive data requires careful consideration of different storage options. This guide will help you choose the most appropriate storage type.

### Security Levels

We offer three security levels for data storage:

#### 1. High Security (with Biometric Authentication)

- **AES_GCM**: Symmetric encryption with biometric protection
- **RSA**: Asymmetric encryption with biometric protection
- Best for: Passwords, personal data, sensitive keys

#### 2. Medium Security (without Authentication)

- **AES_GCM_NO_AUTH**: Symmetric encryption without biometric requirements
- Best for: Cached data, non-sensitive encrypted data

#### 3. Legacy/Deprecated

- **AES_CBC**
- ⚠️ Not recommended for new implementations

### Secure Software vs Secure Hardware

Android Keystore keys can be software-backed or hardware-backed. Both a **Trusted Execution Environment (TEE)** and **StrongBox** count as secure hardware; TEE is not software-only storage. StrongBox uses a separate secure processor with additional isolation. See the [Android Keystore documentation](https://developer.android.com/privacy-and-security/keystore).

`SECURITY_LEVEL.SECURE_SOFTWARE` requires at least Android Keystore storage, while `SECURITY_LEVEL.SECURE_HARDWARE` requires the generated key to be hardware-backed. The library attempts StrongBox where supported, then regular Keystore generation, and checks the resulting key against the requested level. A regular Keystore key may be hardware-backed, but this is not guaranteed solely by the absence of StrongBox.

The `getSecurityLevel()` capability query is not an attestation of a particular stored key. Specify `securityLevel` when saving if your application requires a minimum guarantee.

## iOS

:::note
For iOS standalone apps, data stored with `react-native-keychain` can persist across app installs.
:::

On iOS, values are stored using the keychain services as kSecClassGenericPassword. iOS has the additional option of being able to set the value's kSecAttrAccessible attribute, which controls when the value is available to be fetched.
