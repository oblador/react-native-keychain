package com.oblador.keychain;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;



/**
 * This helper provides access to {@link DeviceAvailability} methods via instance rather than via static calls,
 * this way enabling dependency injection and mocking.
 *
 * Prefer using this helper; direct use of {@link DeviceAvailability} is discouraged.
 */
public class BiometricCapabilitiesHelper {

  /**
   * Listener for the event when capabilities change.
   */
  public interface CapabilitiesChangeListener {
    /**
     * This callback is called when the biometric capabilities are changed (i.e. fingerprint HW is enabled, etc.).
     *
     * @param helper instance of helper
     */
    void onBiometricCapabilitiesChanged(@NonNull final BiometricCapabilitiesHelper helper);
  }

  public static final String FINGERPRINT_SUPPORTED_NAME = "Fingerprint";
  public static final String FACE_SUPPORTED_NAME = "Face";
  public static final String IRIS_SUPPORTED_NAME = "Iris";

  protected final Context context;

  private CapabilitiesChangeListener listener;

  /**
   * Constructor.
   *
   * @param context Android context
   */
  public BiometricCapabilitiesHelper(@NonNull final Context context) {
    this.context = context;
  }

  /**
   * Set or remove capabilities change listener.
   *
   * @param listener the listener
   */
  public void setCapabilitiesChangeListener(@Nullable final CapabilitiesChangeListener listener) {
    this.listener = listener;
  }

  @VisibleForTesting
  /* package */ void notifyCapabilitiesChanged() {
    if (listener != null) {
      listener.onBiometricCapabilitiesChanged(this);
    }
  }

  /**
   * Check if fingerprint auth possible.
   *
   * @return {@code true} when fingerprint hardware available and configured, otherwise {@code false}.
   */
  public boolean isFingerprintAuthAvailable() {
    return DeviceAvailability.isBiometricAuthAvailable(context) && DeviceAvailability.isFingerprintAuthAvailable(context);
  }

  /**
   * Check if fingerprint auth possible.
   *
   * @return {@code true} when face recognition hardware available and configured, otherwise {@code false}.
   */
  public boolean isFaceAuthAvailable() {
    return DeviceAvailability.isBiometricAuthAvailable(context) && DeviceAvailability.isFaceAuthAvailable(context);
  }

  /**
   * Check if fingerprint auth possible.
   *
   * @return {@code true} when iris recognition hardware available and configured, otherwise {@code false}.
   */
  public boolean isIrisAuthAvailable() {
    return DeviceAvailability.isBiometricAuthAvailable(context) && DeviceAvailability.isIrisAuthAvailable(context);
  }

  /**
   * Check if one or more ways of biometric auth possible.
   *
   * @return {@code true} when at least one way of biometric auth is possible, otherwise {@code false}.
   */
  public boolean isAnyBiometryAvailable() {
    return isFingerprintAuthAvailable() || isFaceAuthAvailable() || isIrisAuthAvailable();
  }

  public String getSupportedBiometryType() {
    String reply = null;
    if(isFaceAuthAvailable()){
      reply = FACE_SUPPORTED_NAME;
    } else if(isIrisAuthAvailable()) {
      reply = IRIS_SUPPORTED_NAME;
    } else if(isFingerprintAuthAvailable()) {
      reply = FINGERPRINT_SUPPORTED_NAME;
    }
    return reply;
  }
}
