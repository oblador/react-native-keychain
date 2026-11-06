# Testing the library

Tests use real platform storage. The dedicated `e2e/app` entry imports the public
JavaScript API; it does not replace native methods or mock authentication. The
normal example still starts from `index.js`.

| Layer                                   | Responsibility                                                                                                                                                                                        | CI                                               |
| --------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------ |
| Android `src/test`                      | Fast policy intersection and metadata classification checks; no device needed                                                                                                                         | JVM                                              |
| Android `src/androidTest`               | Real DataStore migration/reopen, complete records, metadata replacement, Keystore encryption, CBC compatibility, tampering/deleted keys, native results/errors, concurrency and security requirements | API 34, 35, 36                                   |
| iOS `ios/KeychainNativeTests`           | Real generic/internet Keychain classes, overwrite/reset, sync isolation, legacy native records, listing, access-control metadata and entitlement errors                                               | Simulator XCTest with the example host           |
| Detox `e2e/testCases/publicApi.spec.js` | Public JS/TurboModule contract and process restart on both platforms; Android biometrics, cancellation/recovery, saved PIN policy and RSA reads                                                       | Seven Android flows, two iOS flows               |
| iOS `ios/KeychainUITests`               | Cancel a real protected read, verify its error and preserve the credential                                                                                                                            | Physical iPhone; explicitly skipped on Simulator |

`test` and `androidTest` are Android's standard source sets. Android imports alone
do not require a device: the JVM policy tests use constants and types without
calling Android services. DataStore/Keystore integration belongs in `androidTest`.

## Run locally

Use the repository's Node/Yarn versions and `yarn install --immutable`. Run
`yarn typecheck`, `yarn lint`, and `yarn tsc -p KeychainExample/tsconfig.json --noEmit`
from the root for the library and test-screen types.

From `KeychainExample/android`, with an Android SDK and JDK configured:

```sh
./gradlew :react-native-keychain:testDebugUnitTest
ANDROID_SERIAL=emulator-5556 ./gradlew :react-native-keychain:connectedDebugAndroidTest
```

Use your selected emulator's serial. The native tests do not need enrollment.
For Detox, configure PIN `1111` and fingerprint `1` as in CI, then run from
`KeychainExample`:

```sh
yarn build:e2e:android
yarn test:android:build
yarn test:android:run
```

For an already running emulator, use
`yarn detox test --configuration android.att.release --device-name <serial>`.
CI builds the library's instrumentation APK once and runs it on every Android
version with `.github/scripts/run-android-native-tests.py`; it verifies the test
result text because `adb` can exit successfully when instrumentation tests fail.

For iOS, install the Gemfile dependencies and Detox's `applesimutils` prerequisite,
then run from `KeychainExample`:

```sh
yarn build:e2e:ios
cd ios && bundle exec pod install && cd ..
yarn test:ios:build
python3 ../.github/scripts/run-ios-native-tests.py
yarn test:ios:run
```

The native runner uses an available iPhone 17 Pro, preferring a booted one.
Set `KEYCHAIN_IOS_DEVICE_ID` to select an exact simulator and `KEYCHAIN_XCRESULT`
to a new output path when repeating a native run. Keep simulator code signing
enabled: the host needs its Keychain entitlements. XCTest classes live in local
test pods attached to React Native Test App's generated test targets.

For the physical-device cancellation test, build the E2E iOS bundle, configure the
host and UI runner's signing in Xcode, and run
`ReactTestAppUITests/RNKeychainAuthenticationTests` on an unlocked iPhone with
biometrics enrolled and Face ID permission granted. Avoid looking at the Face ID
camera or touching the Touch ID sensor while the test cancels the prompt.
This test must observe the system prompt and `E_AUTH_CANCELED`; it cannot pass
by simply sending a biometric event when no prompt exists.

## Keep the suite focused

Each test owns its fixtures. Android uses unique storage directories/key aliases;
iOS native tests use UUID identifiers. Detox explicitly deletes only its test
identifiers before and after each flow. A restart test saves and reads within the
same test. Uninstalling the app is not a Keychain cleanup strategy on iOS.

Put storage/query combinations in native tests. Add a Detox flow when the risk is
at the JS/native boundary or in authentication UI. Do not restore picker, keyboard,
scrolling or example-layout assertions to the library suite. Do not add blanket
retries: capture the failure and fix its cause. Android prompt input waits for the
focused system window; the 5.5-second wait is specifically for the Keystore's
five-second authentication validity window to expire.

Simulator checks do not establish biometric enforcement, cross-device iCloud
synchronization, Secure Enclave security or Android hardware-backed protection.
The iOS simulator used here returns protected Keychain data without authenticating,
so the UI authentication test is visibly skipped rather than reported as passing.
Real-device release checks should include denied/unenrolled biometrics, failed
matches and lockout, enrollment changes, backgrounding during prompts, passcode
fallback and hardware security. These are not all automated by this suite.

Native Android cipher coverage includes embedded null characters. The RN 0.82.1
JavaScript/native transport truncated an embedded null in a credential during
validation; the JS smoke fixture therefore uses Unicode and a newline. This
transport limitation remains unresolved and is not evidence of a storage failure.
