package com.oblador.keychain.exceptions

import com.oblador.keychain.exceptions.CryptoFailedException
import java.security.GeneralSecurityException

class CryptoFailedException : GeneralSecurityException {
    constructor(message: String?) : super(message)
    constructor(message: String?, t: Throwable?) : super(message, t)

    companion object {
        @JvmStatic
        @Throws(CryptoFailedException::class)
        fun reThrowOnError(error: Throwable?) {
            if (null == error) return
            if (error is CryptoFailedException) throw (error as CryptoFailedException?)!!
            throw CryptoFailedException("Wrapped error: " + error.message, error)
        }
    }
}
