package com.oblador.keychain.decryptionHandler;

import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import com.facebook.react.bridge.ReactApplicationContext;
import com.oblador.keychain.cipherStorage.CipherStorage;

public class DecryptionResultHandlerInteractiveBiometricManualRetry extends DecryptionResultHandlerInteractiveBiometric implements DecryptionResultHandler {
  public DecryptionResultHandlerInteractiveBiometricManualRetry(@NonNull ReactApplicationContext reactContext,
                                                                @NonNull CipherStorage storage,
                                                                @NonNull BiometricPrompt.PromptInfo promptInfo) {
    super(reactContext, storage, promptInfo);
  }

  @Override
  protected InteractiveBiometryHandlers provideHandlers() {
    return new InteractiveManualRetryBiometryHandlers();
  }

  class InteractiveManualRetryBiometryHandlers extends DecryptionResultHandlerInteractiveBiometric.InteractiveBiometryHandlers {
    private Boolean didFailBiometric = false;

    /** Manually cancel current (invisible) authentication to clear the fragment. */
    private void cancelPresentedAuthentication() {
      Log.d(LOG_TAG, "Cancelling authentication");
      if (prompt == null) {
        return;
      }

      try {
        prompt.cancelAuthentication();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    /** Called when an unrecoverable error has been encountered and the operation is complete. */
    @Override
    public void onAuthenticationError(final int errorCode, @NonNull final CharSequence errString) {
      if (didFailBiometric) {
        this.didFailBiometric = false;
        retryAuthentication();
        return;
      }

      super.onAuthenticationError(errorCode, errString);
    }

    /** Called when a biometric (e.g. fingerprint, face, etc.) is presented but not recognized as belonging to the user. */
    @Override
    public void onAuthenticationFailed() {
      Log.d(LOG_TAG, "Authentication failed: biometric not recognized.");
      if (prompt != null) {
        this.didFailBiometric = true;
        cancelPresentedAuthentication();
      }
    }

    /** Called when a biometric is recognized. */
    @Override
    public void onAuthenticationSucceeded(@NonNull final BiometricPrompt.AuthenticationResult result) {
      this.didFailBiometric = false;

      super.onAuthenticationSucceeded(result);
    }
  }

  protected void retryAuthentication() {
    Log.d(LOG_TAG, "Retrying biometric authentication.");

    FragmentActivity activity = getCurrentActivity();

    if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
      try {
        /*
         * NOTE: Applications should not cancel and authenticate in a short succession
         * Waiting 100ms in a non-UI thread to make sure previous BiometricPrompt is cleared by OS
         */
        Thread.sleep(100);
      } catch (InterruptedException ignored) {
        /* shutdown sequence */
      }

      activity.runOnUiThread(this::retryAuthentication);
      return;
    }

    startAuthentication();
  }
}
