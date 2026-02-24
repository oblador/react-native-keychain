---
id: knox-integration
title: Samsung Knox Integration
---

Samsung Knox provides hardware-backed security for Samsung devices, offering an additional layer of protection for sensitive data. This library includes built-in support for Knox encryption on compatible Samsung devices.

## What is Samsung Knox?

Samsung Knox is a defense-grade security platform built into Samsung devices. It provides:

- **Hardware-backed encryption**: Keys are stored in a secure hardware enclave
- **FIPS 140-2 compliance**: Meets stringent government security requirements
- **Enhanced key protection**: Additional isolation from the main operating system

## Knox Storage Types

The library provides Knox storage in two variants:

### 1. Knox with Authentication (`STORAGE_TYPE.KNOX`)

- Requires biometric or device passcode authentication
- Best for highly sensitive data like passwords and tokens
- Uses hardware-backed encryption when available

### 2. Knox without Authentication (Internal)

- No authentication required for access
- Still uses Knox hardware security
- Best for encrypted app data that doesn't need auth

## Platform Support

### Knox Availability

Knox storage is available on:
- **Samsung devices** with Knox platform (most Samsung devices since 2013)
- **API Level 23+** (Android 6.0+)

On non-Samsung devices, Knox storage will gracefully fall back to standard Android Keystore encryption if the `useKnox` flag is used. However, if `STORAGE_TYPE.KNOX` is explicitly requested via the `storage` option, the operation will fail with an error if Knox is unavailable.

### Knox Security Architecture

Samsung Knox provides **superior hardware security** compared to standard Android TEE implementations:

#### **Knox Vault (API 31+) - Highest Security**

Knox Vault represents the pinnacle of mobile hardware security, introduced in Galaxy S21 and newer flagship devices:

- **Isolated Security Processor**: Operates completely independently from the main Android OS processor
- **Dedicated Secure Memory**: Has its own processor, memory, and secure storage separate from main system
- **StrongBox Keymaster**: Integrates Android's StrongBox specification with Knox's enhanced security
- **Common Criteria EAL4+ Certified**: Independently tested and certified for high assurance
- **Tamper-Resistant Design**: Protected against both software and hardware attacks including:
  - Physical probing
  - Side-channel attacks
  - Fault injection attacks
  - Software exploits
- **Security Sensors**: Continuously monitors hardware status and environment for tampering

**What Knox Vault Protects**:
- Hardware-backed Android Keystore keys
- Biometric authentication data
- Blockchain private keys
- Security-critical authentication code

#### **TIMA KeyStore (API 23-30) - Enhanced TEE**

For devices running Android 6.0 through Android 10, Knox uses:

- **TrustZone-based Integrity Measurement Architecture (TIMA)**: Samsung's enhanced TEE implementation
- **Hardware-Rooted Trust**: Verifies system integrity from boot-up
- **Tamper Detection**: Real-time kernel protection and integrity monitoring
- **FIPS 140-2 Level 1 Validated**: Government-grade cryptographic modules

#### **Android Keystore (Fallback)**

On non-Samsung devices:
- Falls back to standard Android Keystore (TEE or software-based)
- Maintains compatibility across all Android devices
- Still provides secure encryption, but without Knox-specific enhancements

### Why Knox is More Secure Than Standard Android TEE

| Security Feature | Knox Vault (Samsung) | Standard Android TEE |
|------------------|---------------------|----------------------|
| **Processor Isolation** | Dedicated independent processor | Secure area on main processor |
| **Memory Isolation** | Dedicated secure memory | Shared memory with isolation |
| **Attack Resistance** | Protected against hardware attacks (probing, side-channel, fault injection) | Protected against software attacks |
| **Independence** | Operates completely separately from Android OS | Shares main SoC resources |
| **Certifications** | Common Criteria EAL4+, FIPS 140-2 | Varies by implementation |
| **Tamper Detection** | Hardware sensors monitoring environment | Software-based integrity checks |
| **FIPS 140-2 Compliance** | Level 1 validated cryptographic modules | Not typically certified |

