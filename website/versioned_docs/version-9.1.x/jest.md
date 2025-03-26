---
id: jest
title: Unit Testing with Jest
---

The keychain manager relies on interfacing with the native application itself. As such, it does not successfully compile and run in the context of a Jest test, where there is no underlying app to communicate with. To be able to call the JavaScript (or TypeScript) functions exposed by this module in a unit test, you must mock them. This guide provides two approaches for mocking the module.

## Mocking the Keychain Module in Jest

First, let's create a mock object for the module. This mock should reflect the structure of the actual module that you are testing. Based on the TypeScript file, your mock should include the various enums and functions that are used by the keychain module.

```ts
// keychainMock.ts or keychainMock.js

const keychainMock = {
  SECURITY_LEVEL: {
    SECURE_SOFTWARE: 'MOCK_SECURITY_LEVEL_SECURE_SOFTWARE',
    SECURE_HARDWARE: 'MOCK_SECURITY_LEVEL_SECURE_HARDWARE',
    ANY: 'MOCK_SECURITY_LEVEL_ANY',
  },
  ACCESSIBLE: {
    WHEN_UNLOCKED: 'MOCK_AccessibleWhenUnlocked',
    AFTER_FIRST_UNLOCK: 'MOCK_AccessibleAfterFirstUnlock',
    ALWAYS: 'MOCK_AccessibleAlways',
    WHEN_PASSCODE_SET_THIS_DEVICE_ONLY: 'MOCK_AccessibleWhenPasscodeSetThisDeviceOnly',
    WHEN_UNLOCKED_THIS_DEVICE_ONLY: 'MOCK_AccessibleWhenUnlockedThisDeviceOnly',
    AFTER_FIRST_UNLOCK_THIS_DEVICE_ONLY: 'MOCK_AccessibleAfterFirstUnlockThisDeviceOnly',
  },
  ACCESS_CONTROL: {
    USER_PRESENCE: 'MOCK_UserPresence',
    BIOMETRY_ANY: 'MOCK_BiometryAny',
    BIOMETRY_CURRENT_SET: 'MOCK_BiometryCurrentSet',
    DEVICE_PASSCODE: 'MOCK_DevicePasscode',
    APPLICATION_PASSWORD: 'MOCK_ApplicationPassword',
    BIOMETRY_ANY_OR_DEVICE_PASSCODE: 'MOCK_BiometryAnyOrDevicePasscode',
    BIOMETRY_CURRENT_SET_OR_DEVICE_PASSCODE: 'MOCK_BiometryCurrentSetOrDevicePasscode',
  },
  AUTHENTICATION_TYPE: {
    DEVICE_PASSCODE_OR_BIOMETRICS: 'MOCK_AuthenticationWithBiometricsDevicePasscode',
    BIOMETRICS: 'MOCK_AuthenticationWithBiometrics',
  },
  STORAGE_TYPE: {
    FB: 'MOCK_FacebookConceal',
    AES: 'MOCK_KeystoreAESCBC',
    RSA: 'MOCK_KeystoreRSAECB',
    KC: 'MOCK_keychain',
  },
  setGenericPassword: jest.fn().mockResolvedValue({
    service: 'mockService',
    storage: 'mockStorage',
  }),
  getGenericPassword: jest.fn().mockResolvedValue({
    username: 'mockUser',
    password: 'mockPassword',
    service: 'mockService',
    storage: 'mockStorage',
  }),
  resetGenericPassword: jest.fn().mockResolvedValue(true),
  hasGenericPassword: jest.fn().mockResolvedValue(true),
  getAllGenericPasswordServices: jest
    .fn()
    .mockResolvedValue(['mockService1', 'mockService2']),
  setInternetCredentials: jest.fn().mockResolvedValue({
    service: 'mockService',
    storage: 'mockStorage',
  }),
  getInternetCredentials: jest.fn().mockResolvedValue({
    username: 'mockUser',
    password: 'mockPassword',
    service: 'mockService',
    storage: 'mockStorage',
  }),
  resetInternetCredentials: jest.fn().mockResolvedValue(),
  getSupportedBiometryType: jest.fn().mockResolvedValue('MOCK_TouchID'),
  canImplyAuthentication: jest.fn().mockResolvedValue(true),
  getSecurityLevel: jest.fn().mockResolvedValue('MOCK_SECURE_SOFTWARE'),
};

export default keychainMock;
```

## Using a Jest `__mocks__` Directory

1. Read the [Jest documentation](https://jestjs.io/docs/en/manual-mocks#mocking-node-modules) for initial setup.

2. Create a `__mocks__` directory in your project root.

3. Inside the `__mocks__` directory, create a `react-native-keychain` folder.

4. In the `react-native-keychain` folder, create an `index.js` (or `index.ts` if using TypeScript) file with the following content:

```ts
// index.ts or index.js inside __mocks__/react-native-keychain

const keychainMock = {
  SECURITY_LEVEL: {
    SECURE_SOFTWARE: 'MOCK_SECURITY_LEVEL_SECURE_SOFTWARE',
    SECURE_HARDWARE: 'MOCK_SECURITY_LEVEL_SECURE_HARDWARE',
    ANY: 'MOCK_SECURITY_LEVEL_ANY',
  },
  // ... rest of the keychainMock object ...
  // (Copy the entire keychainMock object from above)
};

module.exports = keychainMock;
```

This approach allows you to mock the entire `react-native-keychain` module for all your tests.

## Using a Jest Setup File

1. In your Jest configuration file (`jest.config.js`), add a reference to a [setup file](https://jestjs.io/docs/en/configuration#setupfiles-array):

```js
module.exports = {
  // ... other configurations ...
  setupFiles: ['<rootDir>/jest.setup.js'],
};
```

2. Inside your `jest.setup.js` file, set up mocking for this package:

```ts
// jest.setup.js

import keychainMock from './path/to/keychainMock';

jest.mock('react-native-keychain', () => keychainMock);
```

Ensure that the path to `keychainMock` is correct relative to your `jest.setup.js` file.

Now your tests should run successfully. Writing and reading to the keychain will be effectively a no-op because the actual native code is not being called. Instead, the mock functions will return the mocked values you specified.

## Example Test

Here is an example of how you could write a unit test for a function that uses the `react-native-keychain` module:

```ts
import Keychain from 'react-native-keychain';

describe('Keychain Manager', () => {
  it('should save and retrieve credentials', async () => {
    // Mock saving credentials
    await Keychain.setGenericPassword('testUser', 'testPassword');
    expect(Keychain.setGenericPassword).toHaveBeenCalledWith(
      'testUser',
      'testPassword'
    );

    // Mock retrieving credentials
    const credentials = await Keychain.getGenericPassword();
    expect(credentials).toEqual({
      username: 'mockUser',
      password: 'mockPassword',
      service: 'mockService',
      storage: 'mockStorage',
    });
  });

  it('should check if a password exists', async () => {
    const exists = await Keychain.hasGenericPassword();
    expect(exists).toBe(true);
  });

  it('should reset credentials', async () => {
    const reset = await Keychain.resetGenericPassword();
    expect(reset).toBe(true);
  });

  it('should get supported biometry type', async () => {
    const biometryType = await Keychain.getSupportedBiometryType();
    expect(biometryType).toBe('MOCK_TouchID');
  });
});
```