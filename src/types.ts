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
  /** Whether to synchronize the keychain item to iCloud. Omitted and `false`
   * select non-synchronizable items; `true` selects synchronizable items.
   * Use the same value when storing, reading, checking, and resetting an item.
   * @platform iOS
   * @default false
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
  /** Authentication prompt details.
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
} & BaseOptions &
  AccessControlOption;

export type GetOptions = {
  /** The access control policy to use for the keychain item.
   * On Android, omitted options reuse the policy saved with the credentials.
   * An explicit policy can narrow, but cannot broaden, the saved policy.
   * Older entries without a saved policy allow strong biometrics or device
   * credentials on Android 11+, and strong biometrics on earlier versions.
   */
  accessControl?: ACCESS_CONTROL;
  /** Authentication prompt details.
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
} & BaseOptions &
  AccessControlOption;

export type GetAllOptions = {
  /** Whether items requiring user authentication should be skipped
   * @platform iOS
   */
  skipUIAuth?: boolean;
};

/** Options for listing internet credential servers without reading passwords. */
export type GetAllInternetCredentialServersOptions = Pick<
  BaseOptions,
  'cloudSync' | 'accessGroup'
> & {
  /** Skip items that would require authentication rather than displaying UI.
   * @platform iOS, visionOS
   * @default true
   */
  skipUIAuth?: boolean;
  /** Include older Android records whose credential kind was not recorded.
   * These may also contain generic password services. Re-saving an entry with
   * setInternetCredentials identifies it for future listings.
   * @platform Android
   * @default false
   */
  includeLegacy?: boolean;
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
  storage: STORAGE_TYPE | 'keychain';
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

/** Internet credentials use `server` on iOS and `service` on Android. */
export type InternetCredentials = Omit<UserCredentials, 'service'> &
  ({ server: string; service?: string } | { service: string; server?: string });

/**
 * Shared web credentials returned by keychain functions.
 * @platform iOS
 */
export type SharedWebCredentials = {
  /** The server associated with the keychain item. */
  server: string;
} & Omit<UserCredentials, 'service'>;
