package com.oblador.keychain.decryptionHandler

import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.facebook.react.bridge.AssertionException
import com.facebook.react.bridge.ReactApplicationContext
import com.oblador.keychain.DeviceAvailability
import com.oblador.keychain.cipherStorage.CipherStorage
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionContext
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionResult
import com.oblador.keychain.cipherStorage.CipherStorageBase
import com.oblador.keychain.exceptions.CryptoFailedException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

open class DecryptionResultHandlerInteractiveBiometric(
    @NonNull protected val reactContext: ReactApplicationContext,
    @NonNull storage: CipherStorage,
    @NonNull protected var promptInfo: BiometricPrompt.PromptInfo
) : BiometricPrompt.AuthenticationCallback(), DecryptionResultHandler {

  // Explicitly declare the visibility and use 'override' to match the interface
  override var result: DecryptionResult? = null
  override var error: Throwable? = null
  protected val storage: CipherStorageBase = storage as CipherStorageBase
  protected val executor: Executor = Executors.newSingleThreadExecutor()
  protected var context: DecryptionContext? = null

  // Synchronization primitives
  private val lock = ReentrantLock()
  private val condition = lock.newCondition()

  /** Logging tag. */
  protected val LOG_TAG = DecryptionResultHandlerInteractiveBiometric::class.java.simpleName

  override fun askAccessPermissions(@NonNull context: DecryptionContext) {
    this.context = context

    if (!DeviceAvailability.isPermissionsGranted(reactContext)) {
      val failure =
          CryptoFailedException(
              "Could not start fingerprint Authentication. No permissions granted.")
      onDecrypt(null, failure)
    } else {
      startAuthentication()
    }
  }

  override fun onDecrypt(
      @Nullable decryptionResult: DecryptionResult?,
      @Nullable error: Throwable?
  ) {
    lock.withLock {
      this.result = decryptionResult
      this.error = error
      condition.signalAll() // Notify waiting thread
    }
  }

  /** Called when an unrecoverable error has been encountered and the operation is complete. */
  override fun onAuthenticationError(errorCode: Int, @NonNull errString: CharSequence) {
    val error = CryptoFailedException("code: $errorCode, msg: $errString")
    onDecrypt(null, error)
  }

  /** Called when a biometric is recognized. */
  override fun onAuthenticationSucceeded(@NonNull result: BiometricPrompt.AuthenticationResult) {
    try {
      context ?: throw NullPointerException("Decrypt context is not assigned yet.")

      val decrypted =
          DecryptionResult(
              storage.decryptBytes(context!!.key, context!!.username),
              storage.decryptBytes(context!!.key, context!!.password))

      onDecrypt(decrypted, null)
    } catch (fail: Throwable) {
      onDecrypt(null, fail)
    }
  }

  /** Trigger interactive authentication. */
  open fun startAuthentication() {
    val activity = getCurrentActivity()

    // Code can be executed only from MAIN thread
    if (Thread.currentThread() != Looper.getMainLooper().thread) {
      activity.runOnUiThread { startAuthentication() }
      waitResult()
      return
    }

    authenticateWithPrompt(activity)
  }

  protected fun getCurrentActivity(): FragmentActivity {
    val activity = reactContext.currentActivity as? FragmentActivity
    return activity ?: throw NullPointerException("Not assigned current activity")
  }

  protected fun authenticateWithPrompt(@NonNull activity: FragmentActivity): BiometricPrompt {
    val prompt = BiometricPrompt(activity, executor, this)
    prompt.authenticate(this.promptInfo)
    return prompt
  }

  /** Block current NON-main thread and wait for user authentication results. */
  override fun waitResult() {
    if (Thread.currentThread() == Looper.getMainLooper().thread) {
      throw AssertionException("method should not be executed from MAIN thread")
    }

    Log.i(LOG_TAG, "blocking thread. waiting for done UI operation.")

    try {
      lock.withLock {
        condition.await() // Wait for signal
      }
    } catch (ignored: InterruptedException) {
      // Shutdown sequence
    }

    Log.i(LOG_TAG, "unblocking thread.")
  }
}
