package com.oblador.keychain.cipherStorage

import android.annotation.TargetApi
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.NonNull
import com.facebook.react.bridge.ReactApplicationContext
import com.oblador.keychain.KeychainModule.KnownCiphers
import com.oblador.keychain.SecurityLevel
import com.oblador.keychain.decryptionHandler.DecryptionResultHandler
import com.oblador.keychain.exceptions.CryptoFailedException
import com.oblador.keychain.exceptions.KeyStoreAccessException
import java.io.IOException
import java.io.InputStream
import java.security.GeneralSecurityException
import java.security.Key
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.GCMParameterSpec


@TargetApi(Build.VERSION_CODES.M)
class CipherStorageKeystoreAesGcm(@NonNull reactContext: ReactApplicationContext) :
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
  override fun getCipherStorageName(): String = KnownCiphers.AES_GCM

  /** API23 is a requirement. */
  override fun getMinSupportedApiLevel(): Int = Build.VERSION_CODES.M

  /** It can guarantee security levels up to SECURE_HARDWARE/SE/StrongBox */
  override fun securityLevel(): SecurityLevel = SecurityLevel.SECURE_HARDWARE

  /** Biometry is Not Supported. */
  override fun isBiometrySupported(): Boolean = false

  /** AES. */
  @NonNull override fun getEncryptionAlgorithm(): String = ALGORITHM_AES

  /** AES/CBC/PKCS7Padding */
  @NonNull override fun getEncryptionTransformation(): String = ENCRYPTION_TRANSFORMATION

  // endregion

  // region Overrides
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
        encryptString(key, username), encryptString(key, password), this)
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
        decryptBytes(key, username), decryptBytes(key, password), getSecurityLevel(key))
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

  // endregion

  // region Implementation

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
      .setBlockModes(BLOCK_MODE_GCM)
      .setEncryptionPaddings(PADDING_NONE)
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
      val iv = readIv(input)
      cipher.init(Cipher.DECRYPT_MODE, key, iv)
    }

    /** Extract initialization vector from provided bytes array. */
    @Throws(IOException::class)
    fun readIv(bytes: ByteArray): GCMParameterSpec {
      val iv = ByteArray(IV_LENGTH)

      if (IV_LENGTH >= bytes.size)
        throw IOException("Insufficient length of input data for IV extracting.")

      System.arraycopy(bytes, 0, iv, 0, IV_LENGTH)

      return GCMParameterSpec(TAG_LENGTH, iv)
    }

    /** Extract initialization vector from provided input stream. */
    @Throws(IOException::class)
    fun readIv(inputStream: InputStream): GCMParameterSpec {
      val iv = ByteArray(IV_LENGTH)
      val result = inputStream.read(iv, 0, IV_LENGTH)

      if (result != IV_LENGTH) throw IOException("Input stream has insufficient data.")

      return GCMParameterSpec(TAG_LENGTH, iv)
    }
  }

  @NonNull
  @Throws(GeneralSecurityException::class, IOException::class)
  override fun encryptString(@NonNull key: Key, @NonNull value: String): ByteArray =
    encryptString(key, value, IV.encrypt)

  @NonNull
  @Throws(GeneralSecurityException::class, IOException::class)
  override fun decryptBytes(@NonNull key: Key, @NonNull bytes: ByteArray): String =
    decryptBytes(key, bytes, IV.decrypt)

  // endregion
}
