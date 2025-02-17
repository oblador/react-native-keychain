package com.oblador.keychain.cipherStorage

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
import java.security.InvalidKeyException
import java.security.Key
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.NoSuchPaddingException


class CipherStorageKeystoreRsaEcb(reactContext: ReactApplicationContext) :
    CipherStorageBase(reactContext) {

    companion object {
        /** Selected algorithm. */
        const val ALGORITHM_RSA: String = KeyProperties.KEY_ALGORITHM_RSA

        /** Selected block mode. */
        const val BLOCK_MODE_ECB: String = KeyProperties.BLOCK_MODE_ECB

        /** Selected padding transformation. */
        const val PADDING_PKCS1: String = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1

        /** Composed transformation algorithms. */
        const val TRANSFORMATION_RSA_ECB_PKCS1: String =
            "$ALGORITHM_RSA/$BLOCK_MODE_ECB/$PADDING_PKCS1"

        /** Selected encryption key size. */
        const val ENCRYPTION_KEY_SIZE = 2048
    }

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
        try {
            extractGeneratedKey(safeAlias, level, retries)
            val result = innerEncryptedCredentials(safeAlias, password, username)
            handler.onEncrypt(result, null)
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is InvalidKeySpecException,
                is NoSuchPaddingException,
                is InvalidKeyException -> {
                    throw CryptoFailedException("Could not encrypt data for service $alias", e)
                }

                is KeyStoreException,
                is KeyStoreAccessException -> {
                    throw CryptoFailedException("Could not access Keystore for service $alias", e)
                }

                is IOException -> {
                    throw CryptoFailedException("I/O error: ${e.message}", e)
                }

                else -> {
                    throw CryptoFailedException("Unknown error: ${e.message}", e)
                }
            }
        }
    }


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
            // key is always NOT NULL otherwise GeneralSecurityException raised
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

    /** RSAECB. */
    override fun getCipherStorageName(): String = KnownCiphers.RSA

    /** API23 is a requirement. */
    override fun getMinSupportedApiLevel(): Int = Build.VERSION_CODES.M

    /** Biometry is supported. */
    override fun isBiometrySupported(): Boolean = true

    /** RSA. */
    override fun getEncryptionAlgorithm(): String = ALGORITHM_RSA

    /** RSA/ECB/PKCS1Padding */
    override fun getEncryptionTransformation(): String = TRANSFORMATION_RSA_ECB_PKCS1

    /**
     * Clean code without try/catch's that encrypt username and password with a key specified by
     * alias.
     */
    @Throws(GeneralSecurityException::class, IOException::class)
    private fun innerEncryptedCredentials(
        alias: String,
        password: String,
        username: String,
    ): CipherStorage.EncryptionResult {
        val keyStore = getKeyStoreAndLoad()

        // Retrieve the certificate after ensuring the key is compatible
        val certificate = keyStore.getCertificate(alias)
            ?: throw GeneralSecurityException("Certificate is null for alias $alias")

        val publicKey = certificate.publicKey
        val kf = KeyFactory.getInstance(ALGORITHM_RSA)
        val keySpec = X509EncodedKeySpec(publicKey.encoded)
        val key = kf.generatePublic(keySpec)

        return CipherStorage.EncryptionResult(
            encryptString(key, username), encryptString(key, password), this
        )
    }


    /** Get builder for encryption and decryption operations with required user Authentication. */
    @Throws(GeneralSecurityException::class)
    override fun getKeyGenSpecBuilder(
        alias: String,
    ): KeyGenParameterSpec.Builder {

        val purposes = KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT

        val keySize = ENCRYPTION_KEY_SIZE

        val validityDuration = 5
        val keyGenParameterSpecBuilder =
            KeyGenParameterSpec.Builder(alias, purposes)
                .setBlockModes(BLOCK_MODE_ECB)
                .setEncryptionPaddings(PADDING_PKCS1)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(true)
                .setKeySize(keySize)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            keyGenParameterSpecBuilder.setUserAuthenticationParameters(
                validityDuration,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            )
        } else {
            keyGenParameterSpecBuilder.setUserAuthenticationValidityDurationSeconds(validityDuration)
        }

        return keyGenParameterSpecBuilder
    }

    /** Get information about provided key. */
    @Throws(GeneralSecurityException::class)
    override fun getKeyInfo(key: Key): KeyInfo {

        val factory = KeyFactory.getInstance(key.algorithm, KEYSTORE_TYPE)

        return factory.getKeySpec(key, KeyInfo::class.java)
    }

    /** Try to generate key from provided specification. */
    @Throws(GeneralSecurityException::class)
    override fun generateKey(spec: KeyGenParameterSpec): Key {

        val generator = KeyPairGenerator.getInstance(getEncryptionAlgorithm(), KEYSTORE_TYPE)
        generator.initialize(spec)

        return generator.generateKeyPair().private
    }
}
