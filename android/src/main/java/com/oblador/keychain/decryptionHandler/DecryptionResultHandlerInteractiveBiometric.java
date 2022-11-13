package com.oblador.keychain.decryptionHandler;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import com.facebook.react.bridge.ReactApplicationContext;
import com.oblador.keychain.DeviceAvailability;
import com.oblador.keychain.cipherStorage.CipherStorage;
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionContext;
import com.oblador.keychain.cipherStorage.CipherStorageBase;
import com.oblador.keychain.exceptions.CryptoFailedException;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DecryptionResultHandlerInteractiveBiometric extends BiometricPrompt.AuthenticationCallback implements DecryptionResultHandler {
  protected final ReactApplicationContext reactContext;
  protected final CipherStorageBase storage;
  protected final BiometricPrompt.PromptInfo promptInfo;
  protected final Handler handler = new Handler(Looper.myLooper());
  protected final Executor executor = Executors.newSingleThreadExecutor();
  protected CipherStorage.DecryptionContext context;
  protected DecryptionResultListener listener;

  /** Logging tag. */
  protected static final String LOG_TAG = DecryptionResultHandlerInteractiveBiometric.class.getSimpleName();

  public DecryptionResultHandlerInteractiveBiometric(
                                                     @NonNull ReactApplicationContext reactContext,
                                                     @NonNull final CipherStorage storage,
                                                     @NonNull final BiometricPrompt.PromptInfo promptInfo) {
    this.reactContext = reactContext;
    this.storage = (CipherStorageBase) storage;
    this.promptInfo = promptInfo;
  }

  @Override
  public void askAccessPermissions(@NonNull final DecryptionContext context,
                                   @NonNull final DecryptionResultListener listener) {
    this.context = context;
    this.listener = listener;

    if (!DeviceAvailability.isPermissionsGranted(reactContext)) {
      final CryptoFailedException failure = new CryptoFailedException(
        "Could not start fingerprint Authentication. No permissions granted.");

      listener.onError(failure);
    } else {
      startAuthentication();
    }
  }

  /** Called when an unrecoverable error has been encountered and the operation is complete. */
  @Override
  public void onAuthenticationError(final int errorCode, @NonNull final CharSequence errString) {
    handler.post(
        () -> {
          final CryptoFailedException error =
              new CryptoFailedException("code: " + errorCode + ", msg: " + errString);
          listener.onError(error);
        });
  }

  /** Called when a biometric is recognized. */
  @Override
  public void onAuthenticationSucceeded(
      @NonNull final BiometricPrompt.AuthenticationResult result) {
    handler.post(
        () -> {
          try {
            if (null == context)
              throw new NullPointerException("Decrypt context is not assigned yet.");

            final CipherStorage.DecryptionResult decrypted =
                new CipherStorage.DecryptionResult(
                    storage.decryptBytes(context.key, context.username),
                    storage.decryptBytes(context.key, context.password));

            listener.onDecrypt(decrypted);
          } catch (Throwable fail) {
            listener.onError(fail);
          }
        });
  }

  /** trigger interactive authentication. */
  public void startAuthentication() {
    FragmentActivity activity = getCurrentActivity();

    // code can be executed only from MAIN thread
    if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
      activity.runOnUiThread(this::startAuthentication);
      return;
    }

    authenticateWithPrompt(activity);
  }

  protected FragmentActivity getCurrentActivity() {
    final FragmentActivity activity = (FragmentActivity) reactContext.getCurrentActivity();
    if (null == activity) throw new NullPointerException("Not assigned current activity");

    return activity;
  }

  protected BiometricPrompt authenticateWithPrompt(@NonNull final FragmentActivity activity) {
    final BiometricPrompt prompt = new BiometricPrompt(activity, executor, this);
    prompt.authenticate(this.promptInfo);

    return prompt;
  }
}
