package com.oblador.keychain.decryptionHandler;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;

import com.facebook.react.bridge.ReactApplicationContext;
import com.oblador.keychain.cipherStorage.CipherStorage;

import java.util.Arrays;

// NOTE: the logic for handling OnePlus bug is taken from the following forum post:
// https://forums.oneplus.com/threads/oneplus-7-pro-fingerprint-biometricprompt-does-not-show.1035821/#post-21710422
public class DecryptionResultHandlerProvider {
  private static final String ONE_PLUS_BRAND = "oneplus";
  private static final String[] ONE_PLUS_MODELS_WITHOUT_BIOMETRIC_BUG = {
    "A0001", // OnePlus One
    "ONE A2001", "ONE A2003", "ONE A2005", // OnePlus 2
    "ONE E1001", "ONE E1003", "ONE E1005", // OnePlus X
    "ONEPLUS A3000", "ONEPLUS SM-A3000", "ONEPLUS A3003", // OnePlus 3
    "ONEPLUS A3010", // OnePlus 3T
    "ONEPLUS A5000", // OnePlus 5
    "ONEPLUS A5010", // OnePlus 5T
    "ONEPLUS A6000", "ONEPLUS A6003" // OnePlus 6
  };

  private static boolean hasOnePlusBiometricBug() {
    return Build.BRAND.toLowerCase().equals(ONE_PLUS_BRAND) &&
      !Arrays.asList(ONE_PLUS_MODELS_WITHOUT_BIOMETRIC_BUG).contains(Build.MODEL);
  }

  public static DecryptionResultHandler getHandler(@NonNull ReactApplicationContext reactContext,
                                                   @NonNull final CipherStorage storage,
                                                   @NonNull final BiometricPrompt.PromptInfo promptInfo) {
    if (storage.isBiometrySupported()) {
      if (hasOnePlusBiometricBug()) {
        return new DecryptionResultHandlerInteractiveBiometricManualRetry(reactContext, storage, promptInfo);
      }

      return new DecryptionResultHandlerInteractiveBiometric(reactContext, storage, promptInfo);
    }

    return new DecryptionResultHandlerNonInteractive();
  }
}
