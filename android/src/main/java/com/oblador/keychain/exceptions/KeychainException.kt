package com.oblador.keychain.exceptions

import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import com.oblador.keychain.KeychainModule.Errors
import java.security.GeneralSecurityException

class KeychainException : GeneralSecurityException {
  val errorCode: String

  constructor(message: String?) : super(message) {
    this.errorCode = Errors.E_CRYPTO_FAILED
  }

  constructor(message: String?, errorCode: String) : super(message) {
    this.errorCode = errorCode
  }

  constructor(message: String?, t: Throwable?) : super(message, t) {
    this.errorCode = when (t) {
      is UserNotAuthenticatedException -> Errors.E_KEYSTORE_USER_NOT_AUTHENTICATED
      is KeyPermanentlyInvalidatedException -> Errors.E_KEYSTORE_KEY_INVALIDATED
      is KeychainException -> t.errorCode
      else -> Errors.E_CRYPTO_FAILED
    }
  }
}
