import { NativeModules, Platform } from 'react-native';

const { RNKeychainManager } = NativeModules;

/**
 * Enum representing when a keychain item is accessible.
 */
export enum ACCESSIBLE {
  /** The data in the keychain item can be accessed only while the device is unlocked by the user. */
  WHEN_UNLOCKED = 'AccessibleWhenUnlocked',
  /** The data in the keychain item cannot be accessed after a restart until the device has been unlocked once by the user. */
  AFTER_FIRST_UNLOCK = 'AccessibleAfterFirstUnlock',
  /** The data in the keychain item can always be accessed regardless of whether the device is locked. */
  ALWAYS = 'AccessibleAlways',
  /** The data in the keychain can only be accessed when the device is unlocked. Only available if a passcode is set on the device. Items with this attribute never migrate to a new device. */
  WHEN_PASSCODE_SET_THIS_DEVICE_ONLY = 'AccessibleWhenPasscodeSetThisDeviceOnly',
  /** The data in the keychain item can be accessed only while the device is unlocked by the user. Items with this attribute do not migrate to a new device. */
  WHEN_UNLOCKED_THIS_DEVICE_ONLY = 'AccessibleWhenUnlockedThisDeviceOnly',
  /** The data in the keychain item cannot be accessed after a restart until the device has been unlocked once by the user. Items with this attribute never migrate to a new device. */
  AFTER_FIRST_UNLOCK_THIS_DEVICE_ONLY = 'AccessibleAfterFirstUnlockThisDeviceOnly',
}

/**
 * Enum representing access control options.
 */
export enum ACCESS_CONTROL {
  /** Constraint to access an item with either Touch ID or passcode. */
  USER_PRESENCE = 'UserPresence',
  /** Constraint to access an item with Touch ID for any enrolled fingers. */
  BIOMETRY_ANY = 'BiometryAny',
  /** Constraint to access an item with Touch ID for currently enrolled fingers. */
  BIOMETRY_CURRENT_SET = 'BiometryCurrentSet',
  /** Constraint to access an item with the device passcode. */
  DEVICE_PASSCODE = 'DevicePasscode',
  /** Constraint to use an application-provided password for data encryption key generation. */
  APPLICATION_PASSWORD = 'ApplicationPassword',
  /** Constraint to access an item with Touch ID for any enrolled fingers or passcode. */
  BIOMETRY_ANY_OR_DEVICE_PASSCODE = 'BiometryAnyOrDevicePasscode',
  /** Constraint to access an item with Touch ID for currently enrolled fingers or passcode. */
  BIOMETRY_CURRENT_SET_OR_DEVICE_PASSCODE = 'BiometryCurrentSetOrDevicePasscode',
}

/**
 * Enum representing authentication types.
 */
export enum AUTHENTICATION_TYPE {
  /** Device owner is going to be authenticated by biometry or device passcode. */
  DEVICE_PASSCODE_OR_BIOMETRICS = 'AuthenticationWithBiometricsDevicePasscode',
  /** Device owner is going to be authenticated using a biometric method (Touch ID or Face ID). */
  BIOMETRICS = 'AuthenticationWithBiometrics',
}

/**
 * Enum representing security levels. (Android only)
 */
export enum SECURITY_LEVEL {
  /** Secure storage using software-based encryption. */
  SECURE_SOFTWARE = RNKeychainManager &&
    RNKeychainManager.SECURITY_LEVEL_SECURE_SOFTWARE,
  /** Secure storage using hardware-based encryption. */
  SECURE_HARDWARE = RNKeychainManager &&
    RNKeychainManager.SECURITY_LEVEL_SECURE_HARDWARE,
  /** Any security level. */
  ANY = RNKeychainManager && RNKeychainManager.SECURITY_LEVEL_ANY,
}

/**
 * Enum representing types of biometric authentication supported by the device.
 */
export enum BIOMETRY_TYPE {
  /** Device supports authentication with Touch ID. (iOS only) */
  TOUCH_ID = 'TouchID',
  /** Device supports authentication with Face ID. (iOS only) */
  FACE_ID = 'FaceID',
  /** Device supports authentication with Optic ID. (visionOS only) */
  OPTIC_ID = 'OpticID',
  /** Device supports authentication with Fingerprint. (Android only) */
  FINGERPRINT = 'Fingerprint',
  /** Device supports authentication with Face Recognition. (Android only) */
  FACE = 'Face',
  /** Device supports authentication with Iris Recognition. (Android only) */
  IRIS = 'Iris',
}

