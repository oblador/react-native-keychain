package com.oblador.keychain.cipherStorage

import android.annotation.TargetApi
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.oblador.keychain.KeychainModule.KnownCiphers
import com.oblador.keychain.SecurityLevel
import com.oblador.keychain.resultHandler.CryptoContext
import com.oblador.keychain.resultHandler.CryptoOperation
import com.oblador.keychain.resultHandler.ResultHandler
import com.oblador.keychain.exceptions.CryptoFailedException
import com.oblador.keychain.exceptions.KeyStoreAccessException
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.Key
import java.security.spec.KeySpec
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec

@TargetApi(Build.VERSION_CODES.M)
class CipherStorageKeystoreAesGcm(reactContext: ReactApplicationContext, private val requiresBiometricAuth: Boolean) :
    CipherStorageBase(reactContext) {

    // region Constants
    /** AES */
    companion object {
        const val ALGORITHM_AES = KeyProperties.KEY_ALGORITHM_AES

        /** GCM */
        const val BLOCK_MODE_GCM = KeyProperties.BLOCK_MODE_GCM

        /** PKCS7 */
        const val PADDING_NONE = KeyProperties.ENCRYPTION_PADDING_NONE

        /** Transformation path. */
        const val ENCRYPTION_TRANSFORMATION = "$ALGORITHM_AES/$BLOCK_MODE_GCM/$PADDING_NONE"

        /** Key size. */
        const val ENCRYPTION_KEY_SIZE = 256
    }

    // endregion

    // region Configuration
    override fun getCipherStorageName(): String = when (requiresBiometricAuth) {
        true -> KnownCiphers.AES_GCM
        false -> KnownCiphers.AES_GCM_NO_AUTH
    }

    /** API23 is a requirement. */
    override fun getMinSupportedApiLevel(): Int = Build.VERSION_CODES.M

    /** It can guarantee security levels up to SECURE_HARDWARE/SE/StrongBox */
    override fun securityLevel(): SecurityLevel = SecurityLevel.SECURE_HARDWARE

    /** Biometry is Not Supported. */
    override fun isBiometrySupported(): Boolean = requiresBiometricAuth

    /** AES. */
    override fun getEncryptionAlgorithm(): String = ALGORITHM_AES

    /** AES/CBC/PKCS7Padding */
    override fun getEncryptionTransformation(): String = ENCRYPTION_TRANSFORMATION

    // endregion

    // region Overrides

    @Throws(CryptoFailedException::class)
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
        var key: Key? = null

        try {
            key = extractGeneratedKey(safeAlias, level, retries)

            val result = CipherStorage.EncryptionResult(
                encryptString(key, username), encryptString(key, password), this
            )
            handler.onEncrypt(result, null)
        } catch (ex: UserNotAuthenticatedException) {
            Log.d(LOG_TAG, "Unlock of keystore is needed. Error: ${ex.message}", ex)
            val context = CryptoContext(
                safeAlias,
                key!!,
                password.toByteArray(),
                username.toByteArray(),
                CryptoOperation.ENCRYPT
            )

            handler.askAccessPermissions(context)
        } catch (fail: Throwable) {
            handler.onEncrypt(null, fail)
        }
    }

    /** Redirect call to [decrypt] method. */
    @Throws(CryptoFailedException::class)
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
        var key: Key? = null

        try {
            key = extractGeneratedKey(safeAlias, level, retries)
            val results =
                CipherStorage.DecryptionResult(
                    decryptBytes(key, username),
                    decryptBytes(key, password)
                )

            handler.onDecrypt(results, null)
        } catch (ex: UserNotAuthenticatedException) {
            Log.d(LOG_TAG, "Unlock of keystore is needed. Error: ${ex.message}", ex)
            // expected that KEY instance is extracted and we caught exception on decryptBytes operation
            val context =
                CryptoContext(safeAlias, key!!, password, username, CryptoOperation.DECRYPT)

            handler.askAccessPermissions(context)
        } catch (fail: Throwable) {
            // any other exception treated as a failure
            handler.onDecrypt(null, fail)
        }
    }

    // endregion

    // region Implementation

    /** Get builder for encryption and decryption operations with required user Authentication. */

    @Throws(GeneralSecurityException::class)
    override fun getKeyGenSpecBuilder(alias: String): KeyGenParameterSpec.Builder =
        getKeyGenSpecBuilder(alias, false)

    /** Get encryption algorithm specification builder instance. */

    @Throws(GeneralSecurityException::class)
    override fun getKeyGenSpecBuilder(
        alias: String,
        isForTesting: Boolean
    ): KeyGenParameterSpec.Builder {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw KeyStoreAccessException("Unsupported API${Build.VERSION.SDK_INT} version detected.")
        }

        val purposes = KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT

        val validityDuration = 5
        val keyGenParameterSpecBuilder =
            KeyGenParameterSpec.Builder(alias, purposes)
                .setBlockModes(BLOCK_MODE_GCM)
                .setEncryptionPaddings(PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(ENCRYPTION_KEY_SIZE)

        if(requiresBiometricAuth) {
            keyGenParameterSpecBuilder.setUserAuthenticationRequired(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                keyGenParameterSpecBuilder.setUserAuthenticationParameters(
                    validityDuration, KeyProperties.AUTH_BIOMETRIC_STRONG
                )
            } else {
                keyGenParameterSpecBuilder.setUserAuthenticationValidityDurationSeconds(
                    validityDuration
                )
            }
        }

        return keyGenParameterSpecBuilder
    }

    /** Get information about provided key. */

    @Throws(GeneralSecurityException::class)
    override fun getKeyInfo(key: Key): KeyInfo {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw KeyStoreAccessException("Unsupported API${Build.VERSION.SDK_INT} version detected.")
        }

        val factory = SecretKeyFactory.getInstance(key.algorithm, KEYSTORE_TYPE)
        val keySpec: KeySpec = factory.getKeySpec(key as SecretKey, KeyInfo::class.java)

        return keySpec as KeyInfo
    }

    /** Try to generate key from provided specification. */

    @Throws(GeneralSecurityException::class)
    override fun generateKey(spec: KeyGenParameterSpec): Key {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw KeyStoreAccessException("Unsupported API${Build.VERSION.SDK_INT} version detected.")
        }

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
        val cipher = getCachedInstance()

        return try {
            if (IV.IV_LENGTH >= bytes.size)
                throw IOException("Insufficient length of input data for IV extracting.")
            val iv = ByteArray(IV.IV_LENGTH)
            System.arraycopy(bytes, 0, iv, 0, IV.IV_LENGTH)
            val spec = GCMParameterSpec(IV.TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            // Decrypt the bytes using cipher.doFinal()
            val decryptedBytes = cipher.doFinal(bytes, IV.IV_LENGTH, bytes.size - IV.IV_LENGTH)
            String(decryptedBytes, UTF8)
        } catch (ex: UserNotAuthenticatedException){
            throw ex
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
        const val IV_LENGTH = 12
        const val TAG_LENGTH = 128

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
            val spec = GCMParameterSpec(TAG_LENGTH, iv)
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
