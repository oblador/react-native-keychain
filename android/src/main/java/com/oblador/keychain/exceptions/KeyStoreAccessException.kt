package com.oblador.keychain.exceptions

import java.security.GeneralSecurityException

class KeyStoreAccessException : GeneralSecurityException {
    constructor(message: String?) : super(message)
    constructor(message: String?, t: Throwable?) : super(message, t)
}
