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
  /**
   * Samsung Knox hardware-backed encryption.
   * Uses TIMAKeyStore (API < 31) or StrongBox (API >= 31) on Samsung devices.
   * Falls back to standard Android Keystore on non-Samsung devices.
   * @platform Android (Samsung devices)
   */
  KNOX = 'KnoxAES',
}

/**
 * Enum representing keychain error codes for reliable, language-independent error handling.
 * These codes are returned in the `error.code` property when keychain operations fail.
 *
 * Use these constants instead of parsing error messages for reliable error detection.
 */
export enum ERROR_CODE {
  /**
   * The device does not have a passcode, PIN, pattern, or password set up.
   * This occurs when `ACCESS_CONTROL` requires device passcode authentication.
   *
   * Action: User must configure device lock screen security (passcode/PIN/pattern) in Settings.
   * This error cannot be resolved by retrying the operation.
   */
  PASSCODE_NOT_SET = 'E_PASSCODE_NOT_SET',
  /**
   * No biometric authentication methods (fingerprint, face, iris) are enrolled on the device.
   * This occurs when `ACCESS_CONTROL` requires biometric authentication.
   *
   * Action: User must enroll biometric authentication (fingerprint/face recognition) in device Settings.
   * This error cannot be resolved by retrying the operation.
   */
  BIOMETRIC_NOT_ENROLLED = 'E_BIOMETRIC_NOT_ENROLLED',
  /**
   * The biometric authentication process timed out. The user took too long to provide biometric
   * input or the system canceled the authentication request. This occurs when `ACCESS_CONTROL` requires
   * biometric authentication.
   *
   * Action: Consider implementing retry logic with user-friendly messaging to prompt authentication again.
   *
   * @platform Android
   */
  BIOMETRIC_TIMEOUT = 'E_BIOMETRIC_TIMEOUT',
  /**
   * Biometric authentication is temporarily locked due to too many failed attempts. The device has disabled
   * biometric authentication for a short period. This occurs when `ACCESS_CONTROL` requires biometric authentication.
   *
   * Action: User must wait for lockout period to expire or use device passcode as alternative authentication.
   * Lockout typically lasts 30 seconds to a few minutes.
   */
  BIOMETRIC_LOCKOUT = 'E_BIOMETRIC_LOCKOUT',
  /**
   * Biometric authentication is permanently locked due to too many failed attempts. This occurs when
   * `ACCESS_CONTROL` requires biometric authentication.
   *
   * Action: User must unlock device through lock screen to restore biometric authentication functionality.
   * This error cannot be resolved by retrying the operation.
   *
   * @platform Android
   */
  BIOMETRIC_LOCKOUT_PERMANENT = 'E_BIOMETRIC_LOCKOUT_PERMANENT',
  /**
   * Biometric authentication hardware is temporarily unavailable, perhaps when the hardware is being used by
   * another app or is temporarily disabled. This occurs when `ACCESS_CONTROL` requires biometric authentication.
   *
   * Action: Consider implementing retry logic with exponential backoff.
   *
   * @platform Android
   */
  BIOMETRIC_TEMPORARILY_UNAVAILABLE = 'E_BIOMETRIC_TEMPORARILY_UNAVAILABLE',
  /**
   * Biometric authentication is not available on this device. This can occur when the device lacks biometric
   * hardware. This occurs when `ACCESS_CONTROL` requires biometric authentication.
   *
   * Action: Use `getSupportedBiometryType` to detect availability and/or consider using `ACCESS_CONTROL` options
   * that include passcode authentication as a fallback.
   *
   * @platform Android
   */
  BIOMETRIC_UNAVAILABLE = 'E_BIOMETRIC_UNAVAILABLE',
  /**
   * An Android vendor-specific biometric authentication error occurred. These are custom errors from device
   * manufacturers that don't fit into standard `BiometricPrompt` error codes. This occurs when `ACCESS_CONTROL`
   * requires biometric authentication.
   *
   * Action: Display the vendor error message directly to users as intended by the device manufacturer.
   *
   * @platform Android
   */
  BIOMETRIC_VENDOR_ERROR = 'E_BIOMETRIC_VENDOR_ERROR',
  /**
   * Authentication interaction is not allowed in the current context. This typically occurs when the app is
   * in the background or another authentication prompt is already active. This occurs when `ACCESS_CONTROL`
   * requires biometric authentication.
   *
   * Action: Ensure authentication requests are only made when the app is active and consider checking
   * React Native's `AppState` before requesting authentication.
   *
   * @platform iOS
   */
  AUTH_INTERACTION_NOT_ALLOWED = 'E_AUTH_INTERACTION_NOT_ALLOWED',
  /**
   * Stored credentials are no longer accessible. This is happens when the biometric set (i.e. fingerprints) or
   * device passcode have been modified since the credentials were stored. This occurs when `ACCESS_CONTROL`
   * requires biometric authentication and the current set is specified. Due to the way this library currently
   * handles user authentication validity duration, this error code is never thrown.
   *
   * Action: Display user-friendly messaging for awareness before restoring the value.
   *
   * @platform Android
   */
  AUTH_INVALIDATED = 'E_AUTH_INVALIDATED',
  /**
   * The user canceled the authentication process. This occurs when users explicitly dismiss authentication
   * prompts or use cancel buttons.
   *
   * Action: Handle gracefully by providing alternative authentication methods or allowing users to manually
   * retry when ready. Avoid immediately re-prompting for authentication.
   */
  AUTH_CANCELED = 'E_AUTH_CANCELED',
  /**
   * A general authentication error occurred that doesn't fit into more specific error codes. This represents
   * unexpected authentication failures that don't match other defined error categories.
   *
   * Action: Handle as a catch-all condition. Display a user-friendly generic error message to users and consider providing
   * alternative authentication methods.
   */
  AUTH_ERROR = 'E_AUTH_ERROR',
  /**
   * Invalid or missing parameters were provided. This includes invalid security levels, unsupported Android
   * SDK versions, and incorrect parameter combinations.
   *
   * Action: Review and correct the parameters passed to the library. Check documentation for required fields
   * and valid parameter combinations.
   */
  INVALID_PARAMETERS = 'E_INVALID_PARAMETERS',
  /**
   * Unable to access iOS Keychain or Android KeyStore. This includes I/O errors, memory allocation failures,
   * storage unavailability, or system-level storage access problems.
   *
   * Users should try restarting the app or device, freeing up storage space, or checking device storage
   * health. If issues persist, users should consider disabling biometric/passcode authentication in favour
   * of alternative authentication methods. This error cannot be resolved by retrying the operation.
   */
  STORAGE_ACCESS_ERROR = 'E_STORAGE_ACCESS_ERROR',
  /**
   * An internal or unexpected error occurred. This represents error conditions that don't map to specific
   * error codes. This error cannot be resolved by retrying the operation.
   *
   * Action: Handle as a catch-all condition. Display a user-friendly generic error message and review
   * underlying error message for specific guidance when available.
   */
  INTERNAL_ERROR = 'E_INTERNAL_ERROR',
}
