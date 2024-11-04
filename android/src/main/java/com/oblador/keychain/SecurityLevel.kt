package com.oblador.keychain

/** Minimal required level of the security implementation. */
enum class SecurityLevel {
  /**
   * No security guarantees needed (default value); Credentials can be stored in FB Secure Storage
   */
  ANY,

  /**
   * Requires for the key to be stored in the Android Keystore, separate from the encrypted data.
   */
  SECURE_SOFTWARE,

  /**
   * Requires for the key to be stored on a secure hardware (Trusted Execution Environment or Secure
   * Environment).
   */
  SECURE_HARDWARE;

  /** Get JavaScript friendly name. */
  fun jsName(): String {
    return String.format("SECURITY_LEVEL_%s", name)
  }

  fun satisfiesSafetyThreshold(threshold: SecurityLevel): Boolean {
    return this.compareTo(threshold) >= 0
  }
}
