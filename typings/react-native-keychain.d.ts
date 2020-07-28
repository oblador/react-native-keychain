import { EmitterSubscription } from "react-native";

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
    ALWAYS_THIS_DEVICE_ONLY = 'AccessibleAlwaysThisDeviceOnly',
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
  ): Promise<false | SharedWebCredentials>;

  function resetGenericPassword(options?: Options): Promise<boolean>;

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
  ): Promise<false | UserCredentials>;

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

  /**
   * Registers an event listener that's called when fallback fingerprint authentication begins.
   *
   * This is ideally when you want to display your UI that tells the user to scan their fingerprint.
   */
  function addOnFallbackAuthenticationStartListener(listener: () => void): EmitterSubscription;

  /**
   * Registers an event listener that's called when fallback fingerprint authentication succeeds and the keystore entry
   * has been successfully decrypted.
   *
   * You ideally want to close any UI elements that you opened when authentication started.
   */
  function addOnFallbackAuthenticationSuccessListener(listener: () => void): EmitterSubscription

  interface OnFailureEvent {
    shouldHideUI: boolean;
    message?: string;
  }

  /**
   * Registers an event listener that's called when fallback fingerprint authentication fails and the user cannot try
   * again or recover from the failure.
   *
   * Depending on the event properties, you'll want to either continue showing or hide your UI, see parameter info below.
   *
   * @param {OnFailureEvent} event An event that may contain a message that can be displayed to the user as-is that will
   *                               instruct them on what to do before trying to scan their fingerprint again and whether
   *                               or not the UI should be hidden, in case the cause of failure isn't recoverable by the
   *                               user.
   */
  function addOnFallbackAuthenticationFailureListener(listener: (event: OnFailureEvent) => void): EmitterSubscription;
}
