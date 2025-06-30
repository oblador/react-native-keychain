package com.oblador.keychain.exceptions

import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
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
      is UserNotAuthenticatedException -> Errors.E_ANDROID_USER_NOT_AUTHENTICATED
      is KeyPermanentlyInvalidatedException -> Errors.E_KEY_PERMANENTLY_INVALIDATED
      is KeychainException -> t.errorCode
      else -> Errors.E_UNKNOWN_ERROR
    }
  }
}
