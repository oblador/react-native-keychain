<h1 align="center"><img
    src="website/static/img/logo.png"
    align="center" width="50" height="50" alt=""
  /> react-native-keychain</h1>

[![Tests](https://github.com/oblador/react-native-keychain/actions/workflows/e2e_tests.yaml/badge.svg)](https://github.com/oblador/react-native-keychain/actions/workflows/e2e_tests.yaml) [![npm](https://img.shields.io/npm/v/react-native-keychain.svg)](https://npmjs.com/package/react-native-keychain) [![npm](https://img.shields.io/npm/dm/react-native-keychain.svg)](https://npmjs.com/package/react-native-keychain)

This library provides access to the Keychain (iOS) and Keystore (Android) for securely storing credentials like passwords, tokens, or other sensitive information in React Native apps.

- [Installation](#installation)
- [Documentation](#documentation)
- [Changelog](#changelog)
- [Maintainers](#maintainers)
- [License](#license)

## Installation

1. Run `yarn add react-native-keychain`

   1 a. **Only for React Native <= 0.59**: `$ react-native link react-native-keychain` and check `MainApplication.java` to verify the package was added. See manual installation below if you have issues with `react-native link`.

2. Run `pod install` in `ios/` directory to install iOS dependencies.
3. If you want to support FaceID, add a `NSFaceIDUsageDescription` entry in your `Info.plist`.
4. Re-build your Android and iOS projects.

## Documentation

Please refer to the documentation website on https://oblador.github.io/react-native-keychain

## Changelog

Check the [GitHub Releases page](https://github.com/oblador/react-native-keychain/releases).

## Maintainers

<table>
  <tbody>
    <tr>
      <td align="center">
        <a href="https://github.com/oblador">
          <img width="100" height="100" src="https://github.com/oblador.png?v=3&s=150">
          <br />
          <strong>Joel Arvidsson</strong>
        </a>
        <br />
        Author
      </td>
      <td align="center">
        <a href="https://github.com/DorianMazur">
          <img width="100" height="100" src="https://github.com/DorianMazur.png?v=3&s=150">
          <br />
          <strong>Dorian Mazur</strong>
        </a>
        <br />
        Maintainer
      </td>
      <td align="center">
        <a href="https://github.com/vonovak">
          <img width="100" height="100" src="https://github.com/vonovak.png?v=3&s=150">
          <br />
          <strong>Vojtech Novak</strong>
        </a>
        <br />
        Maintainer
      </td>
      <td align="center">
        <a href="https://github.com/pcoltau">
          <img width="100" height="100" src="https://github.com/pcoltau.png?v=3&s=150">
          <br />
          <strong>Pelle Stenild Coltau</strong>
        </a>
        <br />
        Maintainer
      </td>
      <td align="center">
        <a href="https://github.com/OleksandrKucherenko">
          <img width="100" height="100" src="https://github.com/OleksandrKucherenko.png?v=3&s=150">
          <br />
          <strong>Oleksandr Kucherenko</strong>
        </a>
        <br />
        Contributor
      </td>
    </tr>
  <tbody>
</table>

## License

MIT © Joel Arvidsson 2016-2020
