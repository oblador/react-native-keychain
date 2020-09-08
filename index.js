// @flow
import { NativeModules, Platform } from 'react-native';

const { RNKeychainManager } = NativeModules;

export const SECURITY_LEVEL = Object.freeze({
  ANY: RNKeychainManager.SECURITY_LEVEL_ANY,
  SECURE_SOFTWARE: RNKeychainManager.SECURITY_LEVEL_SECURE_SOFTWARE,
  SECURE_HARDWARE: RNKeychainManager.SECURITY_LEVEL_SECURE_HARDWARE,
});

export const ACCESSIBLE = Object.freeze({
  WHEN_UNLOCKED: 'AccessibleWhenUnlocked',
  AFTER_FIRST_UNLOCK: 'AccessibleAfterFirstUnlock',
  ALWAYS: 'AccessibleAlways',
  WHEN_PASSCODE_SET_THIS_DEVICE_ONLY: 'AccessibleWhenPasscodeSetThisDeviceOnly',
  WHEN_UNLOCKED_THIS_DEVICE_ONLY: 'AccessibleWhenUnlockedThisDeviceOnly',
  AFTER_FIRST_UNLOCK_THIS_DEVICE_ONLY:
    'AccessibleAfterFirstUnlockThisDeviceOnly',
  ALWAYS_THIS_DEVICE_ONLY: 'AccessibleAlwaysThisDeviceOnly',
});

export const ACCESS_CONTROL = Object.freeze({
  USER_PRESENCE: 'UserPresence',
  BIOMETRY_ANY: 'BiometryAny',
  BIOMETRY_CURRENT_SET: 'BiometryCurrentSet',
  DEVICE_PASSCODE: 'DevicePasscode',
  APPLICATION_PASSWORD: 'ApplicationPassword',
  BIOMETRY_ANY_OR_DEVICE_PASSCODE: 'BiometryAnyOrDevicePasscode',
  BIOMETRY_CURRENT_SET_OR_DEVICE_PASSCODE: 'BiometryCurrentSetOrDevicePasscode',
});

export const AUTHENTICATION_TYPE = Object.freeze({
  DEVICE_PASSCODE_OR_BIOMETRICS: 'AuthenticationWithBiometricsDevicePasscode',
  BIOMETRICS: 'AuthenticationWithBiometrics',
});

export const BIOMETRY_TYPE = Object.freeze({
  TOUCH_ID: 'TouchID',
  FACE_ID: 'FaceID',
  FINGERPRINT: 'Fingerprint',
  FACE: 'Face',
  IRIS: 'Iris',
});

export const STORAGE_TYPE = Object.freeze({
  FB: 'FacebookConceal',
  AES: 'KeystoreAESCBC',
  RSA: 'KeystoreRSAECB',
  KC: 'keychain', // <~ iOS only
});

export const SECURITY_RULES = Object.freeze({
  NONE: 'none',
  AUTOMATIC_UPGRADE: 'automaticUpgradeToMoreSecuredStorage',
});

export type SecAccessible = $Values<typeof ACCESSIBLE>;

export type SecAccessControl = $Values<typeof ACCESS_CONTROL>;

export type LAPolicy = $Values<typeof AUTHENTICATION_TYPE>;

export type SecMinimumLevel = $Values<typeof SECURITY_LEVEL>;

export type SecStorageType = $Values<typeof STORAGE_TYPE>;

export type SecSecurityRules = $Values<typeof SECURITY_RULES>;

export type SecBiometryType = $Values<typeof BIOMETRY_TYPE>;

export type AuthenticationPrompt = {|
  title?: string,
  subtitle?: string,
  description?: string,
  cancel?: string,
|};

type BaseOptions = {|
  accessControl?: SecAccessControl,
  accessGroup?: string,
  accessible?: SecAccessible,
  authenticationType?: LAPolicy,
  service?: string,
  securityLevel?: SecMinimumLevel,
  storage?: SecStorageType,
  rules?: SecSecurityRules,
|};

type NormalizedOptions = {
  authenticationPrompt?: AuthenticationPrompt,
  ...BaseOptions,
};

export type Options = {
  authenticationPrompt?: string | AuthenticationPrompt,
  ...BaseOptions,
};