/**
 * Enum representing storage types. (Android only)
 */
export enum STORAGE_TYPE {
  /** Facebook compatibility cipher. */
  FB = 'FacebookConceal',
  /** Encryptions without human interaction. */
  AES = 'KeystoreAESCBC',
  /** Encryption with biometrics. */
  RSA = 'KeystoreRSAECB',
  /** iOS Keychain or Android default storage. */
  KC = 'keychain',
}

/**
 * Enum representing security rules for storage. (Android only)
 */
export enum SECURITY_RULES {
  /** No special security rules applied. */
  NONE = 'none',
  /** Upgrade secret to the best available storage as soon as it is available and user request secret extraction. Upgrade not applied till we request the secret. This rule only applies to secrets stored with FacebookConseal. */
  AUTOMATIC_UPGRADE = 'automaticUpgradeToMoreSecuredStorage',
}

/**
 * Options for authentication prompt displayed to the user.
 */
export type AuthenticationPrompt = {
  /** The title for the authentication prompt. */
  title?: string;
  /** The subtitle for the authentication prompt (Android only). */
  subtitle?: string;
  /** The description for the authentication prompt (Android only). */
  description?: string;
  /** The cancel button text for the authentication prompt (Android only). */
  cancel?: string;
};

/** Base options for keychain functions. */
export type BaseOptions = {
  /** The access control policy to use for the keychain item. */
  accessControl?: ACCESS_CONTROL;
  /** The access group to share keychain items between apps (iOS and visionOS only). */
  accessGroup?: string;
  /** Specifies when a keychain item is accessible (iOS and visionOS only).*/
  accessible?: ACCESSIBLE;
  /** Authentication type for retrieving keychain item (iOS and visionOS only). */
  authenticationType?: AUTHENTICATION_TYPE;
  /** The service name to associate with the keychain item. */
  service?: string;
  /** The desired security level of the keychain item. */
  securityLevel?: SECURITY_LEVEL;
  /** The storage type (Android only). */
  storage?: STORAGE_TYPE;
  /** The security rules to apply when storing the keychain item (Android only). */
  rules?: SECURITY_RULES;
};

/**
 * Normalized options including authentication prompt details.
 */
export type NormalizedOptions = {
  /** Authentication prompt details. */
  authenticationPrompt?: AuthenticationPrompt;
} & BaseOptions;

/**
 * Options for keychain functions.
 */
export type Options = Partial<
  {
    /** Authentication prompt details or a title string. */
    authenticationPrompt?: string | AuthenticationPrompt;
  } & BaseOptions
>;

/**
 * Result returned by keychain functions.
 */
export type Result = {
  /** The service name associated with the keychain item. */
  service: string;
  /** The storage type used for the keychain item. */
  storage: STORAGE_TYPE;
};

/**
 * User credentials returned by keychain functions.
 */
export type UserCredentials = {
  /** The username associated with the keychain item. */
  username: string;
  /** The password associated with the keychain item. */
  password: string;
} & Result;

/**
 * Shared web credentials returned by keychain functions (iOS only).
 */
export type SharedWebCredentials = {
  /** The server associated with the keychain item. */
  server: string;
} & UserCredentials;

// Default authentication prompt options
const AUTH_PROMPT_DEFAULTS: AuthenticationPrompt = {
  title: 'Authenticate to retrieve secret',
  cancel: 'Cancel',
};

/**
 * Normalizes the service option, handling deprecated string format.
 *
 * @param {string | Options} [serviceOrOptions] - Service name string or options object.
 * @returns {Options} Normalized options object.
 */
