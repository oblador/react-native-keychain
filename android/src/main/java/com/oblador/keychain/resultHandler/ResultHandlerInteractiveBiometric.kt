package com.oblador.keychain.resultHandler

import android.os.Looper
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.facebook.react.bridge.AssertionException
import com.facebook.react.bridge.ReactApplicationContext
import com.oblador.keychain.DeviceAvailability
import com.oblador.keychain.KeychainModule.Errors
import com.oblador.keychain.cipherStorage.CipherStorage
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionResult
import com.oblador.keychain.cipherStorage.CipherStorage.EncryptionResult
import com.oblador.keychain.cipherStorage.CipherStorageBase
import com.oblador.keychain.exceptions.KeychainException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

open class ResultHandlerInteractiveBiometric(
  protected val reactContext: ReactApplicationContext,
  storage: CipherStorage,
  protected var promptInfo: BiometricPrompt.PromptInfo
) : BiometricPrompt.AuthenticationCallback(), ResultHandler {

  // Explicitly declare the visibility and use 'override' to match the interface
  override var decryptionResult: DecryptionResult? = null
  override var encryptionResult: EncryptionResult? = null
  override var error: Throwable? = null
  protected val storage: CipherStorageBase = storage as CipherStorageBase
  protected val executor: Executor = Executors.newSingleThreadExecutor()
  protected var context: CryptoContext? = null

  // Synchronization primitives
  private val lock = ReentrantLock()
  private val condition = lock.newCondition()

  /** Logging tag. */
  protected val LOG_TAG = ResultHandlerInteractiveBiometric::class.java.simpleName

  override fun askAccessPermissions(context: CryptoContext) {
    this.context = context

    if (!DeviceAvailability.isPermissionsGranted(reactContext)) {
      val failure = KeychainException(
        "Could not start biometric Authentication. No permissions granted.",
        Errors.E_AUTH_PERMISSION_DENIED
      )
      when (context.operation) {
        CryptoOperation.ENCRYPT -> onEncrypt(null, failure)
        CryptoOperation.DECRYPT -> onDecrypt(null, failure)
      }
    } else {
      startAuthentication()
    }
  }

  override fun onEncrypt(encryptionResult: EncryptionResult?, error: Throwable?) {
    lock.withLock {
      this.encryptionResult = encryptionResult
      this.error = error
      condition.signalAll() // Notify waiting thread
    }
  }

  override fun onDecrypt(
    decryptionResult: DecryptionResult?, error: Throwable?
  ) {
    lock.withLock {
      this.decryptionResult = decryptionResult
      this.error = error
      condition.signalAll() // Notify waiting thread
    }
  }

  /** Called when an unrecoverable error has been encountered and the operation is complete. */
  override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
    val error = mapBiometricPromptError(errorCode, errString.toString())
    when (context?.operation) {
      CryptoOperation.ENCRYPT -> onEncrypt(null, error)
      CryptoOperation.DECRYPT -> onDecrypt(null, error)
      null -> Log.e(LOG_TAG, "No operation context available")
    }
  }

  /* Maps BiometricPrompt error codes to our custom error codes. */
  private fun mapBiometricPromptError(errorCode: Int, errorMessage: String): KeychainException {
    return when (errorCode) {
      BiometricPrompt.ERROR_CANCELED ->
        KeychainException(errorMessage, Errors.E_AUTH_CANCELED)

      BiometricPrompt.ERROR_USER_CANCELED ->
        KeychainException(errorMessage, Errors.E_AUTH_CANCELED)

      BiometricPrompt.ERROR_NO_BIOMETRICS ->
        KeychainException(errorMessage, Errors.E_BIOMETRIC_NOT_ENROLLED)

      BiometricPrompt.ERROR_TIMEOUT ->
        KeychainException(errorMessage, Errors.E_BIOMETRIC_TIMEOUT)

      BiometricPrompt.ERROR_HW_UNAVAILABLE ->
        KeychainException(errorMessage, Errors.E_BIOMETRIC_HARDWARE_UNAVAILABLE)

      BiometricPrompt.ERROR_LOCKOUT ->
        KeychainException(errorMessage, Errors.E_BIOMETRIC_LOCKOUT)

      BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
        KeychainException(errorMessage, Errors.E_BIOMETRIC_LOCKOUT_PERMANENT)

      BiometricPrompt.ERROR_HW_NOT_PRESENT ->
        KeychainException(errorMessage, Errors.E_BIOMETRIC_HARDWARE_NOT_PRESENT)

      BiometricPrompt.ERROR_NEGATIVE_BUTTON ->
        KeychainException(errorMessage, Errors.E_AUTH_CANCELED)

      BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL ->
        KeychainException(errorMessage, Errors.E_PASSCODE_NOT_SET)

      BiometricPrompt.ERROR_VENDOR ->
        KeychainException(errorMessage, Errors.E_BIOMETRIC_VENDOR_ERROR)

      else ->
        KeychainException("code: $errorCode, msg: $errorMessage", Errors.E_AUTH_ERROR)
    }
  }


  /** Called when a biometric is recognized. */
  override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
    try {
      context ?: throw NullPointerException("Crypto context is not assigned yet.")

      when (context?.operation) {
        CryptoOperation.ENCRYPT -> {
          val encrypted = EncryptionResult(
            storage.encryptString(context!!.key, String(context!!.username)),
            storage.encryptString(context!!.key, String(context!!.password)),
            storage
          )
          onEncrypt(encrypted, null)
        }

        CryptoOperation.DECRYPT -> {
          val decrypted = DecryptionResult(
            storage.decryptBytes(context!!.key, context!!.username),
            storage.decryptBytes(context!!.key, context!!.password)
          )
          onDecrypt(decrypted, null)
        }

        null -> Log.e(LOG_TAG, "No operation context available")
      }
    } catch (fail: Throwable) {
      when (context?.operation) {
        CryptoOperation.ENCRYPT -> onEncrypt(null, fail)
        CryptoOperation.DECRYPT -> onDecrypt(null, fail)
        null -> Log.e(LOG_TAG, "No operation context available")
      }
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

  protected fun authenticateWithPrompt(activity: FragmentActivity): BiometricPrompt {
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
