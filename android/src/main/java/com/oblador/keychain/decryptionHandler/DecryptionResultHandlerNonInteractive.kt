package com.oblador.keychain.decryptionHandler

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionContext
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionResult
import com.oblador.keychain.exceptions.CryptoFailedException

class DecryptionResultHandlerNonInteractive : DecryptionResultHandler {

  // Use 'override' and explicitly declare visibility
  override var result: DecryptionResult? = null
  override var error: Throwable? = null

  override fun askAccessPermissions(@NonNull context: DecryptionContext) {
    val failure = CryptoFailedException("Non-interactive decryption mode.")
    onDecrypt(null, failure)
  }

  override fun onDecrypt(
      @Nullable decryptionResult: DecryptionResult?,
      @Nullable error: Throwable?
  ) {
    this.result = decryptionResult
    this.error = error
  }

  override fun waitResult() {
    // Do nothing, expected synchronized call in one thread
  }
}
