package com.oblador.keychain.exceptions

import android.security.keystore.KeyPermanentlyInvalidatedException
import com.oblador.keychain.KeychainModule.Errors
import java.security.GeneralSecurityException

class KeychainException : GeneralSecurityException {
  val errorCode: String

  constructor(message: String?) : super(message) {
    this.errorCode = Errors.E_INTERNAL_ERROR
  }

  constructor(message: String?, errorCode: String) : super(message) {
    this.errorCode = errorCode
  }

  constructor(message: String?, t: Throwable?) : super(message, t) {
    this.errorCode = when (t) {
      is KeyPermanentlyInvalidatedException -> Errors.E_AUTH_INVALIDATED
      is KeychainException -> t.errorCode
      else -> Errors.E_UNKNOWN_ERROR
    }
  }
}
