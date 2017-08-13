# react-native-keychain
Keychain Access for React Native. Currently functionality is limited to just storing internet and generic passwords.

### New 2.0.0-rc with improved android implementation

install using `yarn add react-native-keychain@2.0.0-rc`

The KeychainModule will now automatically use the appropriate CipherStorage implementation based on API level:

* API level 16-22 will en/de crypt using Facebook Conceal
* API level 23+ will en/de crypt using Android Keystore

Encrypted data is stored in SharedPreferences.

## Installation

1 . `$ npm install --save react-native-keychain`

or

`$ yarn add react-native-keychain`


2 . `$ react-native link` and check `MainApplication.java` to verify the package was added.

3 .  rebuild your project


* on Android, the `setInternetCredentials(server, username, password)` call will be resolved as call to `setGenericPassword(username, password, server)`. Use the `server` argument to distinguish between multiple entries.

Check out the "releases" tab for breaking changes and RN version compatibility. v1.0.0 is for RN >= 0.40

## ❗ Enable `Keychain Sharing` entitlement for iOS 10

For iOS 10 you'll need to enable the `Keychain Sharing` entitlement in the `Capabilities` section of your build target. (See screenshot). Otherwise you'll experience the error shown below.

![screen shot 2016-09-16 at 20 56 33](https://cloud.githubusercontent.com/assets/512692/18597833/15316342-7c50-11e6-92e7-781651e61563.png)

```
Error: {
    code = "-34018";
    domain = NSOSStatusErrorDomain;
    message = "The operation couldn\U2019t be completed. (OSStatus error -34018.)";
}
```


## Usage

See `KeychainExample` for fully working project example.

```js
import * as Keychain from 'react-native-keychain';

const username = 'zuck';
const password = 'poniesRgr8';

// Generic Password, service argument optional
Keychain
  .setGenericPassword(username, password)
  .then(function() {
    console.log('Credentials saved successfully!');
  });

// service argument optional
Keychain
  .getGenericPassword()
  .then(function(credentials) {
    console.log('Credentials successfully loaded for user ' + credentials.username);
  }).catch(function(error) {
    console.log('Keychain couldn\'t be accessed! Maybe no value set?', error);
  });

// service argument optional
Keychain
  .resetGenericPassword()
  .then(function() {
    console.log('Credentials successfully deleted');
  });

// Internet Password, server argument required
const server = 'http://facebook.com';
Keychain
  .setInternetCredentials(server, username, password)
  .then(function() {
    console.log('Credentials saved successfully!');
  });

Keychain
  .getInternetCredentials(server)
  .then(function(credentials) {
    if (credentials) {
      console.log('Credentials successfully loaded for user ' + credentials.username);
    }
  });

Keychain
  .resetInternetCredentials(server)
  .then(function() {
    console.log('Credentials successfully deleted');
  });

Keychain
  .requestSharedWebCredentials()
  .then(function(credentials) {
    if (credentials) {
      console.log('Shared web credentials successfully loaded for user ' + credentials.username);
    } 
  })

Keychain
  .setSharedWebCredentials(server, username, password)
  .then(function() {
    console.log('Shared web credentials saved successfully!');
  })

```

### Note on security

On API levels that do not support Android keystore, Facebook Conceal is used to en/decrypt stored data. The encrypted data is then stored in SharedPreferences. Since Conceal itself stores its encryption key in SharedPreferences, it follows that if the device is rooted (or if an attacker can somehow access the filesystem), the key can be obtained and the stored data can be decrypted. Therefore, on such a device, the conceal encryption is only an obscurity. On API level 23+ the key is stored in the Android Keystore, which makes the key non-exportable and therefore makes the entire process more secure. Follow best practices and do not store user credentials on a device. Instead use tokens or other forms of authentication and re-ask for user credentials before performing sensitive operations.

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
MIT © Joel Arvidsson 2016-2017
