package com.oblador.keychain.cipherStorage

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.oblador.keychain.KeychainModule.KnownCiphers
import com.oblador.keychain.SecurityLevel
import com.oblador.keychain.cipherStorage.CipherStorageKeystoreAesCbc.IV.IV_LENGTH
import com.oblador.keychain.resultHandler.ResultHandler
import com.oblador.keychain.exceptions.KeychainException
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.Key
import java.security.spec.KeySpec
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec

class CipherStorageKeystoreAesCbc(reactContext: ReactApplicationContext) :
    CipherStorageBase(reactContext) {

    // region Constants
    /** AES */
    companion object {
        const val ALGORITHM_AES = KeyProperties.KEY_ALGORITHM_AES

        /** CBC */
        const val BLOCK_MODE_CBC = KeyProperties.BLOCK_MODE_CBC

        /** PKCS7 */
        const val PADDING_PKCS7 = KeyProperties.ENCRYPTION_PADDING_PKCS7

        /** Transformation path. */
        const val ENCRYPTION_TRANSFORMATION = "$ALGORITHM_AES/$BLOCK_MODE_CBC/$PADDING_PKCS7"

        /** Key size. */
        const val ENCRYPTION_KEY_SIZE = 256

        const val DEFAULT_SERVICE = "RN_KEYCHAIN_DEFAULT_ALIAS"
    }

    // endregion

    // region Configuration
    override fun getCipherStorageName(): String = KnownCiphers.AES_CBC

    /** API23 is a requirement. */
    override fun getMinSupportedApiLevel(): Int = Build.VERSION_CODES.M

    /** It can guarantee security levels up to SECURE_HARDWARE/SE/StrongBox */
    override fun securityLevel(): SecurityLevel = SecurityLevel.SECURE_HARDWARE

    /** Biometry is Not Supported. */
    override fun isAuthSupported(): Boolean = false

    /** AES. */

    override fun getEncryptionAlgorithm(): String = ALGORITHM_AES

    /** AES/CBC/PKCS7Padding */

    override fun getEncryptionTransformation(): String = ENCRYPTION_TRANSFORMATION

    /** Override for saving the compatibility with previous version of lib. */
    override fun getDefaultAliasServiceName(): String = DEFAULT_SERVICE

    // endregion

    // region Overrides

    @Throws(KeychainException::class)
    override fun encrypt(
        handler: ResultHandler,
        alias: String,
        username: String,
        password: String,
        level: SecurityLevel
    ) {

        throwIfInsufficientLevel(level)

        val safeAlias = getDefaultAliasIfEmpty(alias, getDefaultAliasServiceName())
        val retries = AtomicInteger(1)

        try {
            val key = extractGeneratedKey(safeAlias, level, retries)

            val result = CipherStorage.EncryptionResult(
                encryptString(key, username), encryptString(key, password), this
            )
            handler.onEncrypt(result, null)
        } catch (fail: Throwable) {
            throw KeychainException("Could not encrypt data with alias: $alias, error: ${fail.message}", fail)
        }
    }


    /** Redirect call to [decrypt] method. */
    @Throws(KeychainException::class)
    override fun decrypt(
        handler: ResultHandler,
        alias: String,
        username: ByteArray,
        password: ByteArray,
        level: SecurityLevel
    ) {

        throwIfInsufficientLevel(level)

        val safeAlias = getDefaultAliasIfEmpty(alias, getDefaultAliasServiceName())
        val retries = AtomicInteger(1)

        try {
            val key = extractGeneratedKey(safeAlias, level, retries)

            val results = CipherStorage.DecryptionResult(
                decryptBytes(key, username), decryptBytes(key, password), getSecurityLevel(key)
            )
            handler.onDecrypt(results, null)
        } catch (e: GeneralSecurityException) {
            throw KeychainException("Could not decrypt data with alias: $alias, error: ${e.message}", e)
        } catch (fail: Throwable) {
            handler.onDecrypt(null, fail)
        }
    }

    // endregion

    // region Implementation

    /** Get encryption algorithm specification builder instance. */

    @Throws(GeneralSecurityException::class)
    override fun getKeyGenSpecBuilder(
        alias: String
    ): KeyGenParameterSpec.Builder {
        val purposes = KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT

        return KeyGenParameterSpec.Builder(alias, purposes)
            .setBlockModes(BLOCK_MODE_CBC)
            .setEncryptionPaddings(PADDING_PKCS7)
            .setRandomizedEncryptionRequired(true)
            .setKeySize(ENCRYPTION_KEY_SIZE)
    }

    /** Get information about provided key. */

    @Throws(GeneralSecurityException::class)
    override fun getKeyInfo(key: Key): KeyInfo {
        val factory = SecretKeyFactory.getInstance(key.algorithm, KEYSTORE_TYPE)
        val keySpec: KeySpec = factory.getKeySpec(key as SecretKey, KeyInfo::class.java)

        return keySpec as KeyInfo
    }

    /** Try to generate key from provided specification. */

    @Throws(GeneralSecurityException::class)
    override fun generateKey(spec: KeyGenParameterSpec): Key {
        val generator = KeyGenerator.getInstance(getEncryptionAlgorithm(), KEYSTORE_TYPE)

        // initialize key generator
        generator.init(spec)

        return generator.generateKey()
    }

    /** Decrypt provided bytes to a string. */

    @Throws(GeneralSecurityException::class, IOException::class)
    override fun decryptBytes(
        key: Key,
        bytes: ByteArray,
        handler: DecryptBytesHandler?
    ): String {
        val cipher = getCipher()

        return try {
            // read the initialization vector from bytes array
            val iv = ByteArray(IV_LENGTH)

            if (IV_LENGTH >= bytes.size)
                throw IOException("Insufficient length of input data for IV extracting.")

            System.arraycopy(bytes, 0, iv, 0, IV_LENGTH)

            val spec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            // Decrypt the bytes using cipher.doFinal()
            val decryptedBytes = cipher.doFinal(bytes, IV.IV_LENGTH, bytes.size - IV.IV_LENGTH)
            String(decryptedBytes, UTF8)
        } catch (fail: Throwable) {
            Log.w(LOG_TAG, fail.message, fail)
            throw fail
        }
    }

    // endregion

    // region Initialization Vector encrypt/decrypt support

    /** Initialization vector support. */
    object IV {
        /** Encryption/Decryption initialization vector length. */
        const val IV_LENGTH = 16

        /** Save Initialization vector to output stream. */
        val encrypt = EncryptStringHandler { cipher, key, output ->
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            output.write(iv, 0, iv.size)
        }

        /** Read initialization vector from input stream and configure cipher by it. */
        val decrypt = DecryptBytesHandler { cipher, key, input ->
            val iv = ByteArray(IV_LENGTH)
            val result = input.read(iv, 0, IV_LENGTH)

            if (result != IV_LENGTH) throw IOException("Input stream has insufficient data.")

            val spec = IvParameterSpec(iv)

            cipher.init(Cipher.DECRYPT_MODE, key, spec)
        }

    }


    @Throws(GeneralSecurityException::class, IOException::class)
    override fun encryptString(key: Key, value: String): ByteArray =
        encryptString(key, value, IV.encrypt)


    @Throws(GeneralSecurityException::class, IOException::class)
    override fun decryptBytes(key: Key, bytes: ByteArray): String =
        decryptBytes(key, bytes, IV.decrypt)

    // endregion
}
