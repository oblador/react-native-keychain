package com.oblador.keychain.cipherStorage

import android.annotation.TargetApi
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.NonNull
import com.oblador.keychain.KeychainModule.KnownCiphers
import com.oblador.keychain.SecurityLevel
import com.oblador.keychain.decryptionHandler.DecryptionResultHandler
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
import javax.crypto.spec.IvParameterSpec

/**
 * @see [Secure Data in Android](https://proandroiddev.com/secure-data-in-android-initialization-vector-6ca1c659762c)
 * @see [AES cipher](https://stackoverflow.com/questions/36827352/android-aes-with-keystore-produces-different-cipher-text-with-same-plain-text)
 */
@TargetApi(Build.VERSION_CODES.M)
@Suppress("unused", "MemberVisibilityCanBePrivate")
class CipherStorageKeystoreAesCbc : CipherStorageBase() {

  //region Constants
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
  //endregion

  //region Configuration
  override fun getCipherStorageName(): String = KnownCiphers.AES

  /** API23 is a requirement. */
  override fun getMinSupportedApiLevel(): Int = Build.VERSION_CODES.M

  /** It can guarantee security levels up to SECURE_HARDWARE/SE/StrongBox */
  override fun securityLevel(): SecurityLevel = SecurityLevel.SECURE_HARDWARE

  /** Biometry is Not Supported. */
  override fun isBiometrySupported(): Boolean = false

  /** AES. */
  @NonNull
  override fun getEncryptionAlgorithm(): String = ALGORITHM_AES

  /** AES/CBC/PKCS7Padding */
  @NonNull
  override fun getEncryptionTransformation(): String = ENCRYPTION_TRANSFORMATION

  /** Override for saving the compatibility with previous version of lib. */
  override fun getDefaultAliasServiceName(): String = DEFAULT_SERVICE

  //endregion

  //region Overrides
  @NonNull
  @Throws(CryptoFailedException::class)
  override fun encrypt(
    @NonNull alias: String,
    @NonNull username: String,
    @NonNull password: String,
    @NonNull level: SecurityLevel
  ): CipherStorage.EncryptionResult {

    throwIfInsufficientLevel(level)

    val safeAlias = getDefaultAliasIfEmpty(alias, getDefaultAliasServiceName())
    val retries = AtomicInteger(1)

    return try {
      val key = extractGeneratedKey(safeAlias, level, retries)

      CipherStorage.EncryptionResult(
        encryptString(key, username),
        encryptString(key, password),
        this
      )
    } catch (e: GeneralSecurityException) {
      throw CryptoFailedException("Could not encrypt data with alias: $alias", e)
    } catch (fail: Throwable) {
      throw CryptoFailedException("Unknown error with alias: $alias, error: ${fail.message}", fail)
    }
  }

  @NonNull
  @Throws(CryptoFailedException::class)
  override fun decrypt(
    @NonNull alias: String,
    @NonNull username: ByteArray,
    @NonNull password: ByteArray,
    @NonNull level: SecurityLevel
  ): CipherStorage.DecryptionResult {

    throwIfInsufficientLevel(level)

    val safeAlias = getDefaultAliasIfEmpty(alias, getDefaultAliasServiceName())
    val retries = AtomicInteger(1)

    return try {
      val key = extractGeneratedKey(safeAlias, level, retries)

      CipherStorage.DecryptionResult(
        decryptBytes(key, username),
        decryptBytes(key, password),
        getSecurityLevel(key)
      )
    } catch (e: GeneralSecurityException) {
      throw CryptoFailedException("Could not decrypt data with alias: $alias", e)
    } catch (fail: Throwable) {
      throw CryptoFailedException("Unknown error with alias: $alias, error: ${fail.message}", fail)
    }
  }

