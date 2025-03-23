---
id: choosing-storage-type
title: Choosing Storage Type
---

On Android, securing sensitive data requires careful consideration of different storage types. This guide helps you choose the right storage type.

## Security Levels

We offer three security levels for data storage:

### 1. High Security (with Biometric Authentication)

- **AES_GCM**: Symmetric encryption with biometric protection
- **RSA**: Asymmetric encryption with biometric protection
- Best for: Passwords, personal data, sensitive keys

### 2. Medium Security (without Authentication)

- **AES_GCM_NO_AUTH**: Symmetric encryption without biometric requirements
- Best for: Cached data, non-sensitive encrypted data

### 3. Legacy/Deprecated

- **AES_CBC**, **FB** (Facebook Conceal)
- ⚠️ Not recommended for new implementations

## Storage Type Selection Guide

### Use AES_GCM (High Security) for:

- User credentials (passwords, tokens)
- Personal information (SSN, credit cards)
- Encryption keys (symmetric keys)

```typescript
import { setGenericPassword, STORAGE_TYPE } from 'react-native-keychain';

await setGenericPassword(
  'username',
  'password123',
  {
    accessControl: ACCESS_CONTROL.BIOMETRY_ANY,
    accessible: ACCESSIBLE.WHEN_UNLOCKED,
    storage: STORAGE_TYPE.AES_GCM,
  }
);
```

### Use RSA (High Security) for:

- Digital signatures
- Key exchange
- Public/private key operations

```typescript
import { setGenericPassword, STORAGE_TYPE } from 'react-native-keychain';

await setGenericPassword(
  'auth_key_id',
  privateKeyData,
  {
    accessControl: ACCESS_CONTROL.BIOMETRY_ANY,
    accessible: ACCESSIBLE.WHEN_UNLOCKED,
    storage: STORAGE_TYPE.RSA,
  }
);
```

### Use AES_GCM_NO_AUTH (Medium Security) for:

- Cache data
- Local data
- Temporary storage

```typescript
import { setGenericPassword, STORAGE_TYPE } from 'react-native-keychain';

await setGenericPassword(
  'cache_key',
  cacheData,
  {
    accessible: ACCESSIBLE.WHEN_UNLOCKED,
    storage: STORAGE_TYPE.AES_GCM_NO_AUTH,
  }
);
```

## Security Considerations

### AES_GCM
- Requires biometric authentication for both encryption and decryption
- Uses 256-bit keys
- Provides authenticated encryption (confidentiality + integrity)
- Best choice for most sensitive data

### RSA
- Requires biometric authentication for private key operations
- Uses 2048-bit keys
- Limited by maximum encryption size
- Best for asymmetric operations

### AES_GCM_NO_AUTH
- No biometric requirement
- Still provides authenticated encryption
- Good balance of security and usability for non-sensitive data
