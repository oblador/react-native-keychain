package com.oblador.keychain.cipherStorage

import android.util.Log
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException

/**
 * Thread-safe cache for Cipher instances to improve performance by reusing existing instances.
 * Uses ThreadLocal storage to maintain separate caches for different threads.
 */
object CipherCache {
    private val LOG_TAG = CipherCache::class.java.simpleName

    private val cipherCache = ThreadLocal<MutableMap<String, Cipher>>()

    /**
     * Gets or creates a Cipher instance for the specified transformation.
     * This method is thread-safe and caches Cipher instances per thread.
     *
     * @param transformation The name of the transformation, e.g., "AES/CBC/PKCS7Padding"
     * @return A Cipher instance for the requested transformation
     * @throws NoSuchAlgorithmException if the transformation algorithm is not available
     * @throws NoSuchPaddingException if the padding scheme is not available
     */
    @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class)
    fun getCipher(transformation: String): Cipher {
        return synchronized(this) {
            (cipherCache.get() ?: mutableMapOf<String, Cipher>().also { cipherCache.set(it) })
                .getOrPut(transformation) { Cipher.getInstance(transformation) }
        }
    }

    /**
     * Clears the cipher cache for the current thread.
     * This should be called when the ciphers are no longer needed to free up resources.
     */
    fun clearCache() {
        try {
            cipherCache.remove()
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to clear cipher cache: ${e.message}")
        }
    }
}
