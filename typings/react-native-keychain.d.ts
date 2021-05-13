declare module 'react-native-keychain' {
  export interface Result {
    service: string;
    storage: string;
  }

  export interface UserCredentials extends Result {
    username: string;
    password: string;
  }

  export interface SharedWebCredentials extends UserCredentials {
    server: string;
  }

  export enum ACCESSIBLE {
    WHEN_UNLOCKED = 'AccessibleWhenUnlocked',
    AFTER_FIRST_UNLOCK = 'AccessibleAfterFirstUnlock',
    ALWAYS = 'AccessibleAlways',
    WHEN_PASSCODE_SET_THIS_DEVICE_ONLY = 'AccessibleWhenPasscodeSetThisDeviceOnly',
    WHEN_UNLOCKED_THIS_DEVICE_ONLY = 'AccessibleWhenUnlockedThisDeviceOnly',
    AFTER_FIRST_UNLOCK_THIS_DEVICE_ONLY = 'AccessibleAfterFirstUnlockThisDeviceOnly',
  }

  export enum ACCESS_CONTROL {
    USER_PRESENCE = 'UserPresence',
    BIOMETRY_ANY = 'BiometryAny',
    BIOMETRY_CURRENT_SET = 'BiometryCurrentSet',
    DEVICE_PASSCODE = 'DevicePasscode',
    APPLICATION_PASSWORD = 'ApplicationPassword',
    BIOMETRY_ANY_OR_DEVICE_PASSCODE = 'BiometryAnyOrDevicePasscode',
    BIOMETRY_CURRENT_SET_OR_DEVICE_PASSCODE = 'BiometryCurrentSetOrDevicePasscode',
  }

  export enum AUTHENTICATION_TYPE {
    DEVICE_PASSCODE_OR_BIOMETRICS = 'AuthenticationWithBiometricsDevicePasscode',
    BIOMETRICS = 'AuthenticationWithBiometrics',
  }

  export enum SECURITY_LEVEL {
    SECURE_SOFTWARE,
    SECURE_HARDWARE,
    ANY,
  }

  export enum BIOMETRY_TYPE {
    TOUCH_ID = 'TouchID',
    FACE_ID = 'FaceID',
    FINGERPRINT = 'Fingerprint',
    FACE = 'Face',
    IRIS = 'Iris',
  }

  export enum STORAGE_TYPE {
    FB = 'FacebookConceal',
    AES = 'KeystoreAESCBC',
    RSA = 'KeystoreRSAECB',
    KC = 'keychain',
  }

  export enum SECURITY_RULES {
    NONE = 'none',
    AUTOMATIC_UPGRADE = 'automaticUpgradeToMoreSecuredStorage',
  }

  export interface AuthenticationPrompt {
    title?: string;
    subtitle?: string;
    description?: string;
    cancel?: string;
  }

  export interface Options {
    accessControl?: ACCESS_CONTROL;
    accessGroup?: string;
    accessible?: ACCESSIBLE;
    authenticationPrompt?: string | AuthenticationPrompt;
    authenticationType?: AUTHENTICATION_TYPE;
    service?: string;
    securityLevel?: SECURITY_LEVEL;
    storage?: STORAGE_TYPE;
    rules?: SECURITY_RULES;
  }

  function setGenericPassword(
    username: string,
    password: string,
    options?: Options
  ): Promise<false | Result>;

  function getGenericPassword(
    options?: Options
  ): Promise<false | UserCredentials>;

  function resetGenericPassword(options?: Options): Promise<boolean>;

  function getAllGenericPasswordServices(): Promise<string[]>;

  function hasInternetCredentials(server: string): Promise<false | Result>;

  function setInternetCredentials(
    server: string,
    username: string,
    password: string,
    options?: Options
  ): Promise<false | Result>;

  function getInternetCredentials(
    server: string,
    options?: Options
  ): Promise<false | SharedWebCredentials>;

  function resetInternetCredentials(
    server: string,
    options?: Options
  ): Promise<void>;

  function getSupportedBiometryType(
    options?: Options
  ): Promise<null | BIOMETRY_TYPE>;

  /** IOS ONLY */

  function requestSharedWebCredentials(): Promise<false | SharedWebCredentials>;

  function setSharedWebCredentials(
    server: string,
    username: string,
    password?: string
  ): Promise<void>;

  function canImplyAuthentication(options?: Options): Promise<boolean>;

  /** ANDROID ONLY */

  function getSecurityLevel(options?: Options): Promise<null | SECURITY_LEVEL>;
}
