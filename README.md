<h1 align="center"><img
    src="website/static/img/logo.png"
    align="center" width="50" height="50" alt=""
  /> react-native-keychain</h1>

[![Tests](https://github.com/oblador/react-native-keychain/actions/workflows/e2e_tests.yaml/badge.svg)](https://github.com/oblador/react-native-keychain/actions/workflows/e2e_tests.yaml) [![npm](https://img.shields.io/npm/v/react-native-keychain.svg)](https://npmjs.com/package/react-native-keychain) [![npm](https://img.shields.io/npm/dm/react-native-keychain.svg)](https://npmjs.com/package/react-native-keychain)

This library provides access to the Keychain (iOS) and Keystore (Android) for securely storing credentials like passwords, tokens, or other sensitive information in React Native apps.

- [Installation](#installation)
- [Example app](#example-app)
- [Documentation](#documentation)
- [Changelog](#changelog)
- [Maintainers](#maintainers)
- [Used By](#used-by)
- [License](#license)

## Installation

React Native **0.82 or later** with the **New Architecture** is required for the upcoming 11.x release. Use 10.x for older React Native versions or the Old Architecture. The iOS minimum is **15.1** and Android minimum is **API 24**. Development requires **Node.js 20.19.4+**, **JDK 17**, and **Xcode 16.1+**.

1. Run `yarn add react-native-keychain`
2. Run `pod install` in `ios/` directory to install iOS dependencies.
3. If you want to support FaceID, add a `NSFaceIDUsageDescription` entry in your `Info.plist`.
4. Re-build your Android and iOS projects.

## Example app

The [example app](KeychainExample) lets you save, load, and reset generic passwords and internet credentials, choose protection options, and inspect the returned service or server and storage type. Retrieved passwords stay hidden until you reveal them.

| iOS                                                                                                                                                         | Android                                                                                                                                                  |
| ----------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| <img src="KeychainExample/media/ios-demo.gif" width="260" alt="iOS example: select Face ID protection, save credentials, and inspect the loaded result." /> | <img src="KeychainExample/media/android-demo.gif" width="260" alt="Android example: choose cipher storage, save credentials, and inspect the result." /> |
| [Watch the iOS demo](KeychainExample/media/ios-demo.mp4)                                                                                                    | [Watch the Android demo](KeychainExample/media/android-demo.mp4)                                                                                         |

To run it locally, install workspace dependencies with `yarn install`. For iOS, also run `bundle install` and `bundle exec pod install --project-directory=ios` from `KeychainExample/`. Then run `yarn workspace KeychainExample ios` or `yarn workspace KeychainExample android` from the repository root.

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

## Used By

This library is used by several projects, including:

- [Rainbow Wallet](https://github.com/rainbow-me/rainbow)
- [MetaMask Mobile](https://github.com/MetaMask/metamask-mobile)
- [BlueWallet](https://github.com/bluewallet/bluewallet)

## License

MIT © Joel Arvidsson 2016-2020
