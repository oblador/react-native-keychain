package com.oblador.keychain.resultHandler

import android.os.Build
import androidx.biometric.BiometricPrompt
import com.facebook.react.bridge.ReactApplicationContext
import com.oblador.keychain.cipherStorage.CipherStorage

// NOTE: the logic for handling OnePlus bug is taken from the following forum post:
// https://forums.oneplus.com/threads/oneplus-7-pro-fingerprint-biometricprompt-does-not-show.1035821/#post-21710422
object ResultHandlerProvider {
  private const val ONE_PLUS_BRAND = "oneplus"
  private val ONE_PLUS_MODELS_WITHOUT_BIOMETRIC_BUG =
          arrayOf(
                  "A0001", // OnePlus One
                  "ONE A2001",
                  "ONE A2003",
                  "ONE A2005", // OnePlus 2
                  "ONE E1001",
                  "ONE E1003",
                  "ONE E1005", // OnePlus X
                  "ONEPLUS A3000",
                  "ONEPLUS SM-A3000",
                  "ONEPLUS A3003", // OnePlus 3
                  "ONEPLUS A3010", // OnePlus 3T
                  "ONEPLUS A5000", // OnePlus 5
                  "ONEPLUS A5010", // OnePlus 5T
                  "ONEPLUS A6000",
                  "ONEPLUS A6003" // OnePlus 6
          )

  private fun hasOnePlusBiometricBug(): Boolean {
    return Build.BRAND.equals(ONE_PLUS_BRAND, ignoreCase = true) &&
            !ONE_PLUS_MODELS_WITHOUT_BIOMETRIC_BUG.contains(Build.MODEL)
  }

  fun getHandler(
          reactContext: ReactApplicationContext,
          storage: CipherStorage,
          promptInfo: BiometricPrompt.PromptInfo,
          retryWithPasscode: Boolean
  ): ResultHandler {
    return if (storage.isAuthSupported()) {
      if (hasOnePlusBiometricBug()) {
        ResultHandlerInteractiveBiometricManualRetry(reactContext, storage, promptInfo)
      } else {
        ResultHandlerInteractiveBiometric(reactContext, storage, promptInfo, retryWithPasscode)
      }
    } else {
      ResultHandlerNonInteractive()
    }
  }
}