  /** Redirect call to [decrypt] method. */
  @Throws(CryptoFailedException::class)
  override fun decrypt(
    @NonNull handler: DecryptionResultHandler,
    @NonNull alias: String,
    @NonNull username: ByteArray,
    @NonNull password: ByteArray,
    @NonNull level: SecurityLevel
  ) {
    try {
      val results = decrypt(alias, username, password, level)
      handler.onDecrypt(results, null)
    } catch (fail: Throwable) {
      handler.onDecrypt(null, fail)
    }
  }
  //endregion

  //region Implementation

  /** Get builder for encryption and decryption operations with required user Authentication. */
  @NonNull
  @Throws(GeneralSecurityException::class)
  override fun getKeyGenSpecBuilder(@NonNull alias: String): KeyGenParameterSpec.Builder =
    getKeyGenSpecBuilder(alias, false)

  /** Get encryption algorithm specification builder instance. */
  @NonNull
  @Throws(GeneralSecurityException::class)
  override fun getKeyGenSpecBuilder(
    @NonNull alias: String,
    isForTesting: Boolean
  ): KeyGenParameterSpec.Builder {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      throw KeyStoreAccessException("Unsupported API${Build.VERSION.SDK_INT} version detected.")
    }

    val purposes = KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT

    return KeyGenParameterSpec.Builder(alias, purposes)
      .setBlockModes(BLOCK_MODE_CBC)
      .setEncryptionPaddings(PADDING_PKCS7)
      .setRandomizedEncryptionRequired(true)
      .setKeySize(ENCRYPTION_KEY_SIZE)
  }

  /** Get information about provided key. */
  @NonNull
  @Throws(GeneralSecurityException::class)
  override fun getKeyInfo(@NonNull key: Key): KeyInfo {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      throw KeyStoreAccessException("Unsupported API${Build.VERSION.SDK_INT} version detected.")
    }

    val factory = SecretKeyFactory.getInstance(key.algorithm, KEYSTORE_TYPE)
    val keySpec: KeySpec = factory.getKeySpec(key as SecretKey, KeyInfo::class.java)

    return keySpec as KeyInfo
  }

  /** Try to generate key from provided specification. */
  @NonNull
  @Throws(GeneralSecurityException::class)
  override fun generateKey(@NonNull spec: KeyGenParameterSpec): Key {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      throw KeyStoreAccessException("Unsupported API${Build.VERSION.SDK_INT} version detected.")
    }

    val generator = KeyGenerator.getInstance(getEncryptionAlgorithm(), KEYSTORE_TYPE)

    // initialize key generator
    generator.init(spec)

    return generator.generateKey()
  }

  /** Decrypt provided bytes to a string. */
  @NonNull
  @Throws(GeneralSecurityException::class, IOException::class)
  override fun decryptBytes(
    @NonNull key: Key,
    @NonNull bytes: ByteArray,
    handler: DecryptBytesHandler?
  ): String {
    val cipher = getCachedInstance()

    return try {
      // read the initialization vector from bytes array
      val iv = IV.readIv(bytes)
      cipher.init(Cipher.DECRYPT_MODE, key, iv)

      // Decrypt the bytes using cipher.doFinal()
      val decryptedBytes = cipher.doFinal(bytes, IV.IV_LENGTH, bytes.size - IV.IV_LENGTH)
      String(decryptedBytes, UTF8)
    } catch (fail: Throwable) {
      Log.w(LOG_TAG, fail.message, fail)
      throw fail
    }
  }
  //endregion

  //region Initialization Vector encrypt/decrypt support
  @NonNull
  @Throws(GeneralSecurityException::class, IOException::class)
  override fun encryptString(@NonNull key: Key, @NonNull value: String): ByteArray =
    encryptString(key, value, IV.encrypt)

  @NonNull
  @Throws(GeneralSecurityException::class, IOException::class)
  override fun decryptBytes(@NonNull key: Key, @NonNull bytes: ByteArray): String =
    decryptBytes(key, bytes, IV.decrypt)
  //endregion
}
