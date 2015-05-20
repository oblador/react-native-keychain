# react-native-keychain
Keychain Access for React Native

Currently functionality is limited to just storing internet passwords. More to come... 

## Installation

* `$ npm install react-native-keychain`
* Right click on Libraries, select **Add files to "…"** and select `node_modules/react-native-keychain/RNKeychain.xcodeproj`
* Select your project and under **Build Phases** -> **Link Binary With Libraries**, press the + and select `libRNKeychain.a`.


## Usage

See `KeychainExample` for fully working project example.

```js
var Keychain = require('Keychain');

var server = 'http://facebook.com';
var username = 'zuck';
var password = 'poniesRgr8';
Keychain
  .setInternetCredentials(server, username, password)
  .then(function() {
    console.log('Credentials saved successfully!')
  });

Keychain
  .getInternetCredentials(server)
  .then(function(credentials) {
    console.log('Credentials successfully loaded', credentials)
  });

Keychain
  .resetInternetCredentials(server)
  .then(function(credentials) {
    console.log('Credentials successfully deleted')
  });

```

## License
MIT © Joel Arvidsson 2015
