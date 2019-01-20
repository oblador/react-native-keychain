// @flow
import { NativeModules, Platform } from 'react-native';
const { RNKeychainManager } = NativeModules;

export const SECURITY_LEVEL = Object.freeze({
  ANY: NativeModules.SECURITY_LEVEL_ANY,
  SECURE_SOFTWARE: NativeModules.SECURITY_LEVEL_SECURE_SOFTWARE,
  SECURE_HARDWARE: NativeModules.SECURITY_LEVEL_SECURE_HARDWARE,
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
});

export type SecAccessible = $Values<typeof ACCESSIBLE>;

export type SecAccessControl = $Values<typeof ACCESS_CONTROL>;

export type LAPolicy = $Values<typeof AUTHENTICATION_TYPE>;

export type SecMinimumLevel = $Values<typeof SECURITY_LEVEL>;

export type Options = {
  accessControl?: SecAccessControl,
  accessGroup?: string,
  accessible?: SecAccessible,
  authenticationPrompt?: string,
  authenticationType?: LAPolicy,
  service?: string,
  securityLevel?: SecMinimumLevel,
};

/**
 * (Android only) Returns guaranteed security level supported by this library
 * on the current device.
 * @return {Promise} Resolves to `SECURITY_LEVEL` when supported, otherwise `null`.
 */
export function getSecurityLevel(): Promise<SecMinimumLevel> {
    if (!RNKeychainManager.getSecurityLevel){
        return Promise.resolve(null);
    }
    return RNKeychainManager.getSecurityLevel();
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

/**
 * Get what type of hardware biometry support the device has.
 * @return {Promise} Resolves to a `BIOMETRY_TYPE` when supported, otherwise `null`
 */
export function getSupportedBiometryType(): Promise<?($Values<typeof BIOMETRY_TYPE>)> {
  if (!RNKeychainManager.getSupportedBiometryType) {
    return Promise.resolve(null);
  }
  return RNKeychainManager.getSupportedBiometryType();
}

/**
 * Saves the `username` and `password` combination for `server`.
 * @param {string} server URL to server.
 * @param {string} username Associated username or e-mail to be saved.
 * @param {string} password Associated password to be saved.
 * @param {object} options Keychain options, iOS only
 * @return {Promise} Resolves to `true` when successful
 */
export function setInternetCredentials(
  server: string,
  username: string,
  password: string,
  options?: Options
): Promise<void> {
  return RNKeychainManager.setInternetCredentialsForServer(
    server,
    username,
    password,
    getMinimumSecurityLevel(options),
    options
  );
}

/**
 * Checks if we have a login combination for `server`.
 * @param {string} server URL to server.
 * @return {Promise} Resolves to `true` when successful
 */
export function hasInternetCredentials(server: string): Promise<boolean> {
  return RNKeychainManager.hasInternetCredentialsForServer(server);
}

export type UserCredentials = {|
  +username: string,
  +password: string,
|};

/**
 * Fetches login combination for `server`.
 * @param {string} server URL to server.
 * @param {object} options Keychain options, iOS only
 * @return {Promise} Resolves to `{ server, username, password }` when successful
 */
export function getInternetCredentials(
  server: string,
  options?: Options
): Promise<UserCredentials> {
  return RNKeychainManager.getInternetCredentialsForServer(server, options);
}

/**
 * Deletes all internet password keychain entries for `server`.
 * @param {string} server URL to server.
 * @param {object} options Keychain options, iOS only
 * @return {Promise} Resolves to `true` when successful
 */
export function resetInternetCredentials(
  server: string,
  options?: Options
): Promise<void> {
  return RNKeychainManager.resetInternetCredentialsForServer(server, options);
}

function getOptionsArgument(serviceOrOptions?: string | Options) {
  if (Platform.OS !== 'ios') {
    return typeof serviceOrOptions === 'object'
      ? serviceOrOptions.service
      : serviceOrOptions;
  }
  return typeof serviceOrOptions === 'string'
    ? { service: serviceOrOptions }
    : serviceOrOptions;
}

function getMinimumSecurityLevel(serviceOrOptions?: string | Options) {
  return typeof serviceOrOptions === 'object'
    ? serviceOrOptions.securityLevel
    : SECURITY_LEVEL.ANY;
}

/**
 * Saves the `username` and `password` combination for `service`.
 * @param {string} username Associated username or e-mail to be saved.
 * @param {string} password Associated password to be saved.
 * @param {string|object} serviceOrOptions Reverse domain name qualifier for the service, defaults to `bundleId` or an options object.
 * @return {Promise} Resolves to `true` when successful
 */
export function setGenericPassword(
  username: string,
  password: string,
  serviceOrOptions?: string | Options
): Promise<boolean> {
  return RNKeychainManager.setGenericPasswordForOptions(
    getOptionsArgument(serviceOrOptions),
    username,
    password,
    getMinimumSecurityLevel(serviceOrOptions)
  );
}

export type SharedWebCredentials = {|
  +server: string,
  +username: string,
  +password: string,
|};

/**
 * Fetches login combination for `service`.
 * @param {string|object} serviceOrOptions Reverse domain name qualifier for the service, defaults to `bundleId` or an options object.
 * @return {Promise} Resolves to `{ service, username, password }` when successful
 */
export function getGenericPassword(
  serviceOrOptions?: string | Options
): Promise<boolean | SharedWebCredentials> {
  return RNKeychainManager.getGenericPasswordForOptions(
    getOptionsArgument(serviceOrOptions)
  );
}

/**
 * Deletes all generic password keychain entries for `service`.
 * @param {string|object} serviceOrOptions Reverse domain name qualifier for the service, defaults to `bundleId` or an options object.
 * @return {Promise} Resolves to `true` when successful
 */
export function resetGenericPassword(
  serviceOrOptions?: string | Options
): Promise<boolean> {
  return RNKeychainManager.resetGenericPasswordForOptions(
    getOptionsArgument(serviceOrOptions)
  );
}

/**
 * Asks the user for a shared web credential.
 * @return {Promise} Resolves to `{ server, username, password }` if approved and
 * `false` if denied and throws an error if not supported on platform or there's no shared credentials
 */
export function requestSharedWebCredentials(): Promise<SharedWebCredentials> {
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
  password: string
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
