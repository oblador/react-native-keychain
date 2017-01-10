# react-native-keychain
Keychain Access for React Native

Currently functionality is limited to just storing internet and generic passwords. 

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

## Installation

`$ npm install --save react-native-keychain`

### Option: Manually

* Right click on Libraries, select **Add files to "…"** and select `node_modules/react-native-keychain/RNKeychain.xcodeproj`
* Select your project and under **Build Phases** -> **Link Binary With Libraries**, press the + and select `libRNKeychain.a`.

### Option: With [CocoaPods](https://cocoapods.org/)

Add the following to your `Podfile` and run `pod update`:

```
pod 'RNKeychain', :path => 'node_modules/react-native-keychain'
```

### Option: With `react-native link`

`$ react-native link`

## Usage

See `KeychainExample` for fully working project example.

```js
import * as Keychain from 'react-native-keychain';

let username = 'zuck';
let password = 'poniesRgr8';

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
var server = 'http://facebook.com';
Keychain
  .setInternetCredentials(server, username, password)
  .then(function() {
    console.log('Credentials saved successfully!');
  });

Keychain
  .getInternetCredentials(server)
  .then(function(credentials) {
    console.log('Credentials successfully loaded for user ' + credentials.username);
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

```

### Android

### Option: With `react-native link`

`$ react-native link` and check MainApplication.java to verify the package was added.

* Note: Android support requires React Native 0.19 or later
* on Android, the `setInternetCredentials(server, username, password)` call will be resolved as call to `setGenericPassword(username, password, server)` and the data will be saved in `SharedPreferences`, encrypted using Facebook conceal. Use the `server` argument to distinguish between multiple entries.


### Option: Manually


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

## Todo

- [x] iOS support
- [x] Android support
- [ ] Storing objects as JSON
- [ ] Expose wider selection of underlying native APIs

## License
MIT © Joel Arvidsson 2016
