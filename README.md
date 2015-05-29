# react-native-keychain
Keychain Access for React Native

Currently functionality is limited to just storing internet and generic passwords. Wider exposure of the underlying API coming.

## Installation

* `$ npm install react-native-keychain`
* Right click on Libraries, select **Add files to "…"** and select `node_modules/react-native-keychain/RNKeychain.xcodeproj`
* Select your project and under **Build Phases** -> **Link Binary With Libraries**, press the + and select `libRNKeychain.a`.


## Usage

See `KeychainExample` for fully working project example.

```js
var Keychain = require('Keychain');

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

## License
MIT © Joel Arvidsson 2015