**Key Advantage**: Knox Vault's complete independence from the main processor means that even if the Android OS is fully compromised, the secrets stored in Knox Vault remain protected. Standard TEE, while secure, shares resources with the main SoC which can be vulnerable to more sophisticated attacks.

## Usage

### Using Knox Storage

```typescript
import Keychain from 'react-native-keychain';

// Save credentials with Knox encryption
await Keychain.setGenericPassword(
  'username',
  'super_secret_password',
  {
    storage: Keychain.STORAGE_TYPE.KNOX,
    accessControl: Keychain.ACCESS_CONTROL.BIOMETRY_ANY_OR_DEVICE_PASSCODE,
  }
);

// Retrieve credentials
const credentials = await Keychain.getGenericPassword({
  storage: Keychain.STORAGE_TYPE.KNOX,
});

if (credentials) {
  console.log('Username:', credentials.username);
  console.log('Password:', credentials.password);
}
```

### Checking Knox Availability

You can check if Knox is available on the current device:

```typescript
import Keychain from 'react-native-keychain';

const knoxAvailable = await Keychain.isKnoxAvailable();

if (knoxAvailable) {
  console.log('Knox encryption is available on this device');
  // Use Knox storage
} else {
  console.log('Knox not available, will use standard encryption');
  // Use standard storage types
}
```

## When to Use Knox

### Use Knox Storage For:

✅ **Government and Enterprise Apps**
- Apps requiring FIPS 140-2 compliance
- Government contracts requiring Knox certification
- Enterprise apps with strict security policies

✅ **High-Security Applications**
- Banking and financial apps
- Healthcare apps handling PHI/HIPAA data
- Apps storing highly sensitive user data

✅ **Samsung-Exclusive Apps**
- Apps distributed exclusively on Samsung devices
- Apps leveraging Samsung-specific features

### Don't Use Knox For:

❌ **Cross-Platform Consistency**
- If you need identical behavior on all Android devices
- Use `AES_GCM` or `AES_GCM_NO_AUTH` instead

❌ **Simple Use Cases**
- Standard credential storage works fine with `AES_GCM`
- Knox adds complexity without significant benefit for most apps

## Knox vs Standard Android Keystore

| Feature | Knox Vault (Samsung Flagship) | TIMA KeyStore (Older Samsung) | Android Keystore TEE | Android Keystore (Software) |
|---------|------------------------------|-------------------------------|---------------------|----------------------------|
| **Hardware Protection** | **Dedicated secure processor** (highest) | Enhanced TEE with TIMA | Standard TEE | Software only |
| **Isolation Level** | **Complete independence** from main OS | Enhanced TrustZone isolation | Shared main SoC with isolation | Process-level only |
| **FIPS 140-2** | **Yes** (Level 1 validated) | **Yes** (Level 1 validated) | No (varies) | No |
| **Common Criteria** | **EAL4+** certified | Varies | No | No |
| **Attack Resistance** | **Hardware + Software** attacks | Software attacks + tamper detection | Software attacks | Software only |
| **Tamper Detection** | **Hardware sensors** | Real-time kernel monitoring | Limited | None |
| **Government/Enterprise** | **Certified** for use | **Certified** for use | Not certified | Not certified |
| **Device Compatibility** | Samsung flagships (S21+) | Samsung devices (API 23-30) | Most Android devices | All Android devices |
| **Performance** | Similar | Similar | Similar | Similar |
| **Complexity** | Higher (requires Knox checks) | Higher (requires Knox checks) | Medium | Low |

### Security Level Ranking (Highest to Lowest)

1. **🏆 Knox Vault** - Isolated secure processor, hardware tamper detection, EAL4+ certified
2. **🥈 Knox TIMA** - Enhanced TEE, FIPS 140-2 validated, real-time kernel protection  
3. **🥉 Android Keystore (TEE)** - Standard TEE implementation
4. **Android Keystore (Software)** - Software-based encryption only

> **Important**: Knox (both Vault and TIMA) provides **stronger security** than standard Android TEE implementations due to enhanced isolation, certifications (FIPS 140-2, Common Criteria), and additional security features like tamper detection and hardware-rooted trust.

## Best Practices

### 1. Check Availability Before Use

Always check if Knox is available before using Knox storage:

