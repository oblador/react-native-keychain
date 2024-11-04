import type {
  ACCESS_CONTROL,
  ACCESSIBLE,
  AUTHENTICATION_TYPE,
  SECURITY_LEVEL,
  SECURITY_RULES,
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
};

/** Base options for keychain functions. */
export type SetOptions = {
  /** The access group to share keychain items between apps.
   * @platform iOS, visionOS
   */
  accessGroup?: string;
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
} & BaseOptions &
  AccessControlOption;

/** Base options for keychain functions. */
export type GetOptions = {
  /** The access control policy to use for the keychain item. */
  accessControl?: ACCESS_CONTROL;
  /** The security rules to apply when storing the keychain item.
   * @platform Android
   * @default SECURITY_RULES.AUTOMATIC_UPGRADE
   */
  rules?: SECURITY_RULES;
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
  authenticationPrompt?: string | AuthenticationPrompt;
} & BaseOptions &
  AccessControlOption;

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
