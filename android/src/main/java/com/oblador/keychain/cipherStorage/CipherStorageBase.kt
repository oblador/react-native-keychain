package com.oblador.keychain.cipherStorage

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.UserNotAuthenticatedException
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.oblador.keychain.DeviceAvailability
import com.oblador.keychain.SecurityLevel
import com.oblador.keychain.cipherStorage.CipherStorageBase.DecryptBytesHandler
import com.oblador.keychain.cipherStorage.CipherStorageBase.EncryptStringHandler
import com.oblador.keychain.exceptions.CryptoFailedException
import com.oblador.keychain.exceptions.KeyStoreAccessException
import com.oblador.keychain.resultHandler.ResultHandler
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.security.GeneralSecurityException
import java.security.Key
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.ProviderException
import java.security.UnrecoverableKeyException
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.NoSuchPaddingException


@Suppress("unused", "MemberVisibilityCanBePrivate", "UnusedReturnValue")
abstract class CipherStorageBase(protected val applicationContext: Context) : CipherStorage {
  // region Constants
  /** Logging tag. */
  protected val LOG_TAG get() = Companion.LOG_TAG

  /** Default key storage type/name. */
  companion object {
    const val KEYSTORE_TYPE = "AndroidKeyStore"
    const val PREFIX_DELIMITER = "_"
    
    /** Logging tag. */
    private val LOG_TAG = CipherStorageBase::class.java.simpleName
        
    // Prefix constants
    const val PREFIX_RSA = "RSA"
    const val PREFIX_AES_GCM = "AES_GCM"
    const val PREFIX_AES_GCM_NO_AUTH = "AES_GCM_NA"
    const val PREFIX_AES_CBC = "AES_CBC"

    /** Size of hash calculation buffer. Default: 4Kb. */
    private const val BUFFER_SIZE = 4 * 1024

    /** Default size of read/write operation buffer. Default: 16Kb. */
    private const val BUFFER_READ_WRITE_SIZE = 4 * BUFFER_SIZE

    /** Default charset encoding. */
    val UTF8: Charset = Charset.forName("UTF-8")

    /** Convert provided service name to safe not-null/not-empty value. */
    fun getDefaultAliasIfEmpty(service: String?, fallback: String): String {
      return if (service.isNullOrEmpty()) fallback else service
    }

    /**
     * Copy input stream to output.
     *
     * @param input instance of input stream.
     * @param output instance of output stream.
     * @throws IOException read/write operation failure.
     */
    @Throws(IOException::class)
    fun copy(input: InputStream, output: OutputStream) {
      val buf = ByteArray(BUFFER_READ_WRITE_SIZE)
      var len: Int
      while (input.read(buf).also { len = it } > 0) {
        output.write(buf, 0, len)
      }
    }

      fun getPrefixedAlias(alias: String, prefix: String): String {
      return if (alias.startsWith("$prefix$PREFIX_DELIMITER")) {
        alias
      } else {
        "$prefix$PREFIX_DELIMITER$alias"
      }
    }


    fun migrateLegacyKey(
        legacyAlias: String,
        newAlias: String,
        keyStore: KeyStore,
        handler: ResultHandler,
        level: SecurityLevel
    ) {
        try {
            val legacyKey = keyStore.getKey(legacyAlias, null) ?: return
            
            // Save the key under new alias
            keyStore.setKeyEntry(
                newAlias,
                legacyKey,
                null,
                keyStore.getCertificateChain(legacyAlias)
            )

            // Verify the new key works
            if (keyStore.getKey(newAlias, null) != null) {
                // If successful, delete the old key
                keyStore.deleteEntry(legacyAlias)
                Log.d(LOG_TAG, "Successfully migrated key from $legacyAlias to $newAlias")
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to migrate legacy key $legacyAlias: ${e.message}")
        }
    }
  }

  // endregion

  // region Members

  /** Cached instance of the Keystore. */
  @Transient
  protected var cachedKeyStore: KeyStore? = null

  // region Overrides

  /** Hardware supports keystore operations. */
  override fun securityLevel(): SecurityLevel {
    return SecurityLevel.SECURE_HARDWARE
  }

  /**
   * The higher value means better capabilities. Range: [19..1129]. Formula: `1000 *
   * isBiometrySupported() + 100 * isSecureHardware() + minSupportedApiLevel()`
   */
  override fun getCapabilityLevel(): Int {
    // max: 1000 + 100 + 29 == 1129
    // min: 0000 + 000 + 19 == 0019

    return (1000 * if (isAuthSupported()) 1 else 0) + (getMinSupportedApiLevel())
  }

  /** {@inheritDoc} */
  override fun getDefaultAliasServiceName(): String {
    return getCipherStorageName()
  }

  /** Remove key with provided name from security storage. */
  override fun removeKey(alias: String) {
    val defaultAlias = getDefaultAliasIfEmpty(alias, getDefaultAliasServiceName())
    // Try removing both prefixed and unprefixed versions for migration support
    val ks = getKeyStoreAndLoad()

    try {
      if (ks.containsAlias(defaultAlias)) {
        ks.deleteEntry(defaultAlias)
      }
      // Try each possible prefix
      listOf(PREFIX_RSA, PREFIX_AES_GCM, PREFIX_AES_GCM_NO_AUTH, PREFIX_AES_CBC).forEach { prefix ->
        val prefixedAlias = getPrefixedAlias(defaultAlias, prefix)
        if (ks.containsAlias(prefixedAlias)) {
          ks.deleteEntry(prefixedAlias)
        }
      }
    } catch (ignored: GeneralSecurityException) {
      /* only one exception can be raised by code: 'KeyStore is not loaded' */
    }
  }

  override fun getAllKeys(): Set<String> {
    val ks = getKeyStoreAndLoad()
    try {
      val aliases = ks.aliases()
      // Strip prefixes when returning keys
      return HashSet(Collections.list(aliases).map { alias ->
        listOf(PREFIX_RSA, PREFIX_AES_GCM, PREFIX_AES_GCM_NO_AUTH, PREFIX_AES_CBC).forEach { prefix ->
          if (alias.startsWith("$prefix$PREFIX_DELIMITER")) {
            return@map alias.substring(prefix.length + PREFIX_DELIMITER.length)
          }
        }
        alias
      })
    } catch (e: KeyStoreException) {
      throw KeyStoreAccessException("Error accessing aliases in keystore $ks", e)
    }
  }

  // endregion

  // region Abstract methods

  /** Get encryption algorithm specification builder instance. */
  @Throws(GeneralSecurityException::class)
  protected abstract fun getKeyGenSpecBuilder(alias: String): KeyGenParameterSpec.Builder


  /** Get information about provided key. */
  @Throws(GeneralSecurityException::class)
  protected abstract fun getKeyInfo(key: Key): KeyInfo

  /** Try to generate key from provided specification. */
  @Throws(GeneralSecurityException::class)
  protected abstract fun generateKey(spec: KeyGenParameterSpec): Key

  /** Get name of the required encryption algorithm. */
  protected abstract fun getEncryptionAlgorithm(): String

  /** Get transformation algorithm for encrypt/decrypt operations. */
  protected abstract fun getEncryptionTransformation(): String

  // endregion

  // region Implementation

  /** Get cipher instance and cache it for any next call. */
  @Throws(NoSuchAlgorithmException::class, NoSuchPaddingException::class)
  fun getCachedInstance(): Cipher {
    return CipherCache.getCipher(getEncryptionTransformation())
  }

  /** Check requirements to the security level. */
  @Throws(CryptoFailedException::class)
  protected fun throwIfInsufficientLevel(level: SecurityLevel) {
    if (!securityLevel().satisfiesSafetyThreshold(level)) {
      throw CryptoFailedException(
        "Insufficient security level (wants $level; got ${securityLevel()})"
      )
    }
  }

  /** Extract existing key or generate a new one. In case of problems raise exception. */
  @Throws(GeneralSecurityException::class)
  protected fun extractGeneratedKey(
    safeAlias: String,
    level: SecurityLevel,
    retries: AtomicInteger
  ): Key {
    var key: Key?
    do {
      val keyStore = getKeyStoreAndLoad()
      // Check if the key exists
      if (!keyStore.containsAlias(safeAlias)) {
        // Key does not exist, generate a new one
        generateKeyAndStoreUnderAlias(safeAlias, level)
      } else {
        // Key exists, check if it's compatible
        key = keyStore.getKey(safeAlias, null)
        if (key != null && !isKeyAlgorithmSupported(
            key,
            getEncryptionAlgorithm()
          )
        ) {
          Log.w(
            LOG_TAG,
            "Incompatible key found for alias: $safeAlias. Expected cipher: ${getEncryptionTransformation()}. " +
              "This can happen if you try to overwrite credentials that were previously saved with a different encryption algorithm."
          )
          // Key is not compatible, delete it
          keyStore.deleteEntry(safeAlias)
          // Generate a new compatible key
          generateKeyAndStoreUnderAlias(safeAlias, level)
          key = null // Set key to null to retry the loop
          continue
        }
      }

      // Attempt to retrieve the key
      key = extractKey(keyStore, safeAlias, retries)
    } while (key == null)

    return key
  }

  /**
   * Try to extract key by alias from keystore, in case of 'known android bug' reduce retry counter.
   */
  @Throws(GeneralSecurityException::class)
  protected fun extractKey(keyStore: KeyStore, safeAlias: String, retry: AtomicInteger): Key? {
    val key: Key?

    try {
      key = keyStore.getKey(safeAlias, null)
    } catch (ex: UnrecoverableKeyException) {
      // try one more time
      if (retry.getAndDecrement() > 0) {
        keyStore.deleteEntry(safeAlias)
        return null
      }
      throw ex
    }

    // null if the given alias does not exist or does not identify a key-related entry.
    if (key == null) {
      throw KeyStoreAccessException("Empty key extracted!")
    }

    return key
  }

  /**
   * Try to extract key, first with prefix then fallback to legacy format
   */
  @Throws(GeneralSecurityException::class) 
  protected fun extractKeyWithMigration(
      alias: String,
      prefix: String,
      handler: ResultHandler,
      level: SecurityLevel,
      retries: AtomicInteger
  ): Key {
      val prefixedAlias = getPrefixedAlias(alias, prefix)
      val keyStore = getKeyStoreAndLoad()

      // First try prefixed alias
      if (keyStore.containsAlias(prefixedAlias)) {
          return extractKey(keyStore, prefixedAlias, retries) ?: 
              throw KeyStoreAccessException("Failed to extract key for $prefixedAlias")
      }

      // Try legacy alias
      if (keyStore.containsAlias(alias)) {
          val key = extractKey(keyStore, alias, retries)
          if (key != null) {
              // Migrate to new format
              migrateLegacyKey(alias, prefixedAlias, keyStore, handler, level)
              return key
          }
      }

      // No existing key found, create new one with prefix
      generateKeyAndStoreUnderAlias(prefixedAlias, level)
      return extractKey(keyStore, prefixedAlias, retries) ?:
          throw KeyStoreAccessException("Failed to generate key for $prefixedAlias")
  }

  /** Verify that provided key satisfy minimal needed level. */
  @Throws(GeneralSecurityException::class)
  protected fun validateKeySecurityLevel(level: SecurityLevel, key: Key): Boolean {
    return getSecurityLevel(key).satisfiesSafetyThreshold(level)
  }

  /** Get the supported level of security for provided Key instance. */
  @Throws(GeneralSecurityException::class)
  protected fun getSecurityLevel(key: Key): SecurityLevel {
    val keyInfo = getKeyInfo(key)

    val insideSecureHardware = keyInfo.isInsideSecureHardware

    if (insideSecureHardware) {
      return SecurityLevel.SECURE_HARDWARE
    }


    return SecurityLevel.SECURE_SOFTWARE
  }

  /** Load key store. */
  @Throws(KeyStoreAccessException::class)
  fun getKeyStoreAndLoad(): KeyStore {
    if (cachedKeyStore == null) {
      synchronized(this) {
        if (cachedKeyStore == null) {
          // initialize instance
          try {
            val keyStore = KeyStore.getInstance(KEYSTORE_TYPE)
            keyStore.load(null)
            cachedKeyStore = keyStore
          } catch (fail: Throwable) {
            throw KeyStoreAccessException("Could not access Keystore", fail)
          }
        }
      }
    }
    return cachedKeyStore!!
  }

  /** Default encryption with cipher without initialization vector. */
  @Throws(IOException::class, GeneralSecurityException::class)
  open fun encryptString(key: Key, value: String): ByteArray {
    return encryptString(key, value, Defaults.encrypt)
  }

  /** Default decryption with cipher without initialization vector. */
  @Throws(IOException::class, GeneralSecurityException::class)
  open fun decryptBytes(key: Key, bytes: ByteArray): String {
    return decryptBytes(key, bytes, Defaults.decrypt)
  }

  /** Encrypt provided string value. */
  @Throws(IOException::class, GeneralSecurityException::class)
  protected fun encryptString(
    key: Key,
    value: String,
    handler: EncryptStringHandler?
  ): ByteArray {
    val cipher = getCachedInstance()
    try {
      ByteArrayOutputStream().use { output ->
        if (handler != null) {
          handler.initialize(cipher, key, output)
          output.flush()
        }

        CipherOutputStream(output, cipher).use { encrypt ->
          encrypt.write(
            value.toByteArray(
              UTF8
            )
          )
        }

        return output.toByteArray()
      }
    } catch (fail: Throwable) {
      Log.e(LOG_TAG, fail.message, fail)
      throw fail
    }
  }

  /** Decrypt provided bytes to a string. */
  @SuppressLint("NewApi")
  @Throws(GeneralSecurityException::class, IOException::class, CryptoFailedException::class)
  protected open fun decryptBytes(
    key: Key,
    bytes: ByteArray,
    handler: DecryptBytesHandler?
  ): String {
    val cipher = getCachedInstance()
    try {
      ByteArrayInputStream(bytes).use { input ->
        ByteArrayOutputStream().use { output ->
          handler?.initialize(cipher, key, input)

          try {
            val decrypted = cipher.doFinal(input.readBytes())
            output.write(decrypted)
          } catch (e: Exception) {
            when {
              e is UserNotAuthenticatedException -> throw e
              e.cause is android.security.KeyStoreException &&
                e.cause?.message?.contains("Key user not authenticated") == true -> {
                throw UserNotAuthenticatedException()
              }
              e is javax.crypto.AEADBadTagException -> {
                throw CryptoFailedException(
                  "Decryption failed: Authentication tag verification failed. " +
                  "This usually indicates that the encrypted data was modified, corrupted, " +
                  "or is being decrypted with the wrong key.",
                  e
                )
              }
              else -> throw e
            }
          }

          return String(output.toByteArray(), UTF8)
        }
      }
    } catch (fail: Throwable) {
      Log.w(LOG_TAG, fail.message, fail)
      throw fail
    }
  }

  /** Get the most secured keystore */
  @Throws(GeneralSecurityException::class)
  fun generateKeyAndStoreUnderAlias(alias: String, requiredLevel: SecurityLevel) {
    // Firstly, try to generate the key as safe as possible (strongbox).
    // see https://developer.android.com/training/articles/keystore#HardwareSecurityModule

    var secretKey: Key? = null
    val supportsSecureHardware = DeviceAvailability.isStrongboxAvailable(applicationContext)

    if (supportsSecureHardware) {
      try {
        secretKey = tryGenerateStrongBoxSecurityKey(alias)
      } catch (ex: GeneralSecurityException) {
        Log.w(LOG_TAG, "StrongBox security storage is not available.", ex)
      } catch (ex: ProviderException) {
        Log.w(LOG_TAG, "StrongBox security storage is not available.", ex)
      }
    }

    // If that is not possible, we generate the key in a regular way
    // (it still might be generated in hardware, but not in StrongBox)
    if (secretKey == null || !supportsSecureHardware) {
      try {
        secretKey = tryGenerateRegularSecurityKey(alias)
      } catch (fail: GeneralSecurityException) {
        Log.e(LOG_TAG, "Regular security storage is not available.", fail)
        throw fail
      }
    }

    if (!validateKeySecurityLevel(requiredLevel, secretKey!!)) {
      throw CryptoFailedException("Cannot generate keys with required security guarantees")
    }
  }

  @Throws(GeneralSecurityException::class)
  protected fun isKeyAlgorithmSupported(
    key: Key,
    expectedAlgorithm: String
  ): Boolean {
    if (!key.algorithm.equals(expectedAlgorithm, ignoreCase = true)) {
      return false
    }

    try {
      val keyInfo = getKeyInfo(key)
      val blockModes = keyInfo.blockModes
      if (keyInfo.isUserAuthenticationRequired != isAuthSupported()) {
        return false
      }
      val expectedBlockMode = getEncryptionTransformation()
        .split("/")[1] // Split "AES/GCM/NoPadding" and get "GCM"
      return blockModes.any { mode ->
        mode.equals(expectedBlockMode, ignoreCase = true)
      }
    } catch (e: GeneralSecurityException) {
      Log.w(LOG_TAG, "Failed to check cipher configuration: ${e.message}")
      return false
    }
  }


  @Throws(GeneralSecurityException::class)
  protected fun tryGenerateRegularSecurityKey(alias: String): Key {
    val specification = getKeyGenSpecBuilder(alias).build()
    return generateKey(specification)
  }


  @Throws(GeneralSecurityException::class)
  protected fun tryGenerateStrongBoxSecurityKey(alias: String): Key {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      throw KeyStoreAccessException(
        "Strong box security keystore is only supported on Android 12 (API level 31) or higher. Current API level: ${Build.VERSION.SDK_INT}."
      )
    }

    val specification =
      getKeyGenSpecBuilder(alias).setIsStrongBoxBacked(true).build()
    return generateKey(specification)
  }

  // endregion

  // region Nested declarations

  /** Generic cipher initialization. */
  object Defaults {
    val encrypt = EncryptStringHandler { cipher, key, _ ->
      cipher.init(Cipher.ENCRYPT_MODE, key)
    }
    val decrypt = DecryptBytesHandler { cipher, key, _ ->
      cipher.init(Cipher.DECRYPT_MODE, key)
    }
  }

  /** Handler for storing cipher configuration in output stream. */
  fun interface EncryptStringHandler {
    @Throws(GeneralSecurityException::class, IOException::class)
    fun initialize(cipher: Cipher, key: Key, output: OutputStream)
  }

  /** Handler for configuring cipher by initialization data from input stream. */
  fun interface DecryptBytesHandler {
    @Throws(GeneralSecurityException::class, IOException::class)
    fun initialize(cipher: Cipher, key: Key, input: InputStream)
  }

  // endregion
}
