package com.oblador.keychain;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.content.Context;
import android.app.KeyguardManager;
import android.hardware.fingerprint.FingerprintManager;
import android.support.v4.app.ActivityCompat;

public class DeviceAvailability {
    public static boolean isFingerprintAuthAvailable(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            FingerprintManager fingerprintManager =
                (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
            return fingerprintManager != null && fingerprintManager.isHardwareDetected() &&
                fingerprintManager.hasEnrolledFingerprints() &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    public static boolean isDeviceSecure(Context context) {
        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return Build.VERSION.SDK_INT >= 23 && keyguardManager != null && keyguardManager.isDeviceSecure();
    }
}
