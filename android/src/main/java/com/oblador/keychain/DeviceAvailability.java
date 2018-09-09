package com.oblador.keychain;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.content.Context;
import android.app.KeyguardManager;
import android.hardware.fingerprint.FingerprintManager;

public class DeviceAvailability {
    public static boolean isFingerprintAuthAvailable(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            FingerprintManager fingerprintManager =
                (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
            return context.checkSelfPermission(Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED &&
                fingerprintManager.isHardwareDetected() &&
                fingerprintManager.hasEnrolledFingerprints();
        }
        return false;
    }

    public static boolean isSecure(Context context) {
        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return android.os.Build.VERSION.SDK_INT >= 23 && keyguardManager.isDeviceSecure();
    }
}
