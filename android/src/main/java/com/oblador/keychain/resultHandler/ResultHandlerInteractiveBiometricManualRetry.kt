package com.oblador.keychain.resultHandler

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.biometric.BiometricPrompt
import com.facebook.react.bridge.ReactApplicationContext
import com.oblador.keychain.cipherStorage.CipherStorage

class ResultHandlerInteractiveBiometricManualRetry(
  reactContext: ReactApplicationContext,
  storage: CipherStorage,
  promptInfo: BiometricPrompt.PromptInfo
) :
  ResultHandlerInteractiveBiometric(reactContext, storage, promptInfo) {
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
  override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
    if (isComplete) return
    if (didFailBiometric && errorCode == BiometricPrompt.ERROR_CANCELED) {
      this.presentedPrompt = null
      this.didFailBiometric = false
      retryAuthentication()
      return
    }

    didFailBiometric = false
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
  override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
    this.presentedPrompt = null
    this.didFailBiometric = false
    super.onAuthenticationSucceeded(result)
  }

  /** Trigger interactive authentication without invoking another waitResult() */
  protected fun retryAuthentication() {
    Log.d(LOG_TAG, "Retrying biometric authentication.")

    // Let the previous prompt clear without sleeping on the callback executor.
    Handler(Looper.getMainLooper()).postDelayed({ startAuthentication() }, 100)
  }
}
