import type {
  ACCESS_CONTROL,
  ACCESSIBLE,
  AUTHENTICATION_TYPE,
  SECURITY_LEVEL,
  STORAGE_TYPE,
} from './enums';

/**
 * Options for authentication prompt displayed to the user.
 */
export type AuthenticationPrompt = {
  /** The title for the authentication prompt. */
  title?: string;
  /** The subtitle for the authentication prompt.
   * @platform Android
   */
  subtitle?: string;
  /** The description for the authentication prompt.
   * @platform Android
   */
  description?: string;
  /** The cancel button text for the authentication prompt.
   * @platform Android
   */
  cancel?: string;
};

export type BaseOptions = {
  /** The service name to associate with the keychain item.
   * @default 'App bundle ID'
   */
  service?: string;
  /** The server name to associate with the keychain item. */
  server?: string;
  /** Whether to synchronize the keychain item to iCloud.
   * @platform iOS
   */
  cloudSync?: boolean;
  /** The access group to share keychain items between apps.
   * @platform iOS, visionOS
   */
  accessGroup?: string;
};

export type SetOptions = {
  /** Specifies when a keychain item is accessible.
   * @platform iOS, visionOS
   * @default ACCESSIBLE.AFTER_FIRST_UNLOCK
   */
  accessible?: ACCESSIBLE;
  /** The desired security level of the keychain item.
   * @platform Android
   */
  securityLevel?: SECURITY_LEVEL;
  /** The storage type.
   * @platform Android
   * @default 'Best available storage'
   */
  storage?: STORAGE_TYPE;
  /** Authentication prompt details or a title string.
   * @default
   * ```json
   * {
   *   "title": "Authenticate to retrieve secret",
   *   "cancel": "Cancel"
   * }
   * ```
   *
   */
  authenticationPrompt?: AuthenticationPrompt;
  /**
   * Whether to use CryptoObject binding for biometric authentication.
   * When enabled, the biometric authentication is atomically bound to the
   * cryptographic operation, preventing race conditions where the KeyStore
   * considers authentication expired despite BiometricPrompt reporting success.
   *
   * Note: When enabled, only Class 3 (Strong) biometrics are allowed (typically
   * fingerprint). Face Unlock may not work on devices where it's classified as
   * Class 2 (Weak).
   *
   * @platform Android
   * @default false
   */
  authenticateWithCryptoObject?: boolean;
} & BaseOptions &
  AccessControlOption;

export type GetOptions = {
  /** The access control policy to use for the keychain item. */
  accessControl?: ACCESS_CONTROL;
  /** Authentication prompt details or a title string.
   * @default
   * ```json
   * {
   *   "title": "Authenticate to retrieve secret",
   *   "cancel": "Cancel"
   * }
   * ```
   *
   */
  authenticationPrompt?: AuthenticationPrompt;
  /**
   * Whether to use CryptoObject binding for biometric authentication.
   * When enabled, the biometric authentication is atomically bound to the
   * cryptographic operation, preventing race conditions where the KeyStore
   * considers authentication expired despite BiometricPrompt reporting success.
   *
   * Note: When enabled, only Class 3 (Strong) biometrics are allowed (typically
   * fingerprint). Face Unlock may not work on devices where it's classified as
   * Class 2 (Weak).
   *
   * @platform Android
   * @default false
   */
  authenticateWithCryptoObject?: boolean;
} & BaseOptions &
  AccessControlOption;

export type GetAllOptions = {
  /** Whether items requiring user authentication should be skipped
   * @platform iOS
   */
  skipUIAuth?: boolean;
};

export type AccessControlOption = {
  /** The access control policy to use for the keychain item. */
  accessControl?: ACCESS_CONTROL;
};

export type AuthenticationTypeOption = {
  /** Authentication type for retrieving keychain item.
   * @platform iOS, visionOS
   * @default AUTHENTICATION_TYPE.DEVICE_PASSCODE_OR_BIOMETRICS
   */
  authenticationType?: AUTHENTICATION_TYPE;
};

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
 * Shared web credentials returned by keychain functions.
 * @platform iOS
 */
export type SharedWebCredentials = {
  /** The server associated with the keychain item. */
  server: string;
} & UserCredentials;
