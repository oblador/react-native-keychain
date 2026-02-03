import { ACCESSIBLE, ACCESS_CONTROL, AUTHENTICATION_TYPE, SECURITY_LEVEL, STORAGE_TYPE, BIOMETRY_TYPE, ERROR_CODE } from './enums';
import type { Result, UserCredentials, SharedWebCredentials, GetOptions, GetAllOptions, BaseOptions, SetOptions, AuthenticationTypeOption, AccessControlOption } from './types';
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
export declare function setGenericPassword(username: string, password: string, options?: SetOptions): Promise<false | Result>;
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
export declare function getGenericPassword(options?: GetOptions): Promise<false | UserCredentials>;
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
export declare function hasGenericPassword(options?: BaseOptions): Promise<boolean>;
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
export declare function resetGenericPassword(options?: BaseOptions): Promise<boolean>;
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
export declare function getAllGenericPasswordServices(options?: GetAllOptions): Promise<string[]>;
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
export declare function hasInternetCredentials(options: string | BaseOptions): Promise<boolean>;
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
export declare function setInternetCredentials(server: string, username: string, password: string, options?: SetOptions): Promise<false | Result>;
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
export declare function getInternetCredentials(server: string, options?: GetOptions): Promise<false | UserCredentials>;
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
export declare function resetInternetCredentials(options: BaseOptions): Promise<void>;
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
export declare function getSupportedBiometryType(): Promise<null | BIOMETRY_TYPE>;
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
export declare function requestSharedWebCredentials(): Promise<false | SharedWebCredentials>;
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
export declare function setSharedWebCredentials(server: string, username: string, password?: string): Promise<void>;
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
export declare function canImplyAuthentication(options?: AuthenticationTypeOption): Promise<boolean>;
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
export declare function getSecurityLevel(options?: AccessControlOption): Promise<null | SECURITY_LEVEL>;
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
export declare function isPasscodeAuthAvailable(): Promise<boolean>;
export * from './enums';
export * from './types';
/** @ignore */
declare const _default: {
    SECURITY_LEVEL: typeof SECURITY_LEVEL;
    ACCESSIBLE: typeof ACCESSIBLE;
    ACCESS_CONTROL: typeof ACCESS_CONTROL;
    AUTHENTICATION_TYPE: typeof AUTHENTICATION_TYPE;
    BIOMETRY_TYPE: typeof BIOMETRY_TYPE;
    STORAGE_TYPE: typeof STORAGE_TYPE;
    ERROR_CODE: typeof ERROR_CODE;
    getSecurityLevel: typeof getSecurityLevel;
    canImplyAuthentication: typeof canImplyAuthentication;
    getSupportedBiometryType: typeof getSupportedBiometryType;
    setInternetCredentials: typeof setInternetCredentials;
    isPasscodeAuthAvailable: typeof isPasscodeAuthAvailable;
    getInternetCredentials: typeof getInternetCredentials;
    resetInternetCredentials: typeof resetInternetCredentials;
    setGenericPassword: typeof setGenericPassword;
    getGenericPassword: typeof getGenericPassword;
    getAllGenericPasswordServices: typeof getAllGenericPasswordServices;
    resetGenericPassword: typeof resetGenericPassword;
    requestSharedWebCredentials: typeof requestSharedWebCredentials;
    setSharedWebCredentials: typeof setSharedWebCredentials;
};
export default _default;
//# sourceMappingURL=index.d.ts.map