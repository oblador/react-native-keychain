package com.oblador.keychain;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricManager;

import static android.content.Context.FINGERPRINT_SERVICE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS;

/**
 * @see <a href="https://stackoverflow.com/questions/50968732/determine-if-biometric-hardware-is-present-and-the-user-has-enrolled-biometrics">Biometric hradware</a>
 */
@SuppressWarnings({"WeakerAccess", "deprecation"})
public class DeviceAvailability {
  public static boolean isBiometricAuthAvailable(@NonNull final Context context) {
    return BiometricManager.from(context).canAuthenticate() == BIOMETRIC_SUCCESS;
  }

  public static boolean isFingerprintAuthAvailable(@NonNull final Context context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return false;
    } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
      // -----------------------------------------------------------------------------------------------
      // WARNING: hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) cannot be reliably used on API23.
      // -----------------------------------------------------------------------------------------------
      return isFingerprintHardwareDetected_Api23_Workaround(context);
    } else {
      return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
    }
  }

  /**
   * On API23, hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) will return false on *some* devices,
   * even though the fingerprint HW is present and usable.
   *
   * This Google Issue Tracker ticket https://issuetracker.google.com/issues/124066957 reported that this check was
   * problematic and they had to remove it in the support lib:
   *
   *   | mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) which can return false,
   *   | even if scanner exists and fingerprint is configured, and FingerprintManager can be used for authentication. ...
   *   | We need to update FingerprintManagerCompat which also checks PackageManager.
   *   | The flag exists on API23, but OEMs were not setting it properly. I believe we started enforcing this after 23.
   *
   * @deprecated TODO Remove this method once API23 support is phased out.
   */
  @RequiresApi(Build.VERSION_CODES.M)
  @Deprecated
  private static boolean isFingerprintHardwareDetected_Api23_Workaround(@NonNull final Context context) {
    final FingerprintManager manager = (FingerprintManager) context.getSystemService(FINGERPRINT_SERVICE);
    return (manager != null) && manager.isHardwareDetected();
  }

  @TargetApi(Build.VERSION_CODES.Q)
  public static boolean isFaceAuthAvailable(@NonNull final Context context) {
    return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FACE);
  }

    @TargetApi(Build.VERSION_CODES.Q)
    public static boolean isIrisAuthAvailable(@NonNull final Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_IRIS);
    }

  /** Check is permissions granted for biometric things. */
  public static boolean isPermissionsGranted(@NonNull final Context context) {
    // before api23 no permissions for biometric, no hardware == no permissions
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return false;
    }

    final KeyguardManager km =
      (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
    if( !km.isKeyguardSecure() ) return false;

    // api28+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      return context.checkSelfPermission(Manifest.permission.USE_BIOMETRIC) == PERMISSION_GRANTED;
    }

    // before api28
    return context.checkSelfPermission(Manifest.permission.USE_FINGERPRINT) == PERMISSION_GRANTED;
  }

  public static boolean isDeviceSecure(@NonNull final Context context) {
    final KeyguardManager km =
      (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
      km != null &&
      km.isDeviceSecure();
  }
}
