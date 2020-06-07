package com.oblador.keychain;

import android.content.Context;

import androidx.annotation.NonNull;


/**
 * This helper provides access to {@link DeviceAvailability} methods via instance rather than via static calls,
 * this way enabling dependency injection and mocking.
 *
 * Prefer using this helper; direct use of {@link DeviceAvailability} is discouraged.
 */
public class BiometricCapabilitiesHelper {
  public static final String FINGERPRINT_SUPPORTED_NAME = "Fingerprint";
  public static final String FACE_SUPPORTED_NAME = "Face";
  public static final String IRIS_SUPPORTED_NAME = "Iris";

  private final Context context;

  /**
   * Constructor.
   *
   * @param context Android context
   */
  public BiometricCapabilitiesHelper(@NonNull final Context context) {
    this.context = context;
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
