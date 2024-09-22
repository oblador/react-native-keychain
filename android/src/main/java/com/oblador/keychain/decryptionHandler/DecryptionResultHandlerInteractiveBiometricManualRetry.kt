package com.oblador.keychain.decryptionHandler

import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import androidx.biometric.BiometricPrompt
import com.facebook.react.bridge.ReactApplicationContext
import com.oblador.keychain.cipherStorage.CipherStorage

class DecryptionResultHandlerInteractiveBiometricManualRetry(
    @NonNull reactContext: ReactApplicationContext,
    @NonNull storage: CipherStorage,
    @NonNull promptInfo: BiometricPrompt.PromptInfo
) :
    DecryptionResultHandlerInteractiveBiometric(reactContext, storage, promptInfo),
    DecryptionResultHandler {

  // Explicitly declare visibility and use 'override' to match the interface
  override var result: CipherStorage.DecryptionResult? = null
  override var error: Throwable? = null

  private var presentedPrompt: BiometricPrompt? = null
  private var didFailBiometric: Boolean = false

  /** Manually cancel current (invisible) authentication to clear the fragment. */
  private fun cancelPresentedAuthentication() {
    Log.d(LOG_TAG, "Cancelling authentication")
    if (presentedPrompt == null) {
      return
    }

    try {
      presentedPrompt?.cancelAuthentication()
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      this.presentedPrompt = null
    }
  }

  /** Called when an unrecoverable error has been encountered and the operation is complete. */
  override fun onAuthenticationError(errorCode: Int, @NonNull errString: CharSequence) {
    if (didFailBiometric) {
      this.presentedPrompt = null
      this.didFailBiometric = false
      retryAuthentication()
      return
    }

    super.onAuthenticationError(errorCode, errString)
  }

  /**
   * Called when a biometric (e.g. fingerprint, face, etc.) is presented but not recognized as
   * belonging to the user.
   */
  override fun onAuthenticationFailed() {
    Log.d(LOG_TAG, "Authentication failed: biometric not recognized.")
    if (presentedPrompt != null) {
      this.didFailBiometric = true
      cancelPresentedAuthentication()
    }
  }

  /** Called when a biometric is recognized. */
  override fun onAuthenticationSucceeded(@NonNull result: BiometricPrompt.AuthenticationResult) {
    this.presentedPrompt = null
    this.didFailBiometric = false
    super.onAuthenticationSucceeded(result)
  }

  /** Trigger interactive authentication. */
  override fun startAuthentication() {
    val activity = getCurrentActivity()

    // Code can be executed only from MAIN thread
    if (Thread.currentThread() != Looper.getMainLooper().thread) {
      activity.runOnUiThread { startAuthentication() }
      waitResult()
      return
    }

    this.presentedPrompt = authenticateWithPrompt(activity)
  }

  /** Trigger interactive authentication without invoking another waitResult() */
  protected fun retryAuthentication() {
    Log.d(LOG_TAG, "Retrying biometric authentication.")

    val activity = getCurrentActivity()

    if (Thread.currentThread() != Looper.getMainLooper().thread) {
      try {
        /*
         * NOTE: Applications should not cancel and authenticate in a short succession
         * Waiting 100ms in a non-UI thread to make sure previous BiometricPrompt is cleared by OS
         */
        Thread.sleep(100)
      } catch (ignored: InterruptedException) {
        // Shutdown sequence
      }

      activity.runOnUiThread { retryAuthentication() }
      return
    }

    this.presentedPrompt = authenticateWithPrompt(activity)
  }
}
