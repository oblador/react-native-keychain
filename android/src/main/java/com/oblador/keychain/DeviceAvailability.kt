package com.oblador.keychain

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricManager
import java.util.Locale

/**
 * @see
 *   [Biometric hardware](https://stackoverflow.com/questions/50968732/determine-if-biometric-hardware-is-present-and-the-user-has-enrolled-biometrics)
 */
@Suppress("deprecation")
object DeviceAvailability {

  private val SLOW_STRONGBOX_MANUFACTURERS = setOf("motorola", "xiaomi")

  fun isStrongboxAvailable(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
    } else {
      false
    }
  }

  fun isSlowStrongBoxManufacturer(): Boolean {
      val manufacturer = (Build.MANUFACTURER ?: "").trim().lowercase(Locale.ROOT)
      val brand = (Build.BRAND ?: "").trim().lowercase(Locale.ROOT)
      return manufacturer in SLOW_STRONGBOX_MANUFACTURERS || brand in SLOW_STRONGBOX_MANUFACTURERS
  }

  fun isStrongBiometricAuthAvailable(context: Context): Boolean {
    return BiometricManager.from(context)
      .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
      BiometricManager.BIOMETRIC_SUCCESS
  }

  fun isDevicePasscodeAvailable(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) ==
        BiometricManager.BIOMETRIC_SUCCESS
    } else {
      false
    }
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
