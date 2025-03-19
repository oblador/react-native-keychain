package com.oblador.keychain.cipherStorage

import android.util.Log
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException

object CipherCache {
    private val LOG_TAG = CipherCache::class.java.simpleName

    private val cipherCache = ThreadLocal<MutableMap<String, Cipher>>()

    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class)
    fun getCipher(transformation: String): Cipher {
        var ciphers = cipherCache.get()
        if (ciphers == null) {
            ciphers = HashMap()
            cipherCache.set(ciphers)
        }

        var cipher = ciphers[transformation]
        if (cipher == null) {
            cipher = Cipher.getInstance(transformation)
            ciphers[transformation] = cipher
        }

        return cipher
    }

    fun clearCache() {
        try {
            cipherCache.remove()
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to clear cipher cache: ${e.message}")
        }
    }
}
