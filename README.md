# react-native-keychain
Keychain Access for React Native

Currently functionality is limited to just storing internet and generic passwords. 

## Installation

`$ npm install react-native-keychain`

### Option: Manually

* Right click on Libraries, select **Add files to "…"** and select `node_modules/react-native-keychain/RNKeychain.xcodeproj`
* Select your project and under **Build Phases** -> **Link Binary With Libraries**, press the + and select `libRNKeychain.a`.

### Option: With [CocoaPods](https://cocoapods.org/)

Add the following to your `Podfile` and run `pod update`:

```
pod 'RNKeychain', :path => 'node_modules/react-native-keychain'
```

### Option: With [rnpm](https://github.com/rnpm/rnpm)

`$ rnpm link`

## Usage

See `KeychainExample` for fully working project example.

```js
var Keychain = require('react-native-keychain');

var username = 'zuck';
var password = 'poniesRgr8';

// Generic Password, service argument optional
Keychain
  .setGenericPassword(username, password)
  .then(function() {
    console.log('Credentials saved successfully!');
  });

Keychain
  .getGenericPassword()
  .then(function(credentials) {
    console.log('Credentials successfully loaded for user ' + credentials.username);
  });

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

```

### Android

* Note: Android support requires React Native 0.19 or later
* on Android, the `*InternetCredentials` calls will be resolved as calls to `*GenericPassword()` and the data will be saved in `SharedPreferences`, encrypted using Facebook conceal.

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

* Edit your `MainActivity.java` (deep in `android/app/src/main/java/...`) to look like this (note **two** places to edit):

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
MIT © Joel Arvidsson 2015
