import { NativeModules, Platform } from 'react-native';
const { RNKeychainManager } = NativeModules;

type SecAccessible =
  | 'AccessibleWhenUnlocked'
  | 'AccessibleAfterFirstUnlock'
  | 'AccessibleAlways'
  | 'AccessibleWhenPasscodeSetThisDeviceOnly'
  | 'AccessibleWhenUnlockedThisDeviceOnly'
  | 'AccessibleAfterFirstUnlockThisDeviceOnly'
  | 'AccessibleAlwaysThisDeviceOnly'

type Options = {
  accessible?: SecAccessible;
  accessGroup?: string;
  service?: string;
};

type SecAccessControl =
  | 'UserPresence'
  | 'TouchIDAny'
  | 'TouchIDCurrentSet'
  | 'DevicePasscode'
  | 'TouchIDAnyOrDevicePasscode'
  | 'TouchIDCurrentSetOrDevicePasscode'

  type LAPolicy =
    | 'Authentication'
    | 'AuthenticationWithBiometrics'

type SecureOptions = {
  customPrompt?: string;
  authenticationType?: LAPolicy;
  accessControl?: SecAccessControl;
};

/**
 * Inquire if the type of local authentication policy (LAPolicy) is supported
 * on this device with the device settings the user chose.
 * @param {object} options LAPolicy option, iOS only
 * @return {Promise} Resolves to `true` when successful
 */
export function canImplyAuthentication(
  options?: SecureOptions
): Promise {
  return RNKeychainManager.canCheckAuthentication(options);
}

/**
 * Saves the `username` and `password` combination for `service` securely - needs authentication to retrieve it.
 * @param {string} service Associated service.
 * @param {string} username Associated username or e-mail to be saved.
 * @param {string} password Associated password to be saved.
 * @param {object} options Keychain options, iOS only
 * @return {Promise} Resolves to `true` when successful
 */
export function setSecurePassword(
  service: string,
  username: string,
  password: string,
  options?: SecureOptions
): Promise {
  return RNKeychainManager.setSecurePasswordForService(service, username, password, options);
}

/**
 * Fetches login combination for `service` - demands for authentication if necessary.
 * @param {string|object} serviceOrOptions Reverse domain name qualifier for the service, defaults to `bundleId` or an options object.
 * @return {Promise} Resolves to `{ service, username, password }` when successful
 */
export function getSecurePassword(
  service: string,
  options?: SecureOptions
): Promise {
  return RNKeychainManager.getSecurePasswordForService(service, options);
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
): Promise {
  return RNKeychainManager.setInternetCredentialsForServer(server, username, password, options);
}

/**
 * Fetches login combination for `server`.
 * @param {string} server URL to server.
 * @param {object} options Keychain options, iOS only
 * @return {Promise} Resolves to `{ server, username, password }` when successful
 */
export function getInternetCredentials(
  server: string,
  options?: Options
): Promise {
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
): Promise {
  return RNKeychainManager.resetInternetCredentialsForServer(server, options);
}

function getOptionsArgument(serviceOrOptions?: string | KeychainOptions) {
  if (Platform.OS !== 'ios') {
    return typeof serviceOrOptions === 'object' ? serviceOrOptions.service : serviceOrOptions;
  }
  return typeof serviceOrOptions === 'string' ? { service: serviceOrOptions } : serviceOrOptions;
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
  serviceOrOptions?: string | KeychainOptions
): Promise {
  return RNKeychainManager.setGenericPasswordForOptions(getOptionsArgument(serviceOrOptions), username, password);
}

/**
 * Fetches login combination for `service`.
 * @param {string|object} serviceOrOptions Reverse domain name qualifier for the service, defaults to `bundleId` or an options object.
 * @return {Promise} Resolves to `{ service, username, password }` when successful
 */
export function getGenericPassword(
  serviceOrOptions?: string | KeychainOptions
): Promise {
  return RNKeychainManager.getGenericPasswordForOptions(getOptionsArgument(serviceOrOptions));
}

/**
 * Deletes all generic password keychain entries for `service`.
 * @param {string|object} serviceOrOptions Reverse domain name qualifier for the service, defaults to `bundleId` or an options object.
 * @return {Promise} Resolves to `true` when successful
 */
export function resetGenericPassword(
  serviceOrOptions?: string | KeychainOptions
): Promise {
  return RNKeychainManager.resetGenericPasswordForOptions(getOptionsArgument(serviceOrOptions));
}

/**
 * Asks the user for a shared web credential.
 * @return {Promise} Resolves to `{ server, username, password }` if approved and
 * `false` if denied and throws an error if not supported on platform or there's no shared credentials
 */
export function requestSharedWebCredentials() : Promise {
  if (Platform.OS !== 'ios') {
    return Promise.reject(new Error(`requestSharedWebCredentials() is not supported on ${Platform.OS} yet`));
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
) : Promise {
  if (Platform.OS !== 'ios') {
    return Promise.reject(new Error(`setSharedWebCredentials() is not supported on ${Platform.OS} yet`));
  }
  return RNKeychainManager.setSharedWebCredentialsForServer(server, username, password);
}