export type Result = {|
  +service: string,
  +storage: string,
|};

export type UserCredentials = {|
  +username: string,
  +password: string,
  ...Result,
|};

export type SharedWebCredentials = {|
  +server: string,
  ...UserCredentials,
|};

const AUTH_PROMPT_DEFAULTS = {
  title: 'Authenticate to retrieve secret',
  cancel: 'Cancel',
};

function normalizeServiceOption(serviceOrOptions?: string | Options) {
  if (typeof serviceOrOptions === 'string') {
    console.warn(
      `You passed a service string as an argument to one of the react-native-keychain functions.
      This way of passing service is deprecated and will be removed in a future major.
      Please update your code to use { service: ${JSON.stringify(
        serviceOrOptions
      )} }`
    );
    return { service: serviceOrOptions };
  }
  return serviceOrOptions || {};
}

function normalizeOptions(
  serviceOrOptions?: string | Options
): NormalizedOptions {
  let options = { ...normalizeServiceOption(serviceOrOptions) };
  const { authenticationPrompt } = options;

  if (typeof authenticationPrompt === 'string') {
    console.warn(
      `You passed a authenticationPrompt string as an argument to one of the react-native-keychain functions.
      This way of passing authenticationPrompt is deprecated and will be removed in a future major.
      Please update your code to use { authenticationPrompt: { title: ${JSON.stringify(
        authenticationPrompt
      )} }`
    );
    options.authenticationPrompt = {
      ...AUTH_PROMPT_DEFAULTS,
      title: authenticationPrompt,
    };
  } else {
    options.authenticationPrompt = {
      ...AUTH_PROMPT_DEFAULTS,
      ...authenticationPrompt,
    };
  }

  // $FlowFixMe >=0.107.x – remove in next major, when authenticationPrompt as string is removed
  return options;
}

//* EXPORTS */

/**
 * Saves the `username` and `password` combination for `service`.
 * @param {string} username Associated username or e-mail to be saved.
 * @param {string} password Associated password to be saved.
 * @param {object} options A keychain options object.
 * @return {Promise} Resolves to `{ service, storage }` when successful
 */
export function setGenericPassword(
  username: string,
  password: string,
  serviceOrOptions?: string | Options
): Promise<false | Result> {
  const options = normalizeOptions(serviceOrOptions);
  return RNKeychainManager.setGenericPasswordForOptions(
    options,
    username,
    password
  );
}

/**
 * Fetches login combination for `service`.
 * @param {object} options A keychain options object.
 * @return {Promise} Resolves to `{ service, username, password, storage }` when successful
 */
export function getGenericPassword(
  serviceOrOptions?: string | Options
): Promise<false | UserCredentials> {
  const options = normalizeOptions(serviceOrOptions);
  return RNKeychainManager.getGenericPasswordForOptions(options);
}

/**
 * Deletes all generic password keychain entries for `service`.
 * @param {object} options An Keychain options object.
 * @return {Promise} Resolves to `true` when successful
 */
export function resetGenericPassword(
  serviceOrOptions?: string | Options
): Promise<boolean> {
  const options = normalizeOptions(serviceOrOptions);
  return RNKeychainManager.resetGenericPasswordForOptions(options);
}

/**
 * Checks if we have a login combination for `server`.
 * @param {string} server URL to server.
 * @return {Promise} Resolves to `{service, storage}` when successful
 */
export function hasInternetCredentials(
  server: string
): Promise<false | Result> {
  return RNKeychainManager.hasInternetCredentialsForServer(server);
}

/**
 * Saves the `username` and `password` combination for `server`.
 * @param {string} server URL to server.
 * @param {string} username Associated username or e-mail to be saved.
 * @param {string} password Associated password to be saved.
 * @param {object} options A keychain options object.
 * @return {Promise} Resolves to `{service, storage}` when successful
 */
export function setInternetCredentials(
  server: string,
  username: string,
  password: string,
  options?: Options
): Promise<false | Result> {
  return RNKeychainManager.setInternetCredentialsForServer(
    server,
    username,
    password,
    options
  );
}

