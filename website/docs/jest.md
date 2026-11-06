# Testing with Jest

Mock the JavaScript API in unit tests. Jest does not run Apple Keychain, Android Keystore, or native authentication prompts.

## Create a manual mock

Create `__mocks__/react-native-keychain.js` in your app. Mock the functions and enum values your application actually uses:

```js
const keychain = {
  setGenericPassword: jest.fn().mockResolvedValue({
    service: 'com.example.session',
    storage: 'keychain',
  }),
  getGenericPassword: jest.fn().mockResolvedValue(false),
  hasGenericPassword: jest.fn().mockResolvedValue(false),
  resetGenericPassword: jest.fn().mockResolvedValue(true),
  getSupportedBiometryType: jest.fn().mockResolvedValue(null),
  isPasscodeAuthAvailable: jest.fn().mockResolvedValue(false),
};

module.exports = keychain;
```

Use the [API reference](./api/index.md) to match return shapes. For example, `getGenericPassword` returns credentials or `false`, while `resetInternetCredentials` resolves without a value.

## Test different outcomes

Activate the mock in your test or Jest setup file:

```ts
import * as Keychain from 'react-native-keychain';

jest.mock('react-native-keychain');

beforeEach(() => {
  jest.clearAllMocks();
  jest.mocked(Keychain.getGenericPassword).mockResolvedValue(false);
});
```

Set the result for the behavior you want to exercise:

```ts
// A saved sign-in.
jest.mocked(Keychain.getGenericPassword).mockResolvedValueOnce({
  username: 'session',
  password: 'test-token',
  service: 'com.example.session',
  storage: 'keychain' as Keychain.STORAGE_TYPE,
});

// No saved sign-in.
jest.mocked(Keychain.getGenericPassword).mockResolvedValueOnce(false);

// A failed read: this must not be treated as a missing credential.
jest
  .mocked(Keychain.getGenericPassword)
  .mockRejectedValueOnce(new Error('Authentication failed'));
```

Exercise these outcomes in separate tests of your app's sign-in and recovery behavior. Avoid `jest.requireActual('react-native-keychain')` in a plain Jest environment: importing the real module requires the native TurboModule.

## What to test on a device

Mocks verify your app's decisions, not native security. Use an app build to check persistence, authentication, cancellation, changed biometric enrollment, and device-specific behavior. The repository's [example app](./example-app.md) and Detox suites cover native flows.
