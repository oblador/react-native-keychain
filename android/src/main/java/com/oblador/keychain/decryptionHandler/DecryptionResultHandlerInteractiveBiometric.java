package com.oblador.keychain.decryptionHandler;

import android.os.Looper;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.facebook.react.bridge.AssertionException;
import com.facebook.react.bridge.ReactApplicationContext;
import com.oblador.keychain.DeviceAvailability;
import com.oblador.keychain.cipherStorage.CipherStorage;
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionResult;
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionContext;
import com.oblador.keychain.cipherStorage.CipherStorageBase;
import com.oblador.keychain.exceptions.CryptoFailedException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class DecryptionResultHandlerInteractiveBiometric implements DecryptionResultHandler {
  protected final ReactApplicationContext reactContext;
  protected final CipherStorageBase storage;
  protected final Executor executor;

  protected DecryptionResultJob job;

  protected BiometricPrompt.PromptInfo promptInfo;
  protected BiometricPrompt prompt;
  protected InteractiveBiometryHandlers handlers;

  protected final AuthSerialQueue authQueue = new AuthSerialQueue();

  /** Logging tag. */
  protected static final String LOG_TAG = DecryptionResultHandlerInteractiveBiometric.class.getSimpleName();

  public DecryptionResultHandlerInteractiveBiometric(@NonNull ReactApplicationContext reactContext,
                                                     @NonNull final CipherStorage storage,
                                                     @NonNull final BiometricPrompt.PromptInfo promptInfo) {
    this.reactContext = reactContext;
    this.storage = (CipherStorageBase) storage;
    this.promptInfo = promptInfo;

    executor = ContextCompat.getMainExecutor(reactContext);
    handlers = provideHandlers();
    prompt = new BiometricPrompt(getCurrentActivity(), executor, handlers);
  }

  protected InteractiveBiometryHandlers provideHandlers() {
    return new InteractiveBiometryHandlers();
  }

  public CompletableFuture<DecryptionResult> authenticate(@NonNull DecryptionResultJob job) {
    return authQueue.add(asyncResult -> {
      try {
        asyncResult.complete(job.get());
      } catch (final UserNotAuthenticatedException ex) {
        Log.d(LOG_TAG, "Unlock of keystore is needed. Error: " + ex.getMessage(), ex);

        // Since the queue is serial I think it's OK to reassign it here
        this.job = job;

        handlers.setOnAuth(() -> {
          try {
            asyncResult.complete(job.get());
          } catch (final Throwable fail) {
            // any other exception treated as a failure
            asyncResult.completeExceptionally(fail);
          }
        }).setOnError(asyncResult::completeExceptionally);

        askAccessPermissions(asyncResult);
      } catch (final Throwable fail) {
        // any other exception treated as a failure
        asyncResult.completeExceptionally(fail);
      }
    });
  }

  protected void askAccessPermissions(CompletableFuture<DecryptionResult> asyncResult) {
    if (!DeviceAvailability.isPermissionsGranted(reactContext)) {
      final CryptoFailedException failure = new CryptoFailedException(
              "Could not start fingerprint Authentication. No permissions granted.");

      asyncResult.completeExceptionally(failure);
    } else {
      startAuthentication();
    }
  }

  /** trigger interactive authentication. */
  protected void startAuthentication() {
    FragmentActivity activity = getCurrentActivity();

    // code can be executed only from MAIN thread
    if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
      activity.runOnUiThread(this::startAuthentication);
      return;
    }

    prompt.authenticate(promptInfo);
  }

  protected FragmentActivity getCurrentActivity() {
    final FragmentActivity activity = (FragmentActivity) reactContext.getCurrentActivity();
    if (null == activity) throw new NullPointerException("Not assigned current activity");

    return activity;
  }

  static class InteractiveBiometryHandlers extends BiometricPrompt.AuthenticationCallback {
    private Runnable cb;
    private Consumer<Throwable> errCb;

    public InteractiveBiometryHandlers setOnAuth(Runnable cb) {
      this.cb = cb;
      return this;
    }

    public InteractiveBiometryHandlers setOnError(Consumer<Throwable> errCb) {
      this.errCb = errCb;
      return this;
    }

    /** Called when an unrecoverable error has been encountered and the operation is complete. */
    @Override
    public void onAuthenticationError(final int errorCode, @NonNull final CharSequence errString) {
      super.onAuthenticationError(errorCode, errString);

      final CryptoFailedException error = new CryptoFailedException("code: " + errorCode + ", msg: " + errString);

      this.errCb.accept(error);
    }

    /** Called when a biometric is recognized. */
    @Override
    public void onAuthenticationSucceeded(@NonNull final BiometricPrompt.AuthenticationResult result) {
      super.onAuthenticationSucceeded(result);

      this.cb.run();
    }
  }
}
