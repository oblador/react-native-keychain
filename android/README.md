# Android Development

## Android Studio Configuration

![Compound Configuration](https://i.imgur.com/woB4iiJh.png)

### Application Runner

![Run application with custom before launch step](https://i.imgur.com/zX7QIGIh.png)

> Important! to set checkbox `Allow parallel run`.

```bash
# cd react-native-keychain
./gradlew :android
```

### Unit Tests

![Configure Execution of all Unit Tests](https://i.imgur.com/vjDVPYhh.png)

```bash
# cd react-native-keychain
./gradlew test
```

### Start React Native Metro Bundler

![React Native Start](https://i.imgur.com/nvLZ9Fph.png)

```bash
# cd react-native-keychain/KeychainExample
react-native start --reset-cache
```

```bash
# set working dir to: 'react-native-keychain/KeychainExample'
/usr/bin/env node node_modules/.bin/react-native start --reset-cache
```

> Important! to set checkbox `Allow parallel run`.

### Create Automatic self-refreshed TCP ports binding

![ADB reverse tcp port 8081](https://i.imgur.com/IatGcsVh.png)

```bash
# brew install watch
/usr/local/bin/watch -n 5 "adb reverse tcp:8081 tcp:8081 && adb reverse tcp:8097 tcp:8097 && adb reverse --list"
```

> Important! to set checkbox `Allow parallel run`.

### Source code synchronization task

Needed for automatic re-publishing of changes source code for sample:

![Configure Source Code Synchronization Task Run](https://i.imgur.com/BqVWThh.png)

The same results can be achieved by executing this command:

```bash
# cd react-native-keychain/KeychainExample
yarn --force

# cd react-native-keychain
./gradlew updateLibrarySourcesInExample
```
