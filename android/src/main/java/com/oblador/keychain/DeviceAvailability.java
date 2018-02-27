package com.oblador.keychain;

import android.os.Build;
import android.content.Context;
import android.app.KeyguardManager;
import android.hardware.fingerprint.FingerprintManager;

public class DeviceAvailability {
    public static boolean isFingerprintAuthAvailable(Context context) {
        FingerprintManager fingerprintManager =
                (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);

        return android.os.Build.VERSION.SDK_INT >= 23 &&
            fingerprintManager.isHardwareDetected() &&
            fingerprintManager.hasEnrolledFingerprints();
    }

    public static boolean isSecure(Context context) {
        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return android.os.Build.VERSION.SDK_INT >= 23 && keyguardManager.isDeviceSecure();
    }
}