```typescript
const knoxAvailable = await Keychain.isKnoxAvailable();
const storageType = knoxAvailable 
  ? Keychain.STORAGE_TYPE.KNOX 
  : Keychain.STORAGE_TYPE.AES_GCM;

await Keychain.setGenericPassword(username, password, {
  storage: storageType,
});
```

### 2. Handle Migration

If transitioning from standard storage to Knox:

```typescript
// Check if credentials exist with old storage
const oldCredentials = await Keychain.getGenericPassword({
  storage: Keychain.STORAGE_TYPE.AES_GCM,
});

if (oldCredentials && knoxAvailable) {
  // Migrate to Knox storage
  await Keychain.setGenericPassword(
    oldCredentials.username,
    oldCredentials.password,
    { storage: Keychain.STORAGE_TYPE.KNOX }
  );
  
  // Remove old credentials
  await Keychain.resetGenericPassword({
    storage: Keychain.STORAGE_TYPE.AES_GCM,
  });
}
```

### 3. Test on Multiple Devices

- Test on Samsung devices with Knox
- Test on non-Samsung devices (fallback behavior)
- Verify authentication prompts work correctly

## Security Considerations

### Knox Vault Provides (Highest Security):

- **Complete Processor Isolation**: Independent secure processor operating separately from main Android OS
- **Dedicated Secure Memory**: Own processor, memory, and storage isolated from main system
- **Hardware Tamper Resistance**: Protected against physical attacks (probing, side-channel, fault injection)
- **Security Monitoring**: Hardware sensors continuously monitor for tampering attempts
- **Common Criteria EAL4+**: Independently certified to high assurance security levels
- **FIPS 140-2 Level 1**: Validated cryptographic modules meeting government standards
- **StrongBox Integration**: Implements Android's StrongBox with Knox enhancements

### Knox TIMA Provides (Enhanced TEE Security):

- **Hardware-Rooted Trust**: TrustZone-based secure boot and integrity verification
- **Real-Time Kernel Protection**: Active monitoring and protection of kernel integrity
- **FIPS 140-2 Level 1**: Government-grade cryptographic validation
- **Tamper Detection**: Software-based integrity measurement and monitoring
- **Enhanced TrustZone**: Samsung's improvements over standard ARM TrustZone

### Security Advantages Over Standard Android:

**Knox Vault vs Standard TEE**:
- Knox Vault operates on a **completely separate processor**, while standard TEE shares the main SoC
- **Hardware tamper detection** vs software-only security monitoring
- **Certified security** (EAL4+, FIPS 140-2) vs uncertified TEE implementations
- Protected against **hardware attacks** that can compromise standard TEE

**Knox TIMA vs Standard TEE**:
- **FIPS 140-2 validated** cryptographic modules vs uncertified implementations
- **Real-time kernel protection** beyond standard Android security
- **Hardware-rooted trust** chain from boot-up
- Enhanced security tailored for enterprise and government use

### What Knox Doesn't Protect Against:

- User-level attacks (phishing, social engineering)
- App-level vulnerabilities (code injection, XSS)
- Network attacks (MITM if HTTPS is compromised)

## Troubleshooting

### Knox Not Available

If `isKnoxAvailable()` returns `false` on a Samsung device:

1. **Check API Level**: Knox requires API 23+ (Android 6.0+)
2. **Knox Version**: Some older Samsung devices may have limited Knox support
3. **Device Integrity**: Knox may be disabled on rooted or modified devices

### Authentication Issues

If biometric authentication fails with Knox storage:

- Ensure biometrics are enrolled on the device
- Check that `accessControl` is set correctly
- Verify device passcode is set (required for fallback)

### Performance Concerns

Knox initialization may add slight overhead on first use:

```typescript
// Perform Knox check during app startup to warm up
const knoxAvailable = await Keychain.isKnoxAvailable();
// Cache result for later use
```

## Additional Resources

- [Samsung Knox Documentation](https://docs.samsungknox.com/)
- [Knox SDK](https://docs.samsungknox.com/dev/)
- [FIPS 140-2 Compliance](https://csrc.nist.gov/projects/cryptographic-module-validation-program)
