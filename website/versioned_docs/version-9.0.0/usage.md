---
id: usage
title: Usage
---

## Basic Usage

To use `react-native-keychain`, follow the example below to securely store and retrieve credentials.

```tsx
import * as Keychain from 'react-native-keychain';

async () => {
  const username = 'zuck';
  const password = 'poniesRgr8';

  // Store the credentials
  await Keychain.setGenericPassword(username, password);

  try {
    // Retrieve the credentials
    const credentials = await Keychain.getGenericPassword();
    if (credentials) {
      console.log(
        'Credentials successfully loaded for user ' + credentials.username
      );
    } else {
      console.log('No credentials stored');
    }
  } catch (error) {
    console.error("Failed to access Keychain", error);
  }

  // Reset the stored credentials
  await Keychain.resetGenericPassword();
};
```

See the `KeychainExample` for a fully working project example.

> **Note**: Both `setGenericPassword` and `setInternetCredentials` only support strings. If you need to store objects, use `JSON.stringify` when storing and `JSON.parse` when retrieving.

## Advanced Usage

### Android

The module automatically selects the appropriate `CipherStorage` implementation based on the device's API level:

- **API levels 16-22**: Uses Facebook Conceal for encryption/decryption.
- **API level 23+**: Uses Android Keystore for encryption/decryption.

Encrypted data is stored in `SharedPreferences`.

#### Multiple Credentials

When using `setInternetCredentials(server, username, password)`, the `server` argument is treated as a key to distinguish between multiple entries. Internally, this is equivalent to calling `setGenericPassword(username, password, server)`.

### Configuring Android-Specific Behavior

Due to inconsistencies in Android implementations across different manufacturers, some devices (e.g., Samsung) may experience slow startup times for the cryptographic system. To address this, the Android implementation includes a "warm-up" strategy by default.

#### Default Warm-Up Behavior

By default, the warm-up strategy is enabled, as shown below:

```java
private List<ReactPackage> createPackageList() {
  return Arrays.asList(
    ...
    new KeychainPackage(),  // warming up is ON by default
    ...
  );
}
```

#### Disabling Warm-Up

If you need more control over the behavior, you can disable warm-up by using the builder pattern:

```java
private List<ReactPackage> createPackageList() {
  return Arrays.asList(
    ...
    new KeychainPackage(
      new KeychainModuleBuilder()
        .withoutWarmUp()  // warming up is OFF
    ),
    ...
  );
}
```

### iOS

To share the Keychain between your main app and a Share Extension in iOS, ensure the following:

1. Use the same App Group and Keychain Sharing group names in both your Main App and Share Extension.
2. Utilize the `accessGroup` and `service` options when calling `setGenericPassword` and `getGenericPassword`:

```tsx
Keychain.getGenericPassword({
  accessGroup: 'group.appname', 
  service: 'com.example.appname'
});
```

## References

- [Apple Local Authentication Documentation](https://developer.apple.com/documentation/localauthentication)
- [Apple Security Documentation](https://developer.apple.com/documentation/security)