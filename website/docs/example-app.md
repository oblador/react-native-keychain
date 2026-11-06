# Example app

Try credential storage and authentication on a simulator or device with the included Keychain workbench.

The app lets you switch between generic passwords and internet credentials, choose protection options, and inspect the result of each operation. Retrieved passwords stay hidden until you reveal them.

## Try the credential flow

1. Select **Generic password** or **Internet credentials**.
2. Enter a username and password.
3. Choose the protection settings for your platform.
4. Tap **Save**, then **Load** to read the saved entry.
5. Tap **Reset** to clear both credential types in the selected synchronization scope.

Use **List** to inspect the stored service or server identifiers. On iOS, try the cloud synchronization setting with the same identifier to see that local and synchronizable entries are separate.

## Run locally

From the repository root, install the workspace dependencies using the Node version in `.node-version` and the Yarn version in `package.json`:

```sh
yarn install --immutable
```

For iOS, install Ruby dependencies and pods:

```sh
cd KeychainExample
bundle install
bundle exec pod install --project-directory=ios
```

Start Metro from the repository root:

```sh
yarn workspace KeychainExample start
```

Then, in another terminal at the repository root:

::: code-group

```sh [iOS]
yarn workspace KeychainExample ios
```

```sh [Android]
yarn workspace KeychainExample android
```

:::

## Explore the source

- [Example application](https://github.com/oblador/react-native-keychain/tree/master/KeychainExample)
- [iOS demo](https://github.com/oblador/react-native-keychain/blob/master/KeychainExample/media/ios-demo.mp4)
- [Android demo](https://github.com/oblador/react-native-keychain/blob/master/KeychainExample/media/android-demo.mp4)
- [Native regression tests](https://github.com/oblador/react-native-keychain/tree/master/KeychainExample/e2e/testCases)

Simulator tests can verify query behavior. Use real devices to verify hardware protection and the authentication behavior you ship.
