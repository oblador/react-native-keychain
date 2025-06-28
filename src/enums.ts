import { NativeModules } from 'react-native';

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
 * Enum representing security levels.
 * @platform Android
 */
export enum SECURITY_LEVEL {
  /** Requires for the key to be stored in the Android Keystore, separate from the encrypted data. */
  SECURE_SOFTWARE = RNKeychainManager &&
    RNKeychainManager.SECURITY_LEVEL_SECURE_SOFTWARE,
  /** Requires for the key to be stored on a secure hardware (Trusted Execution Environment or Secure Environment).
   * Read this article for more information: https://developer.android.com/privacy-and-security/keystore#ExtractionPrevention
   * */
  SECURE_HARDWARE = RNKeychainManager &&
    RNKeychainManager.SECURITY_LEVEL_SECURE_HARDWARE,
  /** No security guarantees needed (default value). Credentials can be stored in FB Secure Storage. */
  ANY = RNKeychainManager && RNKeychainManager.SECURITY_LEVEL_ANY,
}

/**
 * Enum representing types of biometric authentication supported by the device.
 */
export enum BIOMETRY_TYPE {
  /** Device supports authentication with Touch ID.
   * @platform iOS
   */
  TOUCH_ID = 'TouchID',
  /** Device supports authentication with Face ID.
   * @platform iOS
   */
  FACE_ID = 'FaceID',
  /** Device supports authentication with Optic ID.
   *  @platform visionOS
   */
  OPTIC_ID = 'OpticID',
  /** Device supports authentication with Fingerprint.
   * @platform Android
   */
  FINGERPRINT = 'Fingerprint',
  /** Device supports authentication with Face Recognition.
   * @platform Android
   */
  FACE = 'Face',
  /** Device supports authentication with Iris Recognition.
   * @platform Android
   */
  IRIS = 'Iris',
}

/**
 * Enum representing cryptographic storage types for sensitive data.
 *
 * Security Level Categories:
 *
 * 1. High Security (Biometric Authentication Required):
 * - AES_GCM: For sensitive local data (passwords, personal info)
 * - RSA: For asymmetric operations (signatures, key exchange)
 *
 * 2. Medium Security (No Authentication):
 * - AES_GCM_NO_AUTH: For app-level secrets and cached data
 *
 * 3. Legacy/Deprecated:
 * - AES_CBC: Outdated, use AES_GCM_NO_AUTH instead
 *
 * @platform Android
 */
export enum STORAGE_TYPE {
  /**
   * AES encryption in CBC (Cipher Block Chaining) mode.
   * Provides data confidentiality without authentication.
   * @deprecated Use AES_GCM_NO_AUTH instead.
   */
  AES_CBC = 'KeystoreAESCBC',
  /**
   * AES encryption in GCM (Galois/Counter Mode).
   * Provides both data confidentiality and authentication.
   */
  AES_GCM_NO_AUTH = 'KeystoreAESGCM_NoAuth',
  /**
   * AES-GCM encryption with biometric authentication.
   * Requires user authentication for both encryption and decryption operations.
   */
  AES_GCM = 'KeystoreAESGCM',
  /**
   * RSA encryption with biometric authentication.
   * Uses asymmetric encryption and requires biometric authentication.
   */
  RSA = 'KeystoreRSAECB',
}

/**
 * Enum representing keychain error codes for reliable, language-independent error handling.
 * These codes are returned in the `error.code` property when keychain operations fail.
 *
 * Use these constants instead of parsing error messages for reliable error detection
 * across different device languages and Android versions.
 */
export enum ERRORS {
  INVALID_PARAMETERS = 'E_INVALID_PARAMETERS',
  INTERACTIVE_MODE_UNAVAILABLE = 'E_INTERACTIVE_MODE_UNAVAILABLE',
  SECURITY_LEVEL_NOT_SUPPORTED = 'E_SECURITY_LEVEL_NOT_SUPPORTED',
  INSUFFICIENT_SECURITY_LEVEL = 'E_INSUFFICIENT_SECURITY_LEVEL',
  SDK_NOT_SUPPORTED = 'E_SDK_NOT_SUPPORTED',
  CRYPTO_FAILED = 'E_CRYPTO_FAILED',
  ACCESS_ERROR = 'E_ACCESS_ERROR',
  INTERNAL_ERROR = 'E_INTERNAL_ERROR',
  UNKNOWN_ERROR = 'E_UNKNOWN_ERROR',

  // Cross-platform biometric errors
  BIOMETRIC_USER_CANCEL = 'E_BIOMETRIC_USER_CANCEL',
  BIOMETRIC_NOT_ENROLLED = 'E_BIOMETRIC_NOT_ENROLLED',
  BIOMETRIC_HARDWARE_UNAVAILABLE = 'E_BIOMETRIC_HARDWARE_UNAVAILABLE',
  BIOMETRIC_TIMEOUT = 'E_BIOMETRIC_TIMEOUT',
  BIOMETRIC_UNKNOWN_ERROR = 'E_BIOMETRIC_UNKNOWN_ERROR',
  BIOMETRIC_PERMISSION_DENIED = 'E_BIOMETRIC_PERMISSION_DENIED',

  // Android-specific biometric errors
  BIOMETRIC_LOCKOUT = 'E_BIOMETRIC_LOCKOUT',
  BIOMETRIC_LOCKOUT_PERMANENT = 'E_BIOMETRIC_LOCKOUT_PERMANENT',
  BIOMETRIC_HARDWARE_NOT_PRESENT = 'E_BIOMETRIC_HARDWARE_NOT_PRESENT',
  BIOMETRIC_NO_SPACE = 'E_BIOMETRIC_NO_SPACE',
  BIOMETRIC_UNABLE_TO_PROCESS = 'E_BIOMETRIC_UNABLE_TO_PROCESS',
  BIOMETRIC_NO_DEVICE_CREDENTIAL = 'E_BIOMETRIC_NO_DEVICE_CREDENTIAL',
  BIOMETRIC_VENDOR_ERROR = 'E_BIOMETRIC_VENDOR_ERROR',

  // Android Keystore specific errors
  KEYSTORE_KEY_INVALIDATED = 'E_KEYSTORE_KEY_INVALIDATED',
  KEYSTORE_USER_NOT_AUTHENTICATED = 'E_KEYSTORE_USER_NOT_AUTHENTICATED',

  // iOS Keychain specific errors
  KEYCHAIN_DUPLICATE_ITEM = 'E_KEYCHAIN_DUPLICATE_ITEM',
  KEYCHAIN_INTERACTION_NOT_ALLOWED = 'E_KEYCHAIN_INTERACTION_NOT_ALLOWED',

  // iOS-specific error codes
  BIOMETRIC_AUTH_FAILED = 'E_BIOMETRIC_AUTH_FAILED',
}
