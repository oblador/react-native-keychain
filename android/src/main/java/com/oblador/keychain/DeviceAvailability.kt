package com.oblador.keychain

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricManager

/**
 * @see
 *   [Biometric hardware](https://stackoverflow.com/questions/50968732/determine-if-biometric-hardware-is-present-and-the-user-has-enrolled-biometrics)
 */
@Suppress("deprecation")
object DeviceAvailability {
  fun isStrongBiometricAuthAvailable(context: Context): Boolean {
    return BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
        BiometricManager.BIOMETRIC_SUCCESS
  }

  fun isFingerprintAuthAvailable(context: Context): Boolean {
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
  }

  fun isFaceAuthAvailable(context: Context): Boolean {
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)
  }

  fun isIrisAuthAvailable(context: Context): Boolean {
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_IRIS)
  }

  /** Check is permissions granted for biometric things. */
  @JvmStatic
  fun isPermissionsGranted(context: Context): Boolean {
    // before api23 no permissions for biometric, no hardware == no permissions
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return false
    }
    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    if (!km.isKeyguardSecure) return false

    // api28+
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      context.checkSelfPermission(Manifest.permission.USE_BIOMETRIC) ==
          PackageManager.PERMISSION_GRANTED
    } else
        context.checkSelfPermission(Manifest.permission.USE_FINGERPRINT) ==
            PackageManager.PERMISSION_GRANTED

    // before api28
  }
}
