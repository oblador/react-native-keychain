---
id: secure-hardware-vs-software
title: Secure Hardware vs Software
---

When working with sensitive data in Android applications, it's critical to understand how Android handles secure key storage and cryptographic operations. Android uses two primary levels of security for cryptographic key storage and operations:

1. **Secure Hardware ([StrongBox Keymaster](https://source.android.com/docs/security/best-practices/hardware))**
2. **Secure Software ([Trusted Execution Environment](https://source.android.com/docs/security/features/trusty))**

This document explains the differences between these two mechanisms and how they relate to `react-native-keychain`.

---

## **What is Secure Hardware (StrongBox Keymaster)?**

Secure Hardware refers to a dedicated, physically isolated security chip (e.g., StrongBox). It is designed to provide the highest level of security for cryptographic key operations.

### Key Features of Secure Hardware:
- **Hardware Isolation**: The cryptographic keys are stored in a secure, tamper-resistant environment that is completely isolated from the main device's operating system and CPU.
- **Hardware-backed Security**: Cryptographic operations (like signing or encryption) are performed directly on the hardware, ensuring that the keys never leave the secure environment.
- **Resistant to Physical Attacks**: Designed to thwart physical attacks like voltage manipulation or side-channel attacks.
- **StrongBox Support**: Devices with Android 9 (API Level 28) or higher may include StrongBox, which enhances hardware-backed security.

### Limitations of Secure Hardware:
- **Device Dependency**: Not all Android devices support StrongBox. It is only available on certain devices with dedicated secure hardware.
- **Performance**: Cryptographic operations directly on the hardware may be slightly slower than software-based operations.

---

## **What is Secure Software (TEE)?**

Secure Software refers to the **Trusted Execution Environment (TEE)**, a secure area of the device's main processor. It provides a sandboxed environment to store and process cryptographic keys securely, but it is not physically isolated like Secure Hardware.

### Key Features of TEE:
- **Software Isolation**: The TEE is a secure part of the main CPU that runs a separate, trusted OS to handle sensitive operations.
- **Secure Key Storage**: Cryptographic keys are stored in the TEE and are protected from the main operating system and apps.
- **Widely Available**: Most Android devices support TEE-based security, even if they lack dedicated Secure Hardware.

### Limitations of TEE:
- **Lower Security Guarantee**: While still secure, TEE is less resistant to physical attacks compared to Secure Hardware.

---

## **Comparison Table**

| Feature                         | Secure Hardware (StrongBox)    | Secure Software (TEE)         |
|---------------------------------|--------------------------------|--------------------------------|
| **Level of Isolation**          | Physically isolated chip       | Secure area of the main CPU   |
| **Hardware Dependency**         | Requires dedicated hardware    | Available on most devices     |
| **Performance**                 | Slightly slower                | Generally faster              |
| **Physical Attack Resistance**  | High                           | Moderate                      |
| **Key Usage**                   | Hardware-backed cryptography   | Software-backed cryptography  |

---

## **How Does It Affect `react-native-keychain`?**

When using `react-native-keychain` on Android, the library relies on the Android KeyStore system to store and manage cryptographic keys. The level of security provided depends on the device and its capabilities:

1. **StrongBox Enabled Devices**: If a device supports StrongBox, `react-native-keychain` can store keys in the **Secure Hardware**, offering the highest level of security.
2. **TEE-Only Devices**: If StrongBox is not available, the keys are stored in the **TEE**, which is still secure but less resistant to physical attacks.

### **Fallback Behavior in `react-native-keychain`**:
`react-native-keychain` automatically uses the best available security mechanism. If StrongBox is unavailable, it falls back to TEE.