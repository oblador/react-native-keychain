---
id: choosing-storage-type
title: '[Android] Choosing Storage Type'
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

## Complete Examples

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

<Tabs>
<TabItem value="biometrics" label="With Biometrics">

```typescript
import { 
  setGenericPassword, 
  getGenericPassword, 
  getSupportedBiometryType,
  STORAGE_TYPE,
  ACCESS_CONTROL,
  ACCESSIBLE,
} from 'react-native-keychain';

// Store credentials with biometric protection
async function storeCredentials(username: string, password: string) {
  try {
    // Check biometric availability
    const biometryType = await getSupportedBiometryType();
    
    // Handle biometric requirement
    if (options.requireBiometrics && !biometryType) {
      throw new Error('Biometric authentication is required but not available');
    }

    await setGenericPassword(username, password, {
      accessControl: ACCESS_CONTROL.BIOMETRY_ANY,
      accessible: ACCESSIBLE.WHEN_UNLOCKED,
      storage: STORAGE_TYPE.AES_GCM,
      authenticationType: AUTHENTICATION_TYPE.BIOMETRICS,
    });
    console.log('Credentials stored successfully');
  } catch (error) {
    console.error('Error storing credentials:', error);
  }
}

// Retrieve credentials (will trigger biometric prompt)
async function retrieveCredentials() {
  try {
    const credentials = await getGenericPassword();
    if (credentials) {
      console.log('Username:', credentials.username);
      console.log('Password:', credentials.password);
      return credentials;
    }
    return null;
  } catch (error) {
    console.error('Error retrieving credentials:', error);
    return null;
  }
}
```
</TabItem>
<TabItem value="no-biometrics" label="Without Biometrics">

```typescript
import { 
  setGenericPassword, 
  getGenericPassword, 
  STORAGE_TYPE,
  ACCESS_CONTROL,
  ACCESSIBLE,
} from 'react-native-keychain';

// Store credentials with biometric protection
async function storeCredentials(username: string, password: string) {
  try {
    await setGenericPassword(username, password, {
      accessControl: ACCESS_CONTROL.BIOMETRY_ANY,
      accessible: ACCESSIBLE.WHEN_UNLOCKED,
      storage: STORAGE_TYPE.AES_GCM_NO_AUTH,
      authenticationType: AUTHENTICATION_TYPE.BIOMETRICS,
    });
    console.log('Credentials stored successfully');
  } catch (error) {
    console.error('Error storing credentials:', error);
  }
}

// Retrieve credentials (will trigger biometric prompt)
async function retrieveCredentials() {
  try {
    const credentials = await getGenericPassword();
    if (credentials) {
      console.log('Username:', credentials.username);
      console.log('Password:', credentials.password);
      return credentials;
    }
    return null;
  } catch (error) {
    console.error('Error retrieving credentials:', error);
    return null;
  }
}
```
</TabItem>
</Tabs>

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


## Best Practices

1. Always use biometric protection (`AES_GCM` or `RSA`) for sensitive user data
2. Use `AES_GCM_NO_AUTH` for non-sensitive but private app data
3. Handle biometric authentication errors gracefully
4. Provide fallback mechanisms when biometrics are not available
5. Clear sensitive data when the user logs out
6. Don't store sensitive data in plain text or with deprecated storage types
