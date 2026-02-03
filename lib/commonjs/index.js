"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
var _exportNames = {
  setGenericPassword: true,
  getGenericPassword: true,
  hasGenericPassword: true,
  resetGenericPassword: true,
  getAllGenericPasswordServices: true,
  hasInternetCredentials: true,
  setInternetCredentials: true,
  getInternetCredentials: true,
  resetInternetCredentials: true,
  getSupportedBiometryType: true,
  requestSharedWebCredentials: true,
  setSharedWebCredentials: true,
  canImplyAuthentication: true,
  getSecurityLevel: true,
  isPasscodeAuthAvailable: true
};
exports.canImplyAuthentication = canImplyAuthentication;
exports.default = void 0;
exports.getAllGenericPasswordServices = getAllGenericPasswordServices;
exports.getGenericPassword = getGenericPassword;
exports.getInternetCredentials = getInternetCredentials;
exports.getSecurityLevel = getSecurityLevel;
exports.getSupportedBiometryType = getSupportedBiometryType;
exports.hasGenericPassword = hasGenericPassword;
exports.hasInternetCredentials = hasInternetCredentials;
exports.isPasscodeAuthAvailable = isPasscodeAuthAvailable;
exports.requestSharedWebCredentials = requestSharedWebCredentials;
exports.resetGenericPassword = resetGenericPassword;
exports.resetInternetCredentials = resetInternetCredentials;
exports.setGenericPassword = setGenericPassword;
exports.setInternetCredentials = setInternetCredentials;
exports.setSharedWebCredentials = setSharedWebCredentials;
var _reactNative = require("react-native");
var _enums = require("./enums.js");
Object.keys(_enums).forEach(function (key) {
  if (key === "default" || key === "__esModule") return;
  if (Object.prototype.hasOwnProperty.call(_exportNames, key)) return;
  if (key in exports && exports[key] === _enums[key]) return;
  Object.defineProperty(exports, key, {
    enumerable: true,
    get: function () {
      return _enums[key];
    }
  });
});
var _normalizeOptions = require("./normalizeOptions.js");
var _types = require("./types.js");
Object.keys(_types).forEach(function (key) {
  if (key === "default" || key === "__esModule") return;
  if (Object.prototype.hasOwnProperty.call(_exportNames, key)) return;
  if (key in exports && exports[key] === _types[key]) return;
  Object.defineProperty(exports, key, {
    enumerable: true,
    get: function () {
      return _types[key];
    }
  });
});
const {
  RNKeychainManager
} = _reactNative.NativeModules;

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
function setGenericPassword(username, password, options) {
  return RNKeychainManager.setGenericPasswordForOptions((0, _normalizeOptions.normalizeAuthPrompt)(options), username, password);
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
function getGenericPassword(options) {
  return RNKeychainManager.getGenericPasswordForOptions((0, _normalizeOptions.normalizeAuthPrompt)(options));
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
function hasGenericPassword(options) {
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
function resetGenericPassword(options) {
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
function getAllGenericPasswordServices(options) {
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
function hasInternetCredentials(options) {
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
function setInternetCredentials(server, username, password, options) {
  return RNKeychainManager.setInternetCredentialsForServer(server, username, password, (0, _normalizeOptions.normalizeAuthPrompt)(options));
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
function getInternetCredentials(server, options) {
  return RNKeychainManager.getInternetCredentialsForServer(server, (0, _normalizeOptions.normalizeAuthPrompt)(options));
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
function resetInternetCredentials(options) {
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
function getSupportedBiometryType() {
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
function requestSharedWebCredentials() {
  if (_reactNative.Platform.OS !== 'ios') {
    return Promise.reject(new Error(`requestSharedWebCredentials() is not supported on ${_reactNative.Platform.OS} yet`));
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
function setSharedWebCredentials(server, username, password) {
  if (_reactNative.Platform.OS !== 'ios') {
    return Promise.reject(new Error(`setSharedWebCredentials() is not supported on ${_reactNative.Platform.OS} yet`));
  }
  return RNKeychainManager.setSharedWebCredentialsForServer(server, username, password);
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
function canImplyAuthentication(options) {
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
function getSecurityLevel(options) {
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
function isPasscodeAuthAvailable() {
  if (!RNKeychainManager.isPasscodeAuthAvailable) {
    return Promise.resolve(false);
  }
  return RNKeychainManager.isPasscodeAuthAvailable();
}
/** @ignore */
var _default = exports.default = {
  SECURITY_LEVEL: _enums.SECURITY_LEVEL,
  ACCESSIBLE: _enums.ACCESSIBLE,
  ACCESS_CONTROL: _enums.ACCESS_CONTROL,
  AUTHENTICATION_TYPE: _enums.AUTHENTICATION_TYPE,
  BIOMETRY_TYPE: _enums.BIOMETRY_TYPE,
  STORAGE_TYPE: _enums.STORAGE_TYPE,
  ERROR_CODE: _enums.ERROR_CODE,
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
  setSharedWebCredentials
};
//# sourceMappingURL=index.js.map