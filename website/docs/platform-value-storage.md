---
id: platform-value-storage
title: Platform value storage
---

## Android

On Android, values are stored in `Jetpack DataStore`, encrypted with Android's Keystore system.
The Keystore supports various encryption algorithms, such as AES and RSA. Securing sensitive data requires careful consideration of different storage options. This guide will help you choose the most appropriate storage type.

### Security Levels

We offer four security levels for data storage:

#### 1. High Security (with Biometric Authentication)

- **AES_GCM**: Symmetric encryption with biometric protection
- **RSA**: Asymmetric encryption with biometric protection
#### 2. Medium Security (without Authentication)

- **AES_GCM_NO_AUTH**: Symmetric encryption without biometric requirements
- Best for: Cached data, non-sensitive encrypted data

#### 3. Enterprise Security (Samsung Knox)

- **KNOX**: Samsung Knox hardware-backed encryption (Samsung devices)
- Best for: Government, enterprise, and high-security compliance (FIPS 140-2)

#### 4. Legacy/Deprecated

- **AES_CBC**
- ⚠️ Not recommended for new implementations

### Secure Software vs Secure Hardware

 Android uses two primary levels of security for cryptographic key storage and operations:

1. **Secure Hardware ([StrongBox Keymaster](https://source.android.com/docs/security/best-practices/hardware))**
2. **Secure Software ([Trusted Execution Environment](https://source.android.com/docs/security/features/trusty))**

#### **What is Secure Hardware (StrongBox Keymaster)?**

Secure Hardware refers to a dedicated, physically isolated security chip (e.g., StrongBox). It is designed to provide the highest level of security for cryptographic key operations.


#### **What is Secure Software (TEE)?**

Secure Software refers to the **Trusted Execution Environment (TEE)**, a secure area of the device's main processor. It provides a sandboxed environment to store and process cryptographic keys securely, but it is not physically isolated like Secure Hardware.


#### **How Does It Affect `react-native-keychain`?**

When using `react-native-keychain` on Android, the library relies on the Android KeyStore system to store and manage cryptographic keys. The level of security provided depends on the device and its capabilities:

1. **StrongBox Enabled Devices**: If a device supports StrongBox, `react-native-keychain` can store keys in the **Secure Hardware**, offering the highest level of security.
2. **TEE-Only Devices**: If StrongBox is not available, the keys are stored in the **TEE**, which is still secure but less resistant to physical attacks.

#### **Samsung Knox Integration**

On Samsung devices, `react-native-keychain` provides additional Knox storage options:

- **Knox on API 31+**: Uses StrongBox Keymaster for hardware-backed encryption
- **Knox on API 23-30**: Uses TIMA (Trusted Integrity Management Architecture) KeyStore
- **Knox Fallback**: When using the `useKnox` option, the library gracefully falls back to standard Android Keystore on non-Samsung devices. However, explicitly selecting `STORAGE_TYPE.KNOX` on a non-Samsung device will result in an error.

Knox storage is ideal for government and enterprise applications requiring FIPS 140-2 compliance or Samsung Knox certification. For more details, see the [Knox Integration Guide](/docs/knox-integration).


## iOS

:::note
For iOS standalone apps, data stored with `react-native-keychain` can persist across app installs.
:::

On iOS, values are stored using the keychain services as kSecClassGenericPassword. iOS has the additional option of being able to set the value's kSecAttrAccessible attribute, which controls when the value is available to be fetched.