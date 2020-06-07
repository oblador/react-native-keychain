package com.oblador.keychain;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * This helper can be inhibited so it doesn't actually interact with the HW but returns {@code false}.
 * It can be used when the access to the biometric HW should be (temporary) prevented.
 *
 * By default, the access is allowed. Use {@link #setInhibited(boolean)} to change.
 */
public class InhibitableBiometricCapabilitiesHelper extends BiometricCapabilitiesHelper {
    private boolean inhibited = false;

    public InhibitableBiometricCapabilitiesHelper(@NonNull final Context context) {
        super(context);
    }

    /**
     * Inhibit or resume the access to actual biometric HW.
     * When inhibited, the helper will always return negative response for inquiries and do nothing.
     *
     * @param inhibited {@code true} to inhibit the access; {@code false} to resume.
     */
    public void setInhibited(final boolean inhibited) {
      this.inhibited = inhibited;
    }

    /**
     * Start interaction with the biometric HW.
     * This call always go through to the HW and does not depend on the inhibition status.
     */
    public void initialize() {
      // Kick-start the biometry manager. On some (rare) devices it may take up to 30 seconds.
      DeviceAvailability.isBiometricAuthAvailable(context);
    }

    @Override
    public boolean isFingerprintAuthAvailable() {
      if (inhibited) {
        return false;
      } else {
        return super.isFingerprintAuthAvailable();
      }
    }

    @Override
    public boolean isFaceAuthAvailable() {
      if (inhibited) {
        return false;
      } else {
        return super.isFingerprintAuthAvailable();
      }
    }

    @Override
    public boolean isIrisAuthAvailable() {
      if (inhibited) {
        return false;
      } else {
        return super.isFingerprintAuthAvailable();
      }
    }
}
