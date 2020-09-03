<h1 align="center">react-native-keychain</h1>

<p align="center"><img
    src="https://user-images.githubusercontent.com/378279/36642269-6195b10c-1a3d-11e8-9e1b-37a3d1bcf7b3.png"
    align="center" width="150" height="201" alt=""
  />
</p>

[![Travis](https://img.shields.io/travis/oblador/react-native-keychain.svg)](https://travis-ci.org/oblador/react-native-keychain) [![npm](https://img.shields.io/npm/v/react-native-keychain.svg)](https://npmjs.com/package/react-native-keychain) [![npm](https://img.shields.io/npm/dm/react-native-keychain.svg)](https://npmjs.com/package/react-native-keychain)

# Keychain/Keystore Access for React Native

- [Keychain/Keystore Access for React Native](#keychainkeystore-access-for-react-native)
  - [Installation](#installation)
  - [Usage](#usage)
  - [API](#api)
    - [`setGenericPassword(username, password, [{ accessControl, accessible, accessGroup, service, securityLevel }])`](#setgenericpasswordusername-password--accesscontrol-accessible-accessgroup-service-securitylevel-)
    - [`getGenericPassword([{ authenticationPrompt, service, accessControl }])`](#getgenericpassword-authenticationprompt-service-accessControl-)
    - [`resetGenericPassword([{ service }])`](#resetgenericpassword-service-)
    - [`setInternetCredentials(server, username, password, [{ accessControl, accessible, accessGroup, securityLevel }])`](#setinternetcredentialsserver-username-password--accesscontrol-accessible-accessgroup-securitylevel-)
    - [`hasInternetCredentials(server)`](#hasinternetcredentialsserver)
    - [`getInternetCredentials(server, [{ authenticationPrompt }])`](#getinternetcredentialsserver--authenticationprompt-)
    - [`resetInternetCredentials(server)`](#resetinternetcredentialsserver)
    - [`requestSharedWebCredentials()` (iOS only)](#requestsharedwebcredentials-ios-only)
    - [`setSharedWebCredentials(server, username, password)` (iOS only)](#setsharedwebcredentialsserver-username-password-ios-only)
    - [`canImplyAuthentication([{ authenticationType }])` (iOS only)](#canimplyauthentication-authenticationtype--ios-only)
    - [`getSupportedBiometryType()`](#getsupportedbiometrytype)
    - [`getSecurityLevel([{ accessControl }])` (Android only)](#getsecuritylevel-accesscontrol--android-only)
    - [Options](#options)
      - [Data Structure Properties/Fields](#data-structure-propertiesfields)
      - [`Keychain.ACCESS_CONTROL` enum](#keychainaccess_control-enum)
      - [`Keychain.ACCESSIBLE` enum](#keychainaccessible-enum)
      - [`Keychain.AUTHENTICATION_TYPE` enum](#keychainauthentication_type-enum)
      - [`Keychain.BIOMETRY_TYPE` enum](#keychainbiometry_type-enum)
      - [`Keychain.SECURITY_LEVEL` enum (Android only)](#keychainsecurity_level-enum-android-only)
      - [`Keychain.STORAGE_TYPE` enum (Android only)](#keychainstorage_type-enum-android-only)
      - [`Keychain.RULES` enum (Android only)](#keychainrules-enum-android-only)
  - [Important Behavior](#important-behavior)
    - [Rule 1: Automatic Security Level Upgrade](#rule-1-automatic-security-level-upgrade)
  - [Manual Installation](#manual-installation)
    - [iOS](#ios)
      - [Option: Manually](#option--manually-)
      - [Option: With CocoaPods](#option-with-cocoapods)
      - [Enable `Keychain Sharing` entitlement for iOS 10+](#enable-keychain-sharing-entitlement-for-ios-10)
    - [Android](#android)
      - [Option: Manually](#option-manually-1)
      - [Proguard Rules](#proguard-rules)
  - [Unit Testing with Jest](#unit-testing-with-jest)
    - [Using a Jest `__mocks__` Directory](#using-a-jest-__mocks__-directory)
    - [Using a Jest Setup File](#using-a-jest-setup-file)
  - [Notes](#notes)
    - [Android Notes](#android-notes)
    - [iOS Notes](#ios-notes)
    - [macOS Catalyst](#macos-catalyst)
    - [Security](#security)
  - [Maintainers](#maintainers)
  - [For Developers / Contributors](#for-developers--contributors)
  - [License](#license)

## Installation

1. Run `yarn add react-native-keychain`

   1 a. **Only for React Native <= 0.59**: `$ react-native link react-native-keychain` and check `MainApplication.java` to verify the package was added. See manual installation below if you have issues with `react-native link`.

2. Run `pod install` in `ios/` directory to install iOS dependencies.
3. If you want to support FaceID, add a `NSFaceIDUsageDescription` entry in your `Info.plist`.
4. Re-build your Android and iOS projects.

## Usage

```js
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
    console.log("Keychain couldn't be accessed!", error);
  }
  await Keychain.resetGenericPassword();
};
```

See `KeychainExample` for fully working project example.

Both `setGenericPassword` and `setInternetCredentials` are limited to strings only, so if you need to store objects etc, please use `JSON.stringify`/`JSON.parse` when you store/access it.

## API

### `setGenericPassword(username, password, [{ accessControl, accessible, accessGroup, service, securityLevel }])`

Will store the username/password combination in the secure storage. Resolves to `{service, storage}` or rejects in case of an error. `storage` - is a name of used internal cipher for saving secret; `service` - name used for storing secret in internal storage (empty string resolved to valid default name).

### `getGenericPassword([{ authenticationPrompt, service, accessControl }])`

Will retrieve the username/password combination from the secure storage. Resolves to `{ username, password, service, storage }` if an entry exists or `false` if it doesn't. It will reject only if an unexpected error is encountered like lacking entitlements or permission.

### `resetGenericPassword([{ service }])`

Will remove the username/password combination from the secure storage. Resolves to `true` in case of success.

### `setInternetCredentials(server, username, password, [{ accessControl, accessible, accessGroup, securityLevel }])`

Will store the server/username/password combination in the secure storage. Resolves to `{ username, password, service, storage }`;

### `hasInternetCredentials(server)`

Will check if the username/password combination for server is available in the secure storage. Resolves to `true` if an entry exists or `false` if it doesn't.

### `getInternetCredentials(server, [{ authenticationPrompt }])`

Will retrieve the server/username/password combination from the secure storage. Resolves to `{ username, password }` if an entry exists or `false` if it doesn't. It will reject only if an unexpected error is encountered like lacking entitlements or permission.

### `resetInternetCredentials(server)`

Will remove the server/username/password combination from the secure storage.

### `requestSharedWebCredentials()` (iOS only)

Asks the user for a shared web credential. Requires additional setup both in the app and server side, see [Apple documentation](https://developer.apple.com/documentation/security/shared_web_credentials). Resolves to `{ server, username, password }` if approved and `false` if denied and throws an error if not supported on platform or there's no shared credentials.

### `setSharedWebCredentials(server, username, password)` (iOS only)

Sets a shared web credential. Resolves to `true` when successful.

### `canImplyAuthentication([{ authenticationType }])` (iOS only)

Inquire if the type of local authentication policy is supported on this device with the device settings the user chose. Should be used in combination with `accessControl` option in the setter functions. Resolves to `true` if supported.

### `getSupportedBiometryType()`

Get what type of hardware biometry support the device has. Resolves to a `Keychain.BIOMETRY_TYPE` value when supported, otherwise `null`.

> This method returns `null`, if the device haven't enrolled into fingerprint/FaceId. Even though it has hardware for it.

### `getSecurityLevel([{ accessControl }])` (Android only)

Get security level that is supported on the current device with the current OS. Resolves to `Keychain.SECURITY_LEVEL` enum value.

### Options

#### Data Structure Properties/Fields

| Key                        | Platform     | Description                                                                                      | Default                                                                   |
| -------------------------- | ------------ | ------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------- |
| **`accessControl`**        | All          | This dictates how a keychain item may be used, see possible values in `Keychain.ACCESS_CONTROL`. | _None_                                                                    |
| **`accessible`**           | iOS only     | This dictates when a keychain item is accessible, see possible values in `Keychain.ACCESSIBLE`.  | _`Keychain.ACCESSIBLE.WHEN_UNLOCKED`_                                     |
| **`accessGroup`**          | iOS only     | In which App Group to share the keychain. Requires additional setup with entitlements.           | _None_                                                                    |
| **`authenticationPrompt`** | All          | What to prompt the user when unlocking the keychain with biometry or device password.            | See [`authenticationPrompt` Properties](#authenticationprompt-properties) |
| **`authenticationType`**   | iOS only     | Policies specifying which forms of authentication are acceptable.                                | `Keychain.AUTHENTICATION_TYPE.DEVICE_PASSCODE_OR_BIOMETRICS`              |
| **`service`**              | All          | Reverse domain name qualifier for the service associated with password.                          | _App bundle ID_                                                           |
| **`storage`**              | Android only | Force specific cipher storage usage during saving the password                                   | Select best available storage                                             |
| **`rules`**                | Android only | Force following to a specific security rules                                                     | `Keychain.RULES.AUTOMATIC_UPGRADE`                                        |

##### `authenticationPrompt` Properties

| Key               | Platform     | Description                                                                                | Default                           |
| ----------------- | ------------ | ------------------------------------------------------------------------------------------ | --------------------------------- |
| **`title`**       | All          | Title of the authentication prompt when requesting a stored secret.                        | `Authenticate to retrieve secret` |
| **`subtitle`**    | Android only | Subtitle of the Android authentication prompt when requesting a stored secret.             | None. Optional                    |
| **`description`** | Android only | Description of the Android authentication prompt when requesting a stored secret.          | None. Optional                    |
| **`cancel`**      | Android only | Negative button text of the Android authentication prompt when requesting a stored secret. | `Cancel`                          |

#### `Keychain.ACCESS_CONTROL` enum

| Key                                           | Description                                                                            |
| --------------------------------------------- | -------------------------------------------------------------------------------------- |
| **`USER_PRESENCE`**                           | Constraint to access an item with either Touch ID or passcode.                         |
| **`BIOMETRY_ANY`**                            | Constraint to access an item with Touch ID for any enrolled fingers.                   |
| **`BIOMETRY_CURRENT_SET`**                    | Constraint to access an item with Touch ID for currently enrolled fingers.             |
| **`DEVICE_PASSCODE`**                         | Constraint to access an item with a passcode.                                          |
| **`APPLICATION_PASSWORD`**                    | Constraint to use an application-provided password for data encryption key generation. |
| **`BIOMETRY_ANY_OR_DEVICE_PASSCODE`**         | Constraint to access an item with Touch ID for any enrolled fingers or passcode.       |
| **`BIOMETRY_CURRENT_SET_OR_DEVICE_PASSCODE`** | Constraint to access an item with Touch ID for currently enrolled fingers or passcode. |

> Note #1: `BIOMETRY_ANY`, `BIOMETRY_CURRENT_SET`, `BIOMETRY_ANY_OR_DEVICE_PASSCODE`, `BIOMETRY_CURRENT_SET_OR_DEVICE_PASSCODE` - recognized by Android as a requirement for Biometric enabled storage (Till we got a better implementation);
>
> Note #2: For Android we support only two states: `None` (default) and `Fingerprint` (use only biometric protected storage); `Face` recognition fails with "User not authenticated" exception, see issue #318

Refs:

- <https://developer.apple.com/documentation/security/secaccesscontrolcreateflags?language=objc>

#### `Keychain.ACCESSIBLE` enum

| Key                                       | Description                                                                                                                                                                            |
| ----------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`WHEN_UNLOCKED`**                       | The data in the keychain item can be accessed only while the device is unlocked by the user.                                                                                           |
| **`AFTER_FIRST_UNLOCK`**                  | The data in the keychain item cannot be accessed after a restart until the device has been unlocked once by the user.                                                                  |
| **`ALWAYS`**                              | The data in the keychain item can always be accessed regardless of whether the device is locked.                                                                                       |
| **`WHEN_PASSCODE_SET_THIS_DEVICE_ONLY`**  | The data in the keychain can only be accessed when the device is unlocked. Only available if a passcode is set on the device. Items with this attribute never migrate to a new device. |
| **`WHEN_UNLOCKED_THIS_DEVICE_ONLY`**      | The data in the keychain item can be accessed only while the device is unlocked by the user. Items with this attribute do not migrate to a new device.                                 |
| **`AFTER_FIRST_UNLOCK_THIS_DEVICE_ONLY`** | The data in the keychain item cannot be accessed after a restart until the device has been unlocked once by the user. Items with this attribute never migrate to a new device.         |
| **`ALWAYS_THIS_DEVICE_ONLY`**             | The data in the keychain item can always be accessed regardless of whether the device is locked. Items with this attribute never migrate to a new device.                              |

Refs:

- <https://developer.apple.com/documentation/security/ksecattraccessiblewhenunlocked>

#### `Keychain.AUTHENTICATION_TYPE` enum

| Key                                 | Description                                                                               |
| ----------------------------------- | ----------------------------------------------------------------------------------------- |
| **`DEVICE_PASSCODE_OR_BIOMETRICS`** | Device owner is going to be authenticated by biometry or device passcode.                 |
| **`BIOMETRICS`**                    | Device owner is going to be authenticated using a biometric method (Touch ID or Face ID). |

Refs:

- <https://developer.apple.com/documentation/localauthentication/lapolicy>

#### `Keychain.BIOMETRY_TYPE` enum

| Key               | Description                                                          |
| ----------------- | -------------------------------------------------------------------- |
| **`TOUCH_ID`**    | Device supports authentication with Touch ID. (iOS only)             |
| **`FACE_ID`**     | Device supports authentication with Face ID. (iOS only)              |
| **`FINGERPRINT`** | Device supports authentication with Fingerprint. (Android only)      |
| **`FACE`**        | Device supports authentication with Face Recognition. (Android only) |
| **`IRIS`**        | Device supports authentication with Iris Recognition. (Android only) |

Refs:

- <https://developer.apple.com/documentation/localauthentication/labiometrytype?language=objc>

#### `Keychain.SECURITY_LEVEL` enum (Android only)

If set, `securityLevel` parameter specifies minimum security level that the encryption key storage should guarantee for storing credentials to succeed.

| Key               | Description                                                                                                                                                                                                                            |
| ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ANY`             | no security guarantees needed (default value); Credentials can be stored in FB Secure Storage;                                                                                                                                         |
| `SECURE_SOFTWARE` | requires for the key to be stored in the Android Keystore, separate from the encrypted data;                                                                                                                                           |
| `SECURE_HARDWARE` | requires for the key to be stored on a secure hardware (Trusted Execution Environment or Secure Environment). Read [this article](https://developer.android.com/training/articles/keystore#ExtractionPrevention) for more information. |

#### `Keychain.STORAGE_TYPE` enum (Android only)

| Key   | Description                            |
| ----- | -------------------------------------- |
| `FB`  | Facebook compatibility cipher          |
| `AES` | Encryptions without human interaction. |
| `RSA` | Encryption with biometrics.            |

#### `Keychain.RULES` enum (Android only)

| Key                 | Description                                                                                                                                                 |
| ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `NONE`              | No rules. Be dummy, developer control everything                                                                                                            |
| `AUTOMATIC_UPGRADE` | Upgrade secret to the best available storage as soon as it is available and user request secret extraction. Upgrade not applied till we request the secret. |

## Important Behavior

### Rule 1: Automatic Security Level Upgrade

As a rule library try to apply the best possible encryption and access method for storing secrets.

What does it mean in practical use case?

> Scenario #1: User has a new phone and run on it an application with this module and store secret on device.
> Several days later user configures biometrics on the device and run application again. When the user will try to access the secret, the library will detect security enhancement and will upgrade secret storage to the best possible.

---

Q: What will happen if user disables/drops biometric usage?

A: User will lose ability to extract secret from storage. On re-enable biometric access to the secret will be possible again.

---

Q: Is it possible to implement automatic downgrading?

A: From security perspective any Automatic downgrading is treated as "a loss of the trust" point.
Developer should implement own logic to allow downgrade and deal with "security loss". _(My recommendation - never do that!)_

---

Q: How to disable automatic upgrade?

A: Do call `getGenericPassword({ ...otherProps, rules: "none" })` with extra property `rules` set to `none` string value.

---

Q: How to force a specific level of encryption during saving the secret?

A: Do call `setGenericPassword({ ...otherProps, storage: "AES" })` with forced storage.

> Note: attempt to force storage `RSA` when biometrics is not available will force code to reject call with errors specific to device biometric configuration state.

## Manual Installation

### iOS

#### Option: Manually

- Right click on Libraries, select **Add files to "…"** and select `node_modules/react-native-keychain/RNKeychain.xcodeproj`
- Select your project and under **Build Phases** -> **Link Binary With Libraries**, press the + and select `libRNKeychain.a`.
- make sure `pod 'RNKeychain'` is not in your `Podfile`

#### Option: With [CocoaPods](https://cocoapods.org/)

Add the following to your `Podfile` and run `pod update`:

```
pod 'RNKeychain', :path => '../node_modules/react-native-keychain'
```

#### Enable `Keychain Sharing` entitlement for iOS 10+

For iOS 10 you'll need to enable the `Keychain Sharing` entitlement in the `Capabilities` section of your build target. (See screenshot). Otherwise you'll experience the error shown below.

![screen shot 2016-09-16 at 20 56 33](https://cloud.githubusercontent.com/assets/512692/18597833/15316342-7c50-11e6-92e7-781651e61563.png)

```
Error: {
  code = "-34018";
  domain = NSOSStatusErrorDomain;
  message = "The operation couldn\U2019t be completed. (OSStatus error -34018.)";
}
```

### Android

#### Option: Manually

- Edit `android/settings.gradle` to look like this (without the +):

```diff
rootProject.name = 'MyApp'

include ':app'

+ include ':react-native-keychain'
+ project(':react-native-keychain').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-keychain/android')
```

- Edit `android/app/build.gradle` (note: **app** folder) to look like this:

```diff
apply plugin: 'com.android.application'

android {
  ...
}

dependencies {
  implementation fileTree(dir: 'libs', include: ['*.jar'])
  implementation 'com.android.support:appcompat-v7:23.0.1'
  implementation 'com.facebook.react:react-native:0.19.+'
+ implementation project(':react-native-keychain')
}
```

- Edit your `MainApplication.java` (deep in `android/app/src/main/java/...`) to look like this (note **two** places to edit):

```diff
package com.myapp;

+ import com.oblador.keychain.KeychainPackage;

....

public class MainActivity extends extends ReactActivity {

  @Override
  protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
              new MainReactPackage(),
+             new KeychainPackage()
      );
  }
  ...
}
```

#### Proguard Rules

On Android builds that use proguard (like release), you may see the following error:

```
RNKeychainManager: no keychain entry found for service:
JNI DETECTED ERROR IN APPLICATION: JNI FindClass called with pending exception java.lang.NoSuchFieldError: no "J" field "mCtxPtr" in class "Lcom/facebook/crypto/cipher/NativeGCMCipher;" or its superclasses
```

If so, add a proguard rule in `proguard-rules.pro`:

```
-keep class com.facebook.crypto.** {
   *;
}
```

## Unit Testing with Jest

The keychain manager relies on interfacing with the native application itself. As such, it does not successfully compile and run in the context of a Jest test, where there is no underlying app to communicate with. To be able to call the JS functions exposed by this module in a unit test, you should mock them in one of the following two ways:

First, let's create a mock object for the module:

```js
const keychainMock = {
  SECURITY_LEVEL_ANY: "MOCK_SECURITY_LEVEL_ANY",
  SECURITY_LEVEL_SECURE_SOFTWARE: "MOCK_SECURITY_LEVEL_SECURE_SOFTWARE",
  SECURITY_LEVEL_SECURE_HARDWARE: "MOCK_SECURITY_LEVEL_SECURE_HARDWARE",
  setGenericPassword: jest.fn().mockResolvedValue(),
  getGenericPassword: jest.fn().mockResolvedValue(),
  resetGenericPassword: jest.fn().mockResolvedValue(),
  ...
}
```

### Using a Jest `__mocks__` Directory

1. Read the [jest docs](https://jestjs.io/docs/en/manual-mocks#mocking-node-modules) for initial setup

2. Create a `react-native-keychain` folder in the `__mocks__` directory and add `index.js` file in it. It should contain the following code:

```javascript
export default keychainMock;
```

### Using a Jest Setup File

1. In your Jest config, add a reference to a [setup file](https://jestjs.io/docs/en/configuration.html#setupfiles-array)

2. Inside your setup file, set up mocking for this package:

```javascript
jest.mock('react-native-keychain', () => keychainMock);
```

Now your tests should run successfully, though note that writing and reading to the keychain will be effectively a no-op.

## Notes

### Android Notes

The module will automatically use the appropriate CipherStorage implementation based on API level:

- API level 16-22 will en/de crypt using Facebook Conceal
- API level 23+ will en/de crypt using Android Keystore

Encrypted data is stored in SharedPreferences.

The `setInternetCredentials(server, username, password)` call will be resolved as call to `setGenericPassword(username, password, server)`. Use the `server` argument to distinguish between multiple entries.

#### Configuring the Android-specific behavior

Android implementation has behavioural specifics incurred by existing inconsistency between implementations by different vendors. E.g., some Samsung devices show very slow startup of crypto system. To alleviate this, a warm-up strategy is introduced in Android implementation of this library. 

Using default constructor you get default behaviour, i.e. with the warming up on.
```java
    private List<ReactPackage> createPackageList() {
      return Arrays.asList(
        ...
        new KeychainPackage(),  // warming up is ON
        ...
      )

``` 
Those who want finer control are required to use constructor with a builder which can be configured as they like:
```java
    private List<ReactPackage> createPackageList() {
      return Arrays.asList(
        ...
        new KeychainPackage(
                new KeychainModuleBuilder()
                        .withoutWarmUp()),   // warming up is OFF
        ...
      )
```

### iOS Notes

If you need Keychain Sharing in your iOS extension, make sure you use the same App Group and Keychain Sharing group names in your Main App and your Share Extension. To then share the keychain between the Main App and Share Extension, use the `accessGroup` and `service` option on `setGenericPassword` and `getGenericPassword`, like so: `getGenericPassword({ accessGroup: 'group.appname', service: 'com.example.appname' })`

Refs:

- <https://developer.apple.com/documentation/localauthentication>
- <https://developer.apple.com/documentation/security>

### macOS Catalyst

This package supports macOS Catalyst.

### Security

On API levels that do not support Android keystore, Facebook Conceal is used to en/decrypt stored data. The encrypted data is then stored in SharedPreferences. Since Conceal itself stores its encryption key in SharedPreferences, it follows that if the device is rooted (or if an attacker can somehow access the filesystem), the key can be obtained and the stored data can be decrypted. Therefore, on such a device, the conceal encryption is only an obscurity. On API level 23+ the key is stored in the Android Keystore, which makes the key non-exportable and therefore makes the entire process more secure. Follow best practices and do not store user credentials on a device. Instead use tokens or other forms of authentication and re-ask for user credentials before performing sensitive operations.

![Android Security Framework](https://source.android.com/security/images/authentication-flow.png)

- [Android authentication](https://source.android.com/security/authentication)
- [Android Cipher](https://developer.android.com/guide/topics/security/cryptography)
- [Android Protected Confirmation](https://developer.android.com/training/articles/security-android-protected-confirmation)

## Maintainers

<table>
  <tbody>
    <tr>
      <td align="center">
        <a href="https://github.com/oblador">
          <img width="150" height="150" src="https://github.com/oblador.png?v=3&s=150">
          <br />
          <strong>Joel Arvidsson</strong>
        </a>
        <br />
        Author
      </td>
      <td align="center">
        <a href="https://github.com/vonovak">
          <img width="150" height="150" src="https://github.com/vonovak.png?v=3&s=150">
          <br />
          <strong>Vojtech Novak</strong>
        </a>
        <br />
        Maintainer
      </td>
      <td align="center">
        <a href="https://github.com/pcoltau">
          <img width="150" height="150" src="https://github.com/pcoltau.png?v=3&s=150">
          <br />
          <strong>Pelle Stenild Coltau</strong>
        </a>
        <br />
        Maintainer
      </td>
      <td align="center">
        <a href="https://github.com/OleksandrKucherenko">
          <img width="150" height="150" src="https://github.com/OleksandrKucherenko.png?v=3&s=150">
          <br />
          <strong>Oleksandr Kucherenko</strong>
        </a>
        <br />
        Contributor
      </td>
    </tr>
  <tbody>
</table>

## For Developers / Contributors

- [How to Configure Android Studio for Development](android/README.md)

## License

MIT © Joel Arvidsson 2016-2020
