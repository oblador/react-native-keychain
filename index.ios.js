import { NativeModules, Platform } from 'react-native';
const { RNKeychainManager } = NativeModules;

/**
 * Saves the `username` and `password` combination for `server`.
 * @param {string} server URL to server.
 * @param {string} accessGroup Group name for keychain sharing.   
 * @param {string} username Associated username or e-mail to be saved.
 * @param {string} password Associated password to be saved.
 * @return {Promise} Resolves to `true` when successful
 */
export function setInternetCredentials(
  server: string,
  username: string,
  password: string,
  accessGroup?: string  
): Promise {
  return RNKeychainManager.setInternetCredentialsForServer(server, accessGroup, username, password);
}

/**
 * Fetches login combination for `server`.
 * @param {string} server URL to server.
 * @param {string} accessGroup Group name for keychain sharing.
 * @return {Promise} Resolves to `{ server, username, password }` when successful
 */
export function getInternetCredentials(
  server: string,
  accessGroup?: string
): Promise {
  return RNKeychainManager.getInternetCredentialsForServer(server, accessGroup);
}

/**
 * Deletes all internet password keychain entries for `server`.
 * @param {string} server URL to server.
 * @param {string} accessGroup Group name for keychain sharing.
 * @return {Promise} Resolves to `true` when successful
 */
export function resetInternetCredentials(
  server: string,
  accessGroup?: string
): Promise {
  return RNKeychainManager.resetInternetCredentialsForServer(server, accessGroup);
}

/**
 * Saves the `username` and `password` combination for `service`.
 * @param {string} username Associated username or e-mail to be saved.
 * @param {string} password Associated password to be saved.
 * @param {string} service Reverse domain name qualifier for the service, defaults to `bundleId`.
 * @param {string} accessGroup Group name for keychain sharing.
 * @return {Promise} Resolves to `true` when successful
 */
export function setGenericPassword(
  username: string,
  password: string,
  service?: string,
  accessGroup?: string
): Promise {
  return RNKeychainManager.setGenericPasswordForService(service, accessGroup, username, password);
}

/**
 * Fetches login combination for `service`.
 * @param {string} service Reverse domain name qualifier for the service, defaults to `bundleId`.
 * @param {string} accessGroup Group name for keychain sharing.
 * @return {Promise} Resolves to `{ service, username, password }` when successful
 */
export function getGenericPassword(
  service?: string,
  accessGroup?: string
): Promise {
  return RNKeychainManager.getGenericPasswordForService(service, accessGroup);
}

/**
 * Deletes all generic password keychain entries for `service`.
 * @param {string} service Reverse domain name qualifier for the service, defaults to `bundleId`.
 * @param {string} accessGroup Group name for keychain sharing.
 * @return {Promise} Resolves to `true` when successful
 */
export function resetGenericPassword(
  service?: string,
  accessGroup?: string
): Promise {
  return RNKeychainManager.resetGenericPasswordForService(service, accessGroup);
}

/**
 * Asks the user for a shared web credential.
 * @return {Promise} Resolves to `{ server, username, password }` if approved and
 * `false` if denied and throws an error if not supported on platform or there's no shared credentials
 */
export function requestSharedWebCredentials() : Promise {
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
  return RNKeychainManager.setSharedWebCredentialsForServer(server, username, password);
}
