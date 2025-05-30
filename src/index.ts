import { NativeModules, Platform } from 'react-native';
import {
  ACCESSIBLE,
  ACCESS_CONTROL,
  AUTHENTICATION_TYPE,
  SECURITY_LEVEL,
  STORAGE_TYPE,
  BIOMETRY_TYPE,
} from './enums';
import type {
  Result,
  UserCredentials,
  SharedWebCredentials,
  GetOptions,
  GetAllOptions,
  BaseOptions,
  SetOptions,
  AuthenticationTypeOption,
  AccessControlOption,
} from './types';
import { normalizeAuthPrompt } from './normalizeOptions';

const { RNKeychainManager } = NativeModules;

/**
 * Saves the `username` and `password` combination for the given service.
 *
 * @param {string} username - The username or e-mail to be saved.
 * @param {string} password - The password to be saved.
 * @param {SetOptions} [options] - A keychain options object.
 *
 * @returns {Promise<false | Result>} Resolves to an object containing `service` and `storage` when successful, or `false` on failure.
 *
 * @example
 * ```typescript
 * await Keychain.setGenericPassword('username', 'password');
 * ```
 */
export function setGenericPassword(
  username: string,
  password: string,
  options?: SetOptions
): Promise<false | Result> {
  return RNKeychainManager.setGenericPasswordForOptions(
    normalizeAuthPrompt(options),
    username,
    password
  );
}

/**
 * Fetches the `username` and `password` combination for the given service.
 *
 * @param {GetOptions} [options] - A keychain options object.
 *
 * @returns {Promise<false | UserCredentials>} Resolves to an object containing `service`, `username`, `password`, and `storage` when successful, or `false` on failure.
 *
 * @example
 * ```typescript
 * const credentials = await Keychain.getGenericPassword();
 * if (credentials) {
 *   console.log('Credentials successfully loaded for user ' + credentials.username);
 * } else {
 *   console.log('No credentials stored');
 * }
 * ```
 */
export function getGenericPassword(
  options?: GetOptions
): Promise<false | UserCredentials> {
  return RNKeychainManager.getGenericPasswordForOptions(
    normalizeAuthPrompt(options)
  );
}

/**
 * Checks if generic password exists for the given service.
 *
 * @param {BaseOptions} [options] - A keychain options object.
 *
 * @returns {Promise<boolean>} Resolves to `true` if a password exists, otherwise `false`.
 *
 * @example
 * ```typescript
 * const hasPassword = await Keychain.hasGenericPassword();
 * console.log('Password exists:', hasPassword);
 * ```
 */
export function hasGenericPassword(options?: BaseOptions): Promise<boolean> {
  return RNKeychainManager.hasGenericPasswordForOptions(options);
}

/**
 * Deletes all generic password keychain entries for the given service.
 *
 * @param {BaseOptions} [options] - A keychain options object.
 *
 * @returns {Promise<boolean>} Resolves to `true` when successful, otherwise `false`.
 *
 * @example
 * ```typescript
 * const success = await Keychain.resetGenericPassword();
 * console.log('Password reset successful:', success);
 * ```
 */
export function resetGenericPassword(options?: BaseOptions): Promise<boolean> {
  return RNKeychainManager.resetGenericPasswordForOptions(options);
}

/**
 * Gets all service keys used in generic password keychain entries.
 *
 * @returns {Promise<string[]>} Resolves to an array of strings representing service keys.
 *
 * @example
 * ```typescript
 * const services = await Keychain.getAllGenericPasswordServices();
 * console.log('Services:', services);
 * ```
 */
export function getAllGenericPasswordServices(
  options?: GetAllOptions
): Promise<string[]> {
  return RNKeychainManager.getAllGenericPasswordServices(options);
}

/**
 * Checks if internet credentials exist for the given server.
 *
 * @param {BaseOptions} options - A keychain options objectnormalizeAuthPrompt(options).
 *
 * @returns {Promise<boolean>} Resolves to `true` if internet credentials exist, otherwise `false`.
 *
 * @example
 * ```typescript
 * const hasCredentials = await Keychain.hasInternetCredentials('https://example.com');
 * console.log('Internet credentials exist:', hasCredentials);
 * ```
 */
export function hasInternetCredentials(
  options: string | BaseOptions
): Promise<boolean> {
  return RNKeychainManager.hasInternetCredentialsForOptions(options);
}

/**
 * Saves the internet credentials for the given server.
 *
 * @param {string} server - The server URL.
 * @param {string} username - The username or e-mail to be saved.
 * @param {string} password - The password to be saved.
 * @param {SetOptions} [options] - A keychain options object.
 *
 * @returns {Promise<false | Result>} Resolves to an object containing `service` and `storage` when successful, or `false` on failure.
 *
 * @example
 * ```typescript
 * await Keychain.setInternetCredentials('https://example.com', 'username', 'password');
 * ```
 */
export function setInternetCredentials(
  server: string,
  username: string,
  password: string,
  options?: SetOptions
): Promise<false | Result> {
  return RNKeychainManager.setInternetCredentialsForServer(
    server,
    username,
    password,
    normalizeAuthPrompt(options)
  );
}

