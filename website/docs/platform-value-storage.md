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


## iOS

:::note
For iOS standalone apps, data stored with `react-native-keychain` can persist across app installs.
:::

On iOS, values are stored using the keychain services as kSecClassGenericPassword. iOS has the additional option of being able to set the value's kSecAttrAccessible attribute, which controls when the value is available to be fetched.