function normalizeServiceOption(serviceOrOptions?: string | Options): Options {
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

/**
 * Normalizes options, ensuring proper format and defaults.
 *
 * @param {string | Options} [serviceOrOptions] - Service name string or options object.
 * @returns {NormalizedOptions} Normalized options object with default values.
 */
function normalizeOptions(
  serviceOrOptions?: string | Options
): NormalizedOptions {
  const options = {
    ...normalizeServiceOption(serviceOrOptions),
  } as NormalizedOptions;
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

  return options;
}

/**
 * Saves the `username` and `password` combination for the given service.
 *
 * @param {string} username - The username or e-mail to be saved.
 * @param {string} password - The password to be saved.
 * @param {Options | string} [serviceOrOptions] - A keychain options object or a service name string. Passing a service name as a string is deprecated.
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
 * Fetches the `username` and `password` combination for the given service.
 *
 * @param {Options | string} [serviceOrOptions] - A keychain options object or a service name string.
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
  serviceOrOptions?: string | Options
): Promise<false | UserCredentials> {
  const options = normalizeOptions(serviceOrOptions);
  return RNKeychainManager.getGenericPasswordForOptions(options);
}

/**
 * Checks if generic password exists for the given service.
 *
 * @param {Options | string} [serviceOrOptions] - A keychain options object or a service name string.
 *
 * @returns {Promise<boolean>} Resolves to `true` if a password exists, otherwise `false`.
 *
 * @example
 * ```typescript
 * const hasPassword = await Keychain.hasGenericPassword();
 * console.log('Password exists:', hasPassword);
 * ```
 */
export function hasGenericPassword(
  serviceOrOptions?: string | Options
): Promise<boolean> {
  const options = normalizeOptions(serviceOrOptions);
  return RNKeychainManager.hasGenericPasswordForOptions(options);
}

/**
 * Deletes all generic password keychain entries for the given service.
 *
 * @param {Options | string} [serviceOrOptions] - A keychain options object or a service name string.
 *
 * @returns {Promise<boolean>} Resolves to `true` when successful, otherwise `false`.
 *
 * @example
 * ```typescript
 * const success = await Keychain.resetGenericPassword();
 * console.log('Password reset successful:', success);
 * ```
 */
export function resetGenericPassword(
  serviceOrOptions?: string | Options
): Promise<boolean> {
  const options = normalizeOptions(serviceOrOptions);
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
export function getAllGenericPasswordServices(): Promise<string[]> {
  return RNKeychainManager.getAllGenericPasswordServices();
}

/**
 * Checks if internet credentials exist for the given server.
 *
 * @param {string} server - The server URL.
 *
 * @returns {Promise<false | Result>} Resolves to an object containing `service` and `storage` when successful, or `false` if not found.
 *
 * @example
 * ```typescript
 * const hasCredentials = await Keychain.hasInternetCredentials('https://example.com');
 * console.log('Internet credentials exist:', hasCredentials);
 * ```
 */
export function hasInternetCredentials(
  server: string
): Promise<false | Result> {
  return RNKeychainManager.hasInternetCredentialsForServer(server);
}

/**
 * Saves the internet credentials for the given server.
 *
 * @param {string} server - The server URL.
 * @param {string} username - The username or e-mail to be saved.
 * @param {string} password - The password to be saved.
 * @param {Options} [options] - A keychain options object.
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
 * Fetches the internet credentials for the given server.
 *
 * @param {string} server - The server URL.
 * @param {Options} [options] - A keychain options object.
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
  options?: Options
): Promise<false | UserCredentials> {
  return RNKeychainManager.getInternetCredentialsForServer(
    server,
    normalizeOptions(options)
  );
}

/**
 * Deletes all internet password keychain entries for the given server.
 *
 * @param {string} server - The server URL.
 *
 * @returns {Promise<void>} Resolves when the operation is completed.
 *
 * @example
 * ```typescript
 * await Keychain.resetInternetCredentials('https://example.com');
 * console.log('Credentials reset for server');
 * ```
 */
export function resetInternetCredentials(server: string): Promise<void> {
  return RNKeychainManager.resetInternetCredentialsForServer(server);
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
 * Request shared web credentials (iOS only).
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
 * Sets shared web credentials (iOS only).
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
 * Checks if the current device supports the specified authentication policy (iOS only).
 *
 * @param {Options} [options] - A keychain options object.
 *
 * @returns {Promise<boolean>} Resolves to `true` when supported, otherwise `false`.
 *
 * @example
 * ```typescript
 * const canAuthenticate = await Keychain.canImplyAuthentication();
 * console.log('Can imply authentication:', canAuthenticate);
 * ```
 */
export function canImplyAuthentication(options?: Options): Promise<boolean> {
  if (!RNKeychainManager.canCheckAuthentication) {
    return Promise.resolve(false);
  }
  return RNKeychainManager.canCheckAuthentication(options);
}

/**
 * Returns the security level supported by the library on the current device (Android only).
 *
 * @param {Options} [options] - A keychain options object.
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
  options?: Options
): Promise<null | SECURITY_LEVEL> {
  if (!RNKeychainManager.getSecurityLevel) {
    return Promise.resolve(null);
  }
  return RNKeychainManager.getSecurityLevel(options);
}

/** @ignore */
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
  getAllGenericPasswordServices,
  resetGenericPassword,
  requestSharedWebCredentials,
  setSharedWebCredentials,
};
