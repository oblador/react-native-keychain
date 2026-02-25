"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.STORAGE_TYPE = exports.SECURITY_LEVEL = exports.ERROR_CODE = exports.BIOMETRY_TYPE = exports.AUTHENTICATION_TYPE = exports.ACCESS_CONTROL = exports.ACCESSIBLE = void 0;
var _reactNative = require("react-native");
const {
  RNKeychainManager
} = _reactNative.NativeModules;

/**
 * Enum representing when a keychain item is accessible.
 */
let ACCESSIBLE = exports.ACCESSIBLE = /*#__PURE__*/function (ACCESSIBLE) {
  ACCESSIBLE["WHEN_UNLOCKED"] = "AccessibleWhenUnlocked";
  ACCESSIBLE["AFTER_FIRST_UNLOCK"] = "AccessibleAfterFirstUnlock";
  ACCESSIBLE["ALWAYS"] = "AccessibleAlways";
  ACCESSIBLE["WHEN_PASSCODE_SET_THIS_DEVICE_ONLY"] = "AccessibleWhenPasscodeSetThisDeviceOnly";
  ACCESSIBLE["WHEN_UNLOCKED_THIS_DEVICE_ONLY"] = "AccessibleWhenUnlockedThisDeviceOnly";
  ACCESSIBLE["AFTER_FIRST_UNLOCK_THIS_DEVICE_ONLY"] = "AccessibleAfterFirstUnlockThisDeviceOnly";
  return ACCESSIBLE;
}({});
/**
 * Enum representing access control options.
 */
let ACCESS_CONTROL = exports.ACCESS_CONTROL = /*#__PURE__*/function (ACCESS_CONTROL) {
  ACCESS_CONTROL["USER_PRESENCE"] = "UserPresence";
  ACCESS_CONTROL["BIOMETRY_ANY"] = "BiometryAny";
  ACCESS_CONTROL["BIOMETRY_CURRENT_SET"] = "BiometryCurrentSet";
  ACCESS_CONTROL["DEVICE_PASSCODE"] = "DevicePasscode";
  ACCESS_CONTROL["APPLICATION_PASSWORD"] = "ApplicationPassword";
  ACCESS_CONTROL["BIOMETRY_ANY_OR_DEVICE_PASSCODE"] = "BiometryAnyOrDevicePasscode";
  ACCESS_CONTROL["BIOMETRY_CURRENT_SET_OR_DEVICE_PASSCODE"] = "BiometryCurrentSetOrDevicePasscode";
  return ACCESS_CONTROL;
}({});
/**
 * Enum representing authentication types.
 */
let AUTHENTICATION_TYPE = exports.AUTHENTICATION_TYPE = /*#__PURE__*/function (AUTHENTICATION_TYPE) {
  AUTHENTICATION_TYPE["DEVICE_PASSCODE_OR_BIOMETRICS"] = "AuthenticationWithBiometricsDevicePasscode";
  AUTHENTICATION_TYPE["BIOMETRICS"] = "AuthenticationWithBiometrics";
  return AUTHENTICATION_TYPE;
}({});
/**
 * Enum representing security levels.
 * @platform Android
 */
let SECURITY_LEVEL = exports.SECURITY_LEVEL = function (SECURITY_LEVEL) {
  SECURITY_LEVEL[SECURITY_LEVEL["SECURE_SOFTWARE"] = RNKeychainManager && RNKeychainManager.SECURITY_LEVEL_SECURE_SOFTWARE] = "SECURE_SOFTWARE";
  SECURITY_LEVEL[SECURITY_LEVEL["SECURE_HARDWARE"] = RNKeychainManager && RNKeychainManager.SECURITY_LEVEL_SECURE_HARDWARE] = "SECURE_HARDWARE";
  SECURITY_LEVEL[SECURITY_LEVEL["ANY"] = RNKeychainManager && RNKeychainManager.SECURITY_LEVEL_ANY] = "ANY";
  return SECURITY_LEVEL;
}({});
/**
 * Enum representing types of biometric authentication supported by the device.
 */
let BIOMETRY_TYPE = exports.BIOMETRY_TYPE = /*#__PURE__*/function (BIOMETRY_TYPE) {
  BIOMETRY_TYPE["TOUCH_ID"] = "TouchID";
  BIOMETRY_TYPE["FACE_ID"] = "FaceID";
  BIOMETRY_TYPE["OPTIC_ID"] = "OpticID";
  BIOMETRY_TYPE["FINGERPRINT"] = "Fingerprint";
  BIOMETRY_TYPE["FACE"] = "Face";
  BIOMETRY_TYPE["IRIS"] = "Iris";
  return BIOMETRY_TYPE;
}({});
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
let STORAGE_TYPE = exports.STORAGE_TYPE = /*#__PURE__*/function (STORAGE_TYPE) {
  STORAGE_TYPE["AES_CBC"] = "KeystoreAESCBC";
  STORAGE_TYPE["AES_GCM_NO_AUTH"] = "KeystoreAESGCM_NoAuth";
  STORAGE_TYPE["AES_GCM"] = "KeystoreAESGCM";
  STORAGE_TYPE["RSA"] = "KeystoreRSAECB";
  return STORAGE_TYPE;
}({});
/**
 * Enum representing keychain error codes for reliable, language-independent error handling.
 * These codes are returned in the `error.code` property when keychain operations fail.
 *
 * Use these constants instead of parsing error messages for reliable error detection.
 */
let ERROR_CODE = exports.ERROR_CODE = /*#__PURE__*/function (ERROR_CODE) {
  ERROR_CODE["PASSCODE_NOT_SET"] = "E_PASSCODE_NOT_SET";
  ERROR_CODE["BIOMETRIC_NOT_ENROLLED"] = "E_BIOMETRIC_NOT_ENROLLED";
  ERROR_CODE["BIOMETRIC_TIMEOUT"] = "E_BIOMETRIC_TIMEOUT";
  ERROR_CODE["BIOMETRIC_LOCKOUT"] = "E_BIOMETRIC_LOCKOUT";
  ERROR_CODE["BIOMETRIC_LOCKOUT_PERMANENT"] = "E_BIOMETRIC_LOCKOUT_PERMANENT";
  ERROR_CODE["BIOMETRIC_TEMPORARILY_UNAVAILABLE"] = "E_BIOMETRIC_TEMPORARILY_UNAVAILABLE";
  ERROR_CODE["BIOMETRIC_UNAVAILABLE"] = "E_BIOMETRIC_UNAVAILABLE";
  ERROR_CODE["BIOMETRIC_VENDOR_ERROR"] = "E_BIOMETRIC_VENDOR_ERROR";
  ERROR_CODE["AUTH_INTERACTION_NOT_ALLOWED"] = "E_AUTH_INTERACTION_NOT_ALLOWED";
  ERROR_CODE["AUTH_INVALIDATED"] = "E_AUTH_INVALIDATED";
  ERROR_CODE["AUTH_CANCELED"] = "E_AUTH_CANCELED";
  ERROR_CODE["AUTH_ERROR"] = "E_AUTH_ERROR";
  ERROR_CODE["INVALID_PARAMETERS"] = "E_INVALID_PARAMETERS";
  ERROR_CODE["STORAGE_ACCESS_ERROR"] = "E_STORAGE_ACCESS_ERROR";
  ERROR_CODE["INTERNAL_ERROR"] = "E_INTERNAL_ERROR";
  return ERROR_CODE;
}({});
//# sourceMappingURL=enums.js.map