import { NativeModules, Platform } from 'react-native';
const { RNKeychainManager } = NativeModules;

/**
 * Saves the `username` and `password` combination for `server`.
 * @param {string} server URL to server.
 * @param {string} username Associated username or e-mail to be saved.
 * @param {string} password Associated password to be saved.
 * @return {Promise} Resolves to `true` when successful
 */
export function setInternetCredentials(
  server: string,
  username: string,
  password: string
): Promise {
  return RNKeychainManager.setInternetCredentialsForServer(server, username, password);
}

/**
 * Fetches login combination for `server`.
 * @param {string} server URL to server.
 * @return {Promise} Resolves to `{ server, username, password }` when successful
 */
export function getInternetCredentials(
  server: string
): Promise {
  return RNKeychainManager.getInternetCredentialsForServer(server);
}

/**
 * Deletes all internet password keychain entries for `server`.
 * @param {string} server URL to server.
 * @return {Promise} Resolves to `true` when successful
 */
export function resetInternetCredentials(
  server: string
): Promise {
  return RNKeychainManager.resetInternetCredentialsForServer(server);
}

/**
 * Saves the `username` and `password` combination for `service`.
 * @param {string} username Associated username or e-mail to be saved.
 * @param {string} password Associated password to be saved.
 * @param {string} service Reverse domain name qualifier for the service, defaults to `bundleId`.
 * @return {Promise} Resolves to `true` when successful
 */
export function setGenericPassword(
  username: string,
  password: string,
  service?: string
): Promise {
  return RNKeychainManager.setGenericPasswordForService(service, username, password);
}

/**
 * Fetches login combination for `service`.
 * @param {string} service Reverse domain name qualifier for the service, defaults to `bundleId`.
 * @return {Promise} Resolves to `{ service, username, password }` when successful
 */
export function getGenericPassword(
  service?: string
): Promise {
  return RNKeychainManager.getGenericPasswordForService(service);
}

/**
 * Deletes all generic password keychain entries for `service`.
 * @param {string} service Reverse domain name qualifier for the service, defaults to `bundleId`.
 * @return {Promise} Resolves to `true` when successful
 */
export function resetGenericPassword(
  service?: string
): Promise {
  return RNKeychainManager.resetGenericPasswordForService(service);
}

/**
 * Asks the user for a shared web credential.
 * @return {Promise} Resolves to `{ server, username, password }` if approved and
 * `false` if denied and throws an error if not supported on platform or there's no shared credentials
 */
export function requestSharedWebCredentials() : Promise {
  return Promise.reject(new Error(`requestSharedWebCredentials() is not supported on ${Platform.OS} yet`));
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
  return Promise.reject(new Error(`setSharedWebCredentials() is not supported on ${Platform.OS} yet`));
}