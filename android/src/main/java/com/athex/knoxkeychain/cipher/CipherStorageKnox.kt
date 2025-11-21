package com.athex.knoxkeychain.cipher

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Log
import com.athex.knoxkeychain.utils.KnoxUtils
import com.facebook.react.bridge.ReactApplicationContext
import com.oblador.keychain.SecurityLevel
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionResult
import com.oblador.keychain.cipherStorage.CipherStorage.EncryptionResult
import com.oblador.keychain.cipherStorage.CipherStorageBase
import com.oblador.keychain.exceptions.KeychainException
import com.oblador.keychain.resultHandler.CryptoContext
import com.oblador.keychain.resultHandler.CryptoOperation
import com.oblador.keychain.resultHandler.ResultHandler
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.Key
import java.security.KeyStore
import java.security.spec.KeySpec
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory

class CipherStorageKnox(
        reactContext: ReactApplicationContext,
        private val requiresAuth: Boolean = false
) : CipherStorageBase(reactContext) {

    companion object {
        const val CIPHER_NAME = "KnoxAES"
        const val CIPHER_NAME_NO_AUTH = "KnoxAES_NoAuth"
        const val ALGORITHM_AES = KeyProperties.KEY_ALGORITHM_AES
        const val BLOCK_MODE_GCM = KeyProperties.BLOCK_MODE_GCM
        const val PADDING_NONE = KeyProperties.ENCRYPTION_PADDING_NONE
        const val ENCRYPTION_TRANSFORMATION = "$ALGORITHM_AES/$BLOCK_MODE_GCM/$PADDING_NONE"
        const val ENCRYPTION_KEY_SIZE = 256
    }

    private val knoxKeyStoreName: String? = KnoxUtils.getKnoxKeyStoreName()

    override fun getCipherStorageName(): String =
            when (requiresAuth) {
                true -> CIPHER_NAME
                false -> CIPHER_NAME_NO_AUTH
            }

    override fun getMinSupportedApiLevel(): Int {
        return Build.VERSION_CODES.M // Knox usually available on M+
    }

    override fun securityLevel(): SecurityLevel {
        return SecurityLevel.SECURE_HARDWARE
    }

    override fun isAuthSupported(): Boolean {
        return requiresAuth // Support authentication when required
    }

    override fun getEncryptionAlgorithm(): String {
        return ALGORITHM_AES
    }

    override fun getEncryptionTransformation(): String {
        return ENCRYPTION_TRANSFORMATION
    }

    @Throws(KeychainException::class)
    override fun encrypt(
            handler: ResultHandler,
            alias: String,
            username: String,
            password: String,
            level: SecurityLevel
    ) {
        if (!KnoxUtils.isKnoxAvailable()) {
            handler.onEncrypt(null, KeychainException("Knox KeyStore not available"))
            return
        }

        val safeAlias = getDefaultAliasIfEmpty(alias, getDefaultAliasServiceName())
        val retries = AtomicInteger(1)
        var key: Key? = null

        try {
            key = getOrCreateKey(safeAlias, level, retries)

            val result =
                    EncryptionResult(
                            encryptString(key, username),
                            encryptString(key, password),
                            this
                    )
            handler.onEncrypt(result, null)
        } catch (ex: android.security.keystore.UserNotAuthenticatedException) {
            Log.d("CipherStorageKnox", "Unlock of keystore is needed. Error: ${ex.message}", ex)
            val context =
                    CryptoContext(
                            safeAlias,
                            key!!,
                            password.toByteArray(),
                            username.toByteArray(),
                            CryptoOperation.ENCRYPT
                    )
            handler.askAccessPermissions(context)
        } catch (e: Throwable) {
            handler.onEncrypt(null, e)
        }
    }

    @Throws(KeychainException::class)
    override fun decrypt(
            handler: ResultHandler,
            alias: String,
            username: ByteArray,
            password: ByteArray,
            level: SecurityLevel
    ) {
        if (!KnoxUtils.isKnoxAvailable()) {
            handler.onDecrypt(null, KeychainException("Knox KeyStore not available"))
            return
        }

        val safeAlias = getDefaultAliasIfEmpty(alias, getDefaultAliasServiceName())
        val retries = AtomicInteger(1)
        var key: Key? = null

        try {
            key = getOrCreateKey(safeAlias, level, retries)

            val result = DecryptionResult(decryptBytes(key, username), decryptBytes(key, password))
            handler.onDecrypt(result, null)
        } catch (ex: android.security.keystore.UserNotAuthenticatedException) {
            Log.d(
                    "CipherStorageKnox",
                    "Unlock of keystore is needed for decrypt. Error: ${ex.message}",
                    ex
            )
            val context =
                    CryptoContext(safeAlias, key!!, password, username, CryptoOperation.DECRYPT)
            handler.askAccessPermissions(context)
        } catch (e: Throwable) {
            handler.onDecrypt(null, e)
        }
    }

    @Throws(GeneralSecurityException::class)
    public override fun generateKey(spec: KeyGenParameterSpec): Key {
        // For Android 12+ (API 31+), TIMA KeyStore is deprecated/removed.
        // We should use Android KeyStore with StrongBox if available.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val generator = KeyGenerator.getInstance(getEncryptionAlgorithm(), "AndroidKeyStore")
            generator.init(spec)
            return generator.generateKey()
        }

        // Legacy Knox (TIMA)
        if (knoxKeyStoreName != null) {
            val generator = KeyGenerator.getInstance(getEncryptionAlgorithm(), knoxKeyStoreName)
            generator.init(spec)
            return generator.generateKey()
        }

        throw GeneralSecurityException("No suitable KeyStore found for Knox")
    }

    @Throws(GeneralSecurityException::class)
    public override fun getKeyGenSpecBuilder(alias: String): KeyGenParameterSpec.Builder {
        val purposes = KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT
        val builder =
                KeyGenParameterSpec.Builder(alias, purposes)
                        .setBlockModes(BLOCK_MODE_GCM)
                        .setEncryptionPaddings(PADDING_NONE)
                        .setRandomizedEncryptionRequired(true)
                        .setKeySize(ENCRYPTION_KEY_SIZE)

        // Add authentication requirement if needed
        if (requiresAuth) {
            val validityDuration = 5
            builder.setUserAuthenticationRequired(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(
                        validityDuration,
                        KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
                )
            } else {
                builder.setUserAuthenticationValidityDurationSeconds(validityDuration)
            }
        }

        // On Android 9+ (Pie) and above, we can request StrongBox
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // We try to request StrongBox, but it might fail if not available on the device.
            // Ideally we check for StrongBox availability first.
            // For this implementation, we assume if the user selected Knox/Hardware, they want
            // StrongBox.
            // However, forcing it might crash if not available.
            // Let's leave it as default Android behavior which usually picks TEE/StrongBox if
            // available
            // unless we explicitly set setIsStrongBoxBacked(true).
            // builder.setIsStrongBoxBacked(true)
        }

        return builder
    }

    @Throws(GeneralSecurityException::class)
    override fun getKeyInfo(key: Key): KeyInfo {
        val useAndroidKeyStore = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val storeName =
                if (useAndroidKeyStore) "AndroidKeyStore" else knoxKeyStoreName ?: "AndroidKeyStore"

        val factory = SecretKeyFactory.getInstance(key.algorithm, storeName)
        val keySpec: KeySpec = factory.getKeySpec(key as SecretKey, KeyInfo::class.java)
        return keySpec as KeyInfo
    }

    // We define this to ensure we load from the correct KeyStore
    // Renamed from extractGeneratedKey to avoid hiding final parent method
    fun getOrCreateKey(safeAlias: String, level: SecurityLevel, retries: AtomicInteger): Key {
        val useAndroidKeyStore = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val storeName = if (useAndroidKeyStore) "AndroidKeyStore" else knoxKeyStoreName

        val keyStore = KeyStore.getInstance(storeName)
        keyStore.load(null)
        if (!keyStore.containsAlias(safeAlias)) {
            generateKey(getKeyGenSpecBuilder(safeAlias).build())
        }
        return keyStore.getKey(safeAlias, null)
    }

    // ======================== RSA SIGNING SUPPORT ========================

    /**
     * Generate an RSA key pair in Knox KeyStore for signing operations.
     * @param alias The key alias
     * @return true if successful
     */
    @Throws(GeneralSecurityException::class)
    fun generateRSAKey(alias: String): Boolean {
        val useAndroidKeyStore = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val storeName = if (useAndroidKeyStore) "AndroidKeyStore" else knoxKeyStoreName

        if (storeName == null) {
            throw GeneralSecurityException("Knox KeyStore not available")
        }

        val keyPairGenerator =
                java.security.KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_RSA,
                        storeName
                )

        val purposes = KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        val spec =
                KeyGenParameterSpec.Builder(alias, purposes)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                        .setKeySize(2048)
                        .build()

        keyPairGenerator.initialize(spec)
        keyPairGenerator.generateKeyPair()

        Log.d("CipherStorageKnox", "Generated RSA key with alias: $alias in $storeName")
        return true
    }

    /**
     * Sign data using an RSA private key from Knox KeyStore.
     * @param alias The key alias
     * @param data The data to sign
     * @return The signature bytes
     */
    @Throws(GeneralSecurityException::class)
    fun signData(alias: String, data: ByteArray): ByteArray {
        val useAndroidKeyStore = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val storeName = if (useAndroidKeyStore) "AndroidKeyStore" else knoxKeyStoreName

        if (storeName == null) {
            throw GeneralSecurityException("Knox KeyStore not available")
        }

        val keyStore = KeyStore.getInstance(storeName)
        keyStore.load(null)

        val privateKey =
                keyStore.getKey(alias, null) as? java.security.PrivateKey
                        ?: throw GeneralSecurityException("Private key not found for alias: $alias")

        val signature = java.security.Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(data)

        val signatureBytes = signature.sign()
        Log.d(
                "CipherStorageKnox",
                "Signed data with alias: $alias, signature length: ${signatureBytes.size}"
        )
        return signatureBytes
    }

    /**
     * Verify a signature using an RSA public key from Knox KeyStore.
     * @param alias The key alias
     * @param data The original data
     * @param signatureBytes The signature to verify
     * @return true if signature is valid
     */
    @Throws(GeneralSecurityException::class)
    fun verifySignature(alias: String, data: ByteArray, signatureBytes: ByteArray): Boolean {
        val useAndroidKeyStore = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val storeName = if (useAndroidKeyStore) "AndroidKeyStore" else knoxKeyStoreName

        if (storeName == null) {
            throw GeneralSecurityException("Knox KeyStore not available")
        }

        val keyStore = KeyStore.getInstance(storeName)
        keyStore.load(null)

        val certificate =
                keyStore.getCertificate(alias)
                        ?: throw GeneralSecurityException("Certificate not found for alias: $alias")

        val publicKey = certificate.publicKey
        val signature = java.security.Signature.getInstance("SHA256withRSA")
        signature.initVerify(publicKey)
        signature.update(data)

        val isValid = signature.verify(signatureBytes)
        Log.d("CipherStorageKnox", "Verified signature for alias: $alias, valid: $isValid")
        return isValid
    }
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
            val spec = javax.crypto.spec.GCMParameterSpec(TAG_LENGTH, iv)
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