/**
 * Fetches the internet credentials for the given server.
 *
 * @param {string} server - The server URL.
 * @param {GetOptions} [options] - A keychain options object.
 *
 * @returns {Promise<false | UserCredentials>} Resolves to an object containing `server`, `username`, `password`, and `storage` when successful, or `false` on failure.
 *
 * @example
 * ```typescript
 * const credentials = await Keychain.getInternetCredentials('https://example.com');
 * if (credentials) {
 *   console.log('Credentials loaded for user ' + credentials.username);
 * } else {
 *   console.log('No credentials stored for server');
 * }
 * ```
 */
export function getInternetCredentials(
  server: string,
  options?: GetOptions
): Promise<false | UserCredentials> {
  return RNKeychainManager.getInternetCredentialsForServer(
    server,
    normalizeAuthPrompt(options)
  );
}

/**
 * Deletes all internet password keychain entries for the given server.
 *
 * @param {BaseOptions} [options] - A keychain options object.
 *
 * @returns {Promise<void>} Resolves when the operation is completed.
 *
 * @example
 * ```typescript
 * await Keychain.resetInternetCredentials('https://example.com');
 * console.log('Credentials reset for server');
 * ```
 */
export function resetInternetCredentials(options: BaseOptions): Promise<void> {
  return RNKeychainManager.resetInternetCredentialsForOptions(options);
}

/**
 * Gets the type of biometric authentication supported by the device.
 *
 * @returns {Promise<null | BIOMETRY_TYPE>} Resolves to a `BIOMETRY_TYPE` when supported, otherwise `null`.
 *
 * @example
 * ```typescript
 * const biometryType = await Keychain.getSupportedBiometryType();
 * console.log('Supported Biometry Type:', biometryType);
 * ```
 */
export function getSupportedBiometryType(): Promise<null | BIOMETRY_TYPE> {
  if (!RNKeychainManager.getSupportedBiometryType) {
    return Promise.resolve(null);
  }

  return RNKeychainManager.getSupportedBiometryType();
}

/**
 * Request shared web credentials.
 *
 * @platform iOS
 *
 * @returns {Promise<false | SharedWebCredentials>} Resolves to an object containing `server`, `username`, and `password` if approved, or `false` if denied.
 *
 * @example
 * ```typescript
 * const credentials = await Keychain.requestSharedWebCredentials();
 * if (credentials) {
 *   console.log('Shared credentials retrieved:', credentials);
 * } else {
 *   console.log('No shared credentials available');
 * }
 * ```
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
 * Sets shared web credentials.
 *
 * @platform iOS
 *
 * @param {string} server - The server URL.
 * @param {string} username - The username or e-mail to be saved.
 * @param {string} [password] - The password to be saved.
 *
 * @returns {Promise<void>} Resolves when the operation is completed.
 *
 * @example
 * ```typescript
 * await Keychain.setSharedWebCredentials('https://example.com', 'username', 'password');
 * console.log('Shared web credentials set');
 * ```
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
 * Checks if the current device supports the specified authentication policy.
 *
 * @platform iOS
 *
 * @param {AuthenticationTypeOption} [options] - A keychain options object.
 *
 * @returns {Promise<boolean>} Resolves to `true` when supported, otherwise `false`.
 *
 * @example
 * ```typescript
 * const canAuthenticate = await Keychain.canImplyAuthentication();
 * console.log('Can imply authentication:', canAuthenticate);
 * ```
 */
export function canImplyAuthentication(
  options?: AuthenticationTypeOption
): Promise<boolean> {
  if (!RNKeychainManager.canCheckAuthentication) {
    return Promise.resolve(false);
  }
  return RNKeychainManager.canCheckAuthentication(options);
}

/**
 * Returns the security level supported by the library on the current device.
 *
 * @platform Android
 *
 * @param {AccessControlOption} [options] - A keychain options object.
 *
 * @returns {Promise<null | SECURITY_LEVEL>} Resolves to a `SECURITY_LEVEL` when supported, otherwise `null`.
 *
 * @example
 * ```typescript
 * const securityLevel = await Keychain.getSecurityLevel();
 * console.log('Security Level:', securityLevel);
 * ```
 */
export function getSecurityLevel(
  options?: AccessControlOption
): Promise<null | SECURITY_LEVEL> {
  if (!RNKeychainManager.getSecurityLevel) {
    return Promise.resolve(null);
  }
  return RNKeychainManager.getSecurityLevel(options);
}

/**
 * Checks if passcode authentication is available on the current device.
 *
 * @returns {Promise<boolean>} Resolves to `true` if passcode authentication is available, otherwise `false`.
 *
 * @example
 * ```typescript
 * const isAvailable = await Keychain.isPasscodeAuthAvailable();
 * console.log('Passcode authentication available:', isAvailable);
 * ```
 */
export function isPasscodeAuthAvailable(): Promise<boolean> {
  if (!RNKeychainManager.isPasscodeAuthAvailable) {
    return Promise.resolve(false);
  }
  return RNKeychainManager.isPasscodeAuthAvailable();
}

export * from './enums';
export * from './types';
/** @ignore */
export default {
  SECURITY_LEVEL,
  ACCESSIBLE,
  ACCESS_CONTROL,
  AUTHENTICATION_TYPE,
  BIOMETRY_TYPE,
  STORAGE_TYPE,
  getSecurityLevel,
  canImplyAuthentication,
  getSupportedBiometryType,
  setInternetCredentials,
  isPasscodeAuthAvailable,
  getInternetCredentials,
  resetInternetCredentials,
  setGenericPassword,
  getGenericPassword,
  getAllGenericPasswordServices,
  resetGenericPassword,
  requestSharedWebCredentials,
  setSharedWebCredentials,
};
