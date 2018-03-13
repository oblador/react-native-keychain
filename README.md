<p align="center"><img src="https://user-images.githubusercontent.com/378279/36642269-6195b10c-1a3d-11e8-9e1b-37a3d1bcf7b3.png" align="center" width="150" height="201" alt="" /></p>

<h1 align="center">react-native-keychain</h1>

[![Travis](https://img.shields.io/travis/oblador/react-native-keychain.svg)](https://travis-ci.org/oblador/react-native-keychain) [![npm](https://img.shields.io/npm/v/react-native-keychain.svg)](https://npmjs.com/package/react-native-keychain) [![npm](https://img.shields.io/npm/dm/react-native-keychain.svg)](https://npmjs.com/package/react-native-keychain)

Keychain/Keystore Access for React Native. 

## Installation

1. `$ yarn add react-native-keychain`
2. `$ react-native link react-native-keychain` and check `MainApplication.java` to verify the package was added.
3. Rebuild your project with `react-native run-ios/android`

See manual installation below if you have issues with `react-native link`.

## Usage

```js
import * as Keychain from 'react-native-keychain';

async () => {
  const username = 'zuck';
  const password = 'poniesRgr8';

  // Store the credentials
  await Keychain.setGenericPassword(username, password);

  try {
    // Retreive the credentials
    const credentials = await Keychain.getGenericPassword();
    if (credentials) {
      console.log('Credentials successfully loaded for user ' + credentials.username);
    } else {
      console.log('No credentials stored')
    }
  } catch (error) {
    console.log('Keychain couldn\'t be accessed!', error);
  }
  await Keychain.resetGenericPassword()
}
```

See `KeychainExample` for fully working project example.

Both `setGenericPassword` and `setInternetCredentials` are limited to strings only, so if you need to store objects etc, please use `JSON.stringify`/`JSON.parse` when you store/access it.

### `setGenericPassword(username, password, [{ accessControl, accessible, accessGroup, service }])`

Will store the username/password combination in the secure storage. Resolves to `true` or rejects in case of an error.

### `getGenericPassword([{ authenticationPrompt, service }])`

Will retreive the username/password combination from the secure storage. Resolves to `{ username, password }` if an entry exists or `false` if it doesn't. It will reject only if an unexpected error is encountered like lacking entitlements or permission.

### `resetGenericPassword([{ service }])`

Will remove the username/password combination from the secure storage.

### `setInternetCredentials(server, username, password, [{ accessControl, accessible, accessGroup }])`

Will store the server/username/password combination in the secure storage.

### `getInternetCredentials(server, [{ authenticationPrompt }])`

Will retreive the server/username/password combination from the secure storage. Resolves to `{ username, password }` if an entry exists or `false` if it doesn't. It will reject only if an unexpected error is encountered like lacking entitlements or permission.

### `resetInternetCredentials(server)`

Will remove the server/username/password combination from the secure storage.

### `requestSharedWebCredentials()` (iOS only)

Asks the user for a shared web credential. Requires additional setup both in the app and server side, see [Apple documentation](https://developer.apple.com/documentation/security/shared_web_credentials). Resolves to `{ server, username, password }` if approved and `false` if denied and throws an error if not supported on platform or there's no shared credentials.

### `setSharedWebCredentials(server, username, password)` (iOS only)

Sets a shared web credential. Resolves to `true` when successful.

### `canImplyAuthentication([{ authenticationType }])`

Inquire if the type of local authentication policy is supported on this device with the device settings the user chose. Should be used in combination with `accessControl` option in the setter functions. Resolves to `true` if supported.

### `getSupportedBiometryType()`

Get what type of hardware biometry support the device has. Resolves to a `Keychain.BIOMETRY_TYPE` value when supported, otherwise `null`.

### Options

| Key | Platform | Description | Default |
|---|---|---|---|
|**`accessControl`**|iOS only|This dictates how a keychain item may be used, see possible values in `Keychain.ACCESS_CONTROL`. |*None*|
|**`accessible`**|iOS only|This dictates when a keychain item is accessible, see possible values in `Keychain.ACCESSIBLE`. |*`Keychain.ACCESSIBLE.WHEN_UNLOCKED`*|
|**`accessGroup`**|iOS only|In which App Group to share the keychain. Requires additional setup with entitlements. |*None*|
|**`authenticationPrompt`**|iOS only|What to prompt the user when unlocking the keychain with biometry or device password. |`Authenticate to retrieve secret`|
|**`authenticationType`**|iOS only|Policies specifying which forms of authentication are acceptable. |`Keychain.AUTHENTICATION_TYPE.DEVICE_PASSCODE_OR_BIOMETRICS`|
|**`service`**|All|Reverse domain name qualifier for the service associated with password. |*App bundle ID*|

#### `Keychain.ACCESS_CONTROL` enum

| Key | Description |
|-----|-------------|
|**`USER_PRESENCE`**|Constraint to access an item with either Touch ID or passcode.|
|**`BIOMETRY_ANY`**|Constraint to access an item with Touch ID for any enrolled fingers.|
|**`BIOMETRY_CURRENT_SET`**|Constraint to access an item with Touch ID for currently enrolled fingers.|
|**`DEVICE_PASSCODE`**|Constraint to access an item with a passcode.|
|**`APPLICATION_PASSWORD`**|Constraint to use an application-provided password for data encryption key generation.|
|**`BIOMETRY_ANY_OR_DEVICE_PASSCODE`**|Constraint to access an item with Touch ID for any enrolled fingers or passcode.|
|**`BIOMETRY_CURRENT_SET_OR_DEVICE_PASSCODE`**|Constraint to access an item with Touch ID for currently enrolled fingers or passcode.|

#### `Keychain.ACCESSIBLE` enum

| Key | Description |
|-----|-------------|
|**`WHEN_UNLOCKED`**|The data in the keychain item can be accessed only while the device is unlocked by the user.|
|**`AFTER_FIRST_UNLOCK`**|The data in the keychain item cannot be accessed after a restart until the device has been unlocked once by the user.|
|**`ALWAYS`**|The data in the keychain item can always be accessed regardless of whether the device is locked.|
|**`WHEN_PASSCODE_SET_THIS_DEVICE_ONLY`**|The data in the keychain can only be accessed when the device is unlocked. Only available if a passcode is set on the device. Items with this attribute never migrate to a new device.|
|**`WHEN_UNLOCKED_THIS_DEVICE_ONLY`**|The data in the keychain item can be accessed only while the device is unlocked by the user. Items with this attribute do not migrate to a new device.|
|**`AFTER_FIRST_UNLOCK_THIS_DEVICE_ONLY`**|The data in the keychain item cannot be accessed after a restart until the device has been unlocked once by the user. Items with this attribute never migrate to a new device.|
|**`ALWAYS_THIS_DEVICE_ONLY`**|The data in the keychain item can always be accessed regardless of whether the device is locked. Items with this attribute never migrate to a new device.|

#### `Keychain.AUTHENTICATION_TYPE` enum

| Key | Description |
|-----|-------------|
|**`DEVICE_PASSCODE_OR_BIOMETRICS`**|Device owner is going to be authenticated by biometry or device passcode.|
|**`BIOMETRICS`**|Device owner is going to be authenticated using a biometric method (Touch ID or Face ID).|

#### `Keychain.BIOMETRY_TYPE` enum

| Key | Description |
|-----|-------------|
|**`TOUCH_ID`**|Device supports authentication with Touch ID.|
|**`FACE_ID`**|Device supports authentication with Face ID.|
|**`FINGERPRINT`**|Device supports authentication with Android Fingerprint.|

## Manual Installation

### iOS

#### Option: Manually

* Right click on Libraries, select **Add files to "…"** and select `node_modules/react-native-keychain/RNKeychain.xcodeproj`
* Select your project and under **Build Phases** -> **Link Binary With Libraries**, press the + and select `libRNKeychain.a`.

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

* Edit `android/settings.gradle` to look like this (without the +):

```diff
rootProject.name = 'MyApp'

include ':app'

+ include ':react-native-keychain'
+ project(':react-native-keychain').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-keychain/android')
```

* Edit `android/app/build.gradle` (note: **app** folder) to look like this: 

```diff
apply plugin: 'com.android.application'

android {
  ...
}

dependencies {
  compile fileTree(dir: 'libs', include: ['*.jar'])
  compile 'com.android.support:appcompat-v7:23.0.1'
  compile 'com.facebook.react:react-native:0.19.+'
+ compile project(':react-native-keychain')
}
```

* Edit your `MainApplication.java` (deep in `android/app/src/main/java/...`) to look like this (note **two** places to edit):

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

## Notes

### Android 

The module will automatically use the appropriate CipherStorage implementation based on API level:

* API level 16-22 will en/de crypt using Facebook Conceal
* API level 23+ will en/de crypt using Android Keystore

Encrypted data is stored in SharedPreferences.

The `setInternetCredentials(server, username, password)` call will be resolved as call to `setGenericPassword(username, password, server)`. Use the `server` argument to distinguish between multiple entries.

### Security

On API levels that do not support Android keystore, Facebook Conceal is used to en/decrypt stored data. The encrypted data is then stored in SharedPreferences. Since Conceal itself stores its encryption key in SharedPreferences, it follows that if the device is rooted (or if an attacker can somehow access the filesystem), the key can be obtained and the stored data can be decrypted. Therefore, on such a device, the conceal encryption is only an obscurity. On API level 23+ the key is stored in the Android Keystore, which makes the key non-exportable and therefore makes the entire process more secure. Follow best practices and do not store user credentials on a device. Instead use tokens or other forms of authentication and re-ask for user credentials before performing sensitive operations.

## Maintainers

<table>
  <tbody>
    <tr>
      <td align="center">
        <a href="https://github.com/oblador">
          <img width="150" height="150" src="https://github.com/oblador.png?v=3&s=150">
          <br>
          <strong>Joel Arvidsson</strong>
        </a>
        <br>
        Author
      </td>
      <td align="center">
        <a href="https://github.com/vonovak">
          <img width="150" height="150" src="https://github.com/vonovak.png?v=3&s=150">
          </br>
          <strong>Vojtech Novak</strong>
        </a>
        <br>
        Maintainer
      </td>
      <td align="center">
        <a href="https://github.com/pcoltau">
          <img width="150" height="150" src="https://github.com/pcoltau.png?v=3&s=150">
          </br>
          <strong>Pelle Stenild Coltau</strong>
        </a>
        <br>
        Maintainer
      </td>
    </tr>
  <tbody>
</table>


## License
MIT © Joel Arvidsson 2016-2018
