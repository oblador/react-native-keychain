const path = require('path');

module.exports = {
  dependency: {
    platforms: {
      ios: { podspecPath: path.join(__dirname, 'RNKeychain.podspec') },
      android: {
        packageImportPath: 'import com.oblador.keychain.KeychainPackage;',
        packageInstance: 'new KeychainPackage()',
      },
    },
  },
};
