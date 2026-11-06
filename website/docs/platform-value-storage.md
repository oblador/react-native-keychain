# Platform storage

The same JavaScript API uses different native storage systems on Apple platforms and Android.

## Apple Keychain

Generic credentials use `kSecClassGenericPassword`; internet credentials use `kSecClassInternetPassword`. The `accessible` option controls when an item can be accessed, and `accessControl` adds an authentication policy.

| Option          | What it controls                                            |
| --------------- | ----------------------------------------------------------- |
| `accessible`    | When the item is available, such as after the first unlock. |
| `accessControl` | Whether authentication is required to access the item.      |
| `accessGroup`   | The entitled Keychain group that owns the item.             |
| `cloudSync`     | Whether to select synchronizable items.                     |

Keep the service or server, access group, and synchronization setting consistent across operations. See [Data persistence](./data-persistence.md) for synchronization and reinstall behavior.

## Android Keystore

Encrypted credentials are stored in Jetpack DataStore. The encryption keys are managed by Android Keystore.

When `storage` is omitted, the library selects a cipher using the Android version, requested access control, and available authentication methods. Without `storage` or `accessControl`, it selects storage that does not require user authentication. The returned `storage` field identifies the cipher used.

### Storage types

| Storage           | Authentication | Use                                        |
| ----------------- | -------------- | ------------------------------------------ |
| `AES_GCM`         | Required       | AES-GCM with user authentication.          |
| `RSA`             | Required       | RSA with user authentication.              |
| `AES_GCM_NO_AUTH` | Not required   | AES-GCM without an authentication prompt.  |
| `AES_CBC`         | Not required   | Legacy entries; deprecated for new writes. |

Authentication requirements and hardware protection are separate choices. An unauthenticated cipher still encrypts credentials; it simply does not require the user to approve access.

```ts
import * as Keychain from 'react-native-keychain';

await Keychain.setGenericPassword('session', 'example-token', {
  service: 'com.example.session',
  storage: Keychain.STORAGE_TYPE.AES_GCM_NO_AUTH,
});
```

Reads use the cipher recorded with the entry. Changing the default for new writes does not automatically re-encrypt existing credentials. See [Common questions](./faq.md) before upgrading older Android installations.

### Security levels

[`SECURITY_LEVEL`](./api/enumerations/SECURITY_LEVEL.md) expresses the required protection of Android key material:

- `ANY` accepts the available level.
- `SECURE_SOFTWARE` requires at least software protection.
- `SECURE_HARDWARE` requires hardware-backed protection.

Hardware-backed keys can use a Trusted Execution Environment (TEE) or StrongBox. A TEE is not the same as software-only key storage, and `SECURE_HARDWARE` does not specifically promise StrongBox. See [Android Keystore](https://developer.android.com/privacy-and-security/keystore) for the platform's security model.

A device may not satisfy the requested level. Handle a rejected operation without silently lowering the requirement. Emulator results do not establish hardware-backed security on a real device.
