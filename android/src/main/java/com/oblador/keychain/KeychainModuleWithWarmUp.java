package com.oblador.keychain;

import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.oblador.keychain.cipherStorage.CipherStorageBase;
import com.oblador.keychain.workaround.IDeviceFilter;

import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;

public class KeychainModuleWithWarmUp extends KeychainModule {
    /**
     * Default constructor.
     *
     * @param reactContext
     * @param deviceFilter
     */
    public KeychainModuleWithWarmUp(@NonNull ReactApplicationContext reactContext, IDeviceFilter deviceFilter) {
      super(reactContext, deviceFilter);

      // force initialization of the crypto api in background thread
      final Thread warmingUp = new Thread(this::internalWarmingBestCipher, "keychain-warming-up");
      warmingUp.setDaemon(true);
      warmingUp.start();
    }

  /** cipher (crypto api) warming up logic. force java load classes and intializations. */
  private void internalWarmingBestCipher() {
    final long startTime = System.nanoTime();

    try {
      Log.v(KEYCHAIN_MODULE, "warming up started at " + startTime);
      final CipherStorageBase best = (CipherStorageBase) getCipherStorageForCurrentAPILevel();
      final Cipher instance = best.getCachedInstance();
      final boolean isSecure = best.supportsSecureHardware();
      final SecurityLevel requiredLevel = isSecure ? SecurityLevel.SECURE_HARDWARE : SecurityLevel.SECURE_SOFTWARE;
      best.generateKeyAndStoreUnderAlias("warmingUp", requiredLevel);
      best.getKeyStoreAndLoad();
    } catch (Throwable ex) {
      Log.e(KEYCHAIN_MODULE, "warming up failed!", ex);
    }

    Log.v(KEYCHAIN_MODULE, "warming up takes: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) + " ms");
  }
}
