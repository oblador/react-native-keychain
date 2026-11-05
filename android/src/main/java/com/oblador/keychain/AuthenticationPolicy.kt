package com.oblador.keychain

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import com.oblador.keychain.KeychainModule.AccessControl
import com.oblador.keychain.KeychainModule.Errors
import com.oblador.keychain.exceptions.KeychainException

internal object AuthenticationPolicy {
  private fun requested(accessControl: String?): Int? = when (accessControl) {
    AccessControl.BIOMETRY_ANY, AccessControl.BIOMETRY_CURRENT_SET -> BIOMETRIC_STRONG
    AccessControl.DEVICE_PASSCODE -> DEVICE_CREDENTIAL
    AccessControl.BIOMETRY_ANY_OR_DEVICE_PASSCODE,
    AccessControl.BIOMETRY_CURRENT_SET_OR_DEVICE_PASSCODE -> BIOMETRIC_STRONG or DEVICE_CREDENTIAL
    else -> null
  }

  // Older entries do not record a prompt policy. Authenticated keys created by
  // this library allow both authenticators on API 30+. Earlier versions cannot
  // express that combination consistently, so retain strong biometric prompts.
  private fun legacy(sdk: Int): Int =
    if (sdk >= 30) BIOMETRIC_STRONG or DEVICE_CREDENTIAL else BIOMETRIC_STRONG

  fun forWrite(accessControl: String?, requiresAuth: Boolean, sdk: Int): Int? =
    if (requiresAuth) requested(accessControl) ?: legacy(sdk) else null

  fun forRead(accessControl: String?, stored: Int?, requiresAuth: Boolean, sdk: Int): Int? {
    if (!requiresAuth) return null
    val allowed = stored ?: legacy(sdk)
    val requested = requested(accessControl) ?: return allowed
    // A read may narrow a saved policy, but cannot silently broaden it.
    val intersection = allowed and requested
    if (intersection == 0) {
      throw KeychainException(
        "Requested authentication does not match the stored credential policy",
        Errors.E_INVALID_PARAMETERS
      )
    }
    return intersection
  }
}