/**
 * Fetches login combination for `server`.
 * @param {string} server URL to server.
 * @param {object} options A keychain options object.
 * @return {Promise} Resolves to `{ server, username, password }` when successful
 */
export function getInternetCredentials(
  server: string,
  options?: Options
): Promise<false | UserCredentials> {
  return RNKeychainManager.getInternetCredentialsForServer(
    server,
    normalizeOptions(options)
  );
}

/**
 * Deletes all internet password keychain entries for `server`.
 * @param {string} server URL to server.
 * @param {object} options Keychain options, iOS only
 * @return {Promise} Resolves to `true` when successful
 */
export function resetInternetCredentials(server: string): Promise<void> {
  return RNKeychainManager.resetInternetCredentialsForServer(server);
}

/**
 * Get what type of hardware biometry support the device has.
 * @param {object} options An Keychain options object.
 * @return {Promise} Resolves to a `BIOMETRY_TYPE` when supported, otherwise `null`
 */
export function getSupportedBiometryType(): Promise<null | SecBiometryType> {
  if (!RNKeychainManager.getSupportedBiometryType) {
    return Promise.resolve(null);
  }

  if (Platform.OS === 'ios') {
    return RNKeychainManager.getSupportedBiometryType();
  }

  return RNKeychainManager.getSupportedBiometryType();
}

//* IOS ONLY */

/**
 * Asks the user for a shared web credential.
 * @return {Promise} Resolves to `{ server, username, password }` if approved and
 * `false` if denied and throws an error if not supported on platform or there's no shared credentials
 */
export function requestSharedWebCredentials(): Promise<
  false | SharedWebCredentials
> {
  if (Platform.OS !== 'ios') {
    return Promise.reject(
      new Error(
        `requestSharedWebCredentials() is not supported on ${Platform.OS} yet`
      )
    );
  }
  return RNKeychainManager.requestSharedWebCredentials();
}

/**
 * Sets a shared web credential.
 * @param {string} server URL to server.
 * @param {string} username Associated username or e-mail to be saved.
 * @param {string} password Associated password to be saved.
 * @return {Promise} Resolves to `true` when successful
 */
export function setSharedWebCredentials(
  server: string,
  username: string,
  password?: string
): Promise<void> {
  if (Platform.OS !== 'ios') {
    return Promise.reject(
      new Error(
        `setSharedWebCredentials() is not supported on ${Platform.OS} yet`
      )
    );
  }
  return RNKeychainManager.setSharedWebCredentialsForServer(
    server,
    username,
    password
  );
}

/**
 * Inquire if the type of local authentication policy (LAPolicy) is supported
 * on this device with the device settings the user chose.
 * @param {object} options LAPolicy option, iOS only
 * @return {Promise} Resolves to `true` when supported, otherwise `false`
 */
export function canImplyAuthentication(options?: Options): Promise<boolean> {
  if (!RNKeychainManager.canCheckAuthentication) {
    return Promise.resolve(false);
  }
  return RNKeychainManager.canCheckAuthentication(options);
}

//* ANDROID ONLY */

/**
 * (Android only) Returns guaranteed security level supported by this library
 * on the current device.
 * @param {object} options A keychain options object.
 * @return {Promise} Resolves to `SECURITY_LEVEL` when supported, otherwise `null`.
 */
export function getSecurityLevel(
  options?: Options
): Promise<null | SecMinimumLevel> {
  if (!RNKeychainManager.getSecurityLevel) {
    return Promise.resolve(null);
  }
  return RNKeychainManager.getSecurityLevel(options);
}

/** Refs: https://www.saltycrane.com/cheat-sheets/flow-type/latest/ */

export default {
  SECURITY_LEVEL,
  ACCESSIBLE,
  ACCESS_CONTROL,
  AUTHENTICATION_TYPE,
  BIOMETRY_TYPE,
  STORAGE_TYPE,
  SECURITY_RULES,
  getSecurityLevel,
  canImplyAuthentication,
  getSupportedBiometryType,
  setInternetCredentials,
  getInternetCredentials,
  resetInternetCredentials,
  setGenericPassword,
  getGenericPassword,
  resetGenericPassword,
  requestSharedWebCredentials,
  setSharedWebCredentials,
};
