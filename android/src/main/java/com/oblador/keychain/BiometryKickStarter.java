package com.oblador.keychain;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

/**
 * Start the biometric subsystem in a background thread with a given timeout.
 * If once the timeout elapsed the biometry is not yet ready, it is reported as temporary unavailable.
 * The initialization continues in the background; once it completes, the biometry status changes to the actual status
 * (i.e. available or unavailable, as reported by the hardware).
 */
public class BiometryKickStarter {
  @NonNull
  private final InhibitableBiometricCapabilitiesHelper helper;

  @VisibleForTesting
  /*package*/ Thread thread;

  public BiometryKickStarter(@NonNull final InhibitableBiometricCapabilitiesHelper helper) {
    this.helper = helper;
  }

  /**
   * Start the biometric subsystem.
   *
   * This call shall block for maximum of #timeoutMillis milliseconds, or less if the
   * biometry initialized quickly, and return. In case the biometry didn't initialize by the moment of return, the
   * initialization continues in the background.
   *
   * Communication to the biometric subsystem is inhibited until the initialization completes.
   *
   * @param timeoutMillis timeout for the initialization attempt
   */
  public void tryStartBiometry(final int timeoutMillis) {
    final Object mutex = new Object();

    thread = new Thread("biometry-warming-up") {
      @Override
      public void run() {
        helper.initialize();
        synchronized (mutex) {
          helper.setInhibited(false);
          helper.notifyCapabilitiesChanged();
        }
      }
    };

    // Start the initialization process in bg and wait for the given timeout
    thread.setDaemon(true);
    thread.start();
    try {
      thread.join(timeoutMillis);
    } catch (final InterruptedException ignored) {
      /* ignore */
    }

    // Check if it was able to initialize in time
    synchronized (mutex) {
      if (thread.isAlive()) {
        // The initialization is still in progress.
        // Inhibit the helper and let the initialization continue in bg.
        helper.setInhibited(true);
      }
    }
  }
}
