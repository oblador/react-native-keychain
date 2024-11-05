package com.oblador.keychain.cipherStorage

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.oblador.keychain.SecurityLevel
import com.oblador.keychain.cipherStorage.CipherStorageBase.DecryptBytesHandler
import com.oblador.keychain.cipherStorage.CipherStorageBase.EncryptStringHandler
import com.oblador.keychain.exceptions.CryptoFailedException
import com.oblador.keychain.exceptions.KeyStoreAccessException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec


@Suppress("unused", "MemberVisibilityCanBePrivate", "UnusedReturnValue")
abstract class CipherStorageBase(protected val applicationContext: Context) : CipherStorage {
  // region Constants
  /** Logging tag. */
  protected val LOG_TAG = CipherStorageBase::class.java.simpleName

  /** Default key storage type/name. */
  companion object {
    const val KEYSTORE_TYPE = "AndroidKeyStore"

    /** Key used for testing storage capabilities. */
    const val TEST_KEY_ALIAS = "$KEYSTORE_TYPE#supportsSecureHardware"

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
  }

  // endregion

  // region Members
  /** Guard object for [isSupportsSecureHardware] field. */
  protected val _sync = Any()

  /** Try to resolve support of the StrongBox feature. */
  protected val isStrongboxAvailable: Boolean by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
    } else {
      false
    }
  }

  /** Try to resolve it only once and cache result for all future calls. */
  @Transient
  protected var isSupportsSecureHardware: AtomicBoolean? = null

  /** Get cached instance of cipher. Get instance operation is slow. */
  @Transient
  protected var cachedCipher: Cipher? = null

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

    return (1000 * if (isBiometrySupported()) 1 else 0) + (getMinSupportedApiLevel())
  }

  /** Try device capabilities by creating temporary key in keystore. */
  override fun supportsSecureHardware(): Boolean {
    if (isSupportsSecureHardware != null) return isSupportsSecureHardware!!.get()

    synchronized(_sync) {
      // double check pattern in use
      if (isSupportsSecureHardware != null) return isSupportsSecureHardware!!.get()

      isSupportsSecureHardware = AtomicBoolean(false)

      var sdk: SelfDestroyKey? = null

      try {
        sdk = SelfDestroyKey(TEST_KEY_ALIAS)
        val newValue = validateKeySecurityLevel(SecurityLevel.SECURE_HARDWARE, sdk.key)
        isSupportsSecureHardware!!.set(newValue)
      } catch (ignored: Throwable) {
      } finally {
        sdk?.close()
      }
    }

    return isSupportsSecureHardware!!.get()
  }

  /** {@inheritDoc} */
  override fun getDefaultAliasServiceName(): String {
    return getCipherStorageName()
  }

  /** Remove key with provided name from security storage. */
  override fun removeKey(alias: String) {
    val safeAlias = getDefaultAliasIfEmpty(alias, getDefaultAliasServiceName())
    val ks = getKeyStoreAndLoad()

    try {
      if (ks.containsAlias(safeAlias)) {
        ks.deleteEntry(safeAlias)
      }
    } catch (ignored: GeneralSecurityException) {
      /* only one exception can be raised by code: 'KeyStore is not loaded' */
    }
  }

  override fun getAllKeys(): Set<String> {
    val ks = getKeyStoreAndLoad()
    try {
      val aliases = ks.aliases()
      return HashSet(Collections.list(aliases))
    } catch (e: KeyStoreException) {
      throw KeyStoreAccessException("Error accessing aliases in keystore $ks", e)
    }
  }

  // endregion

  // region Abstract methods

  /** Get encryption algorithm specification builder instance. */
  @Throws(GeneralSecurityException::class)
  protected abstract fun getKeyGenSpecBuilder(alias: String): KeyGenParameterSpec.Builder

  @Throws(GeneralSecurityException::class)
  protected abstract fun getKeyGenSpecBuilder(
    alias: String,
    isForTesting: Boolean
  ): KeyGenParameterSpec.Builder

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
    if (cachedCipher == null) {
      synchronized(this) {
        if (cachedCipher == null) {
          cachedCipher = Cipher.getInstance(getEncryptionTransformation())
        }
      }
    }
    return cachedCipher!!
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
        if (key != null && !isKeyAlgorithmSupported(key, getEncryptionAlgorithm())) {
          Log.w(
            LOG_TAG,
            "Incompatible key found for alias: $safeAlias. Expected: ${getEncryptionAlgorithm()}, Found: ${key.algorithm}." +
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

  /** Verify that provided key satisfy minimal needed level. */
  @Throws(GeneralSecurityException::class)
  protected fun validateKeySecurityLevel(level: SecurityLevel, key: Key): Boolean {
    return getSecurityLevel(key).satisfiesSafetyThreshold(level)
  }

  /** Get the supported level of security for provided Key instance. */
  @Throws(GeneralSecurityException::class)
  protected fun getSecurityLevel(key: Key): SecurityLevel {
    val keyInfo = getKeyInfo(key)

    // lower API23 we don't have any hardware support
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val insideSecureHardware = keyInfo.isInsideSecureHardware

      if (insideSecureHardware) {
        return SecurityLevel.SECURE_HARDWARE
      }
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
  protected fun encryptString(key: Key, value: String, handler: EncryptStringHandler?): ByteArray {
    val cipher = getCachedInstance()
    try {
      ByteArrayOutputStream().use { output ->
        // write initialization vector to the beginning of the stream
        if (handler != null) {
          handler.initialize(cipher, key, output)
          output.flush()
        }

        CipherOutputStream(output, cipher).use { encrypt -> encrypt.write(value.toByteArray(UTF8)) }

        return output.toByteArray()
      }
    } catch (fail: Throwable) {
      Log.e(LOG_TAG, fail.message, fail)
      throw fail
    }
  }

  /** Decrypt provided bytes to a string. */
  @Throws(GeneralSecurityException::class, IOException::class)
  protected open fun decryptBytes(
    key: Key,
    bytes: ByteArray,
    handler: DecryptBytesHandler?
  ): String {
    val cipher = getCachedInstance()
    try {
      ByteArrayInputStream(bytes).use { input ->
        ByteArrayOutputStream().use { output ->
          // read the initialization vector from the beginning of the stream
          if (handler != null) {
            handler.initialize(cipher, key, input)
          }

          CipherInputStream(input, cipher).use { decrypt -> copy(decrypt, output) }

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

    if (isStrongboxAvailable) {
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
    if (secretKey == null || !isStrongboxAvailable) {
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
  protected fun isKeyAlgorithmSupported(key: Key, expectedAlgorithm: String): Boolean {
    return key.algorithm.equals(expectedAlgorithm, ignoreCase = true)
  }

  /** Try to get secured keystore instance. */
  @Throws(GeneralSecurityException::class)
  protected fun tryGenerateRegularSecurityKey(alias: String): Key {
    return tryGenerateRegularSecurityKey(alias, false)
  }

  @Throws(GeneralSecurityException::class)
  protected fun tryGenerateRegularSecurityKey(alias: String, isForTesting: Boolean): Key {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      throw KeyStoreAccessException(
        "Regular security keystore is not supported for old API${Build.VERSION.SDK_INT}."
      )
    }

    val specification = getKeyGenSpecBuilder(alias, isForTesting).build()
    return generateKey(specification)
  }

  /** Try to get strong secured keystore instance. (StrongBox security chip) */
  @Throws(GeneralSecurityException::class)
  protected fun tryGenerateStrongBoxSecurityKey(alias: String): Key {
    return tryGenerateStrongBoxSecurityKey(alias, false)
  }

  @Throws(GeneralSecurityException::class)
  protected fun tryGenerateStrongBoxSecurityKey(alias: String, isForTesting: Boolean): Key {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      throw KeyStoreAccessException(
        "Strong box security keystore is not supported for old API${Build.VERSION.SDK_INT}."
      )
    }

    val specification = getKeyGenSpecBuilder(alias, isForTesting).setIsStrongBoxBacked(true).build()
    return generateKey(specification)
  }

  // endregion

  // region Testing

  /** Override internal cipher instance cache. */
  @VisibleForTesting
  fun setCipher(cipher: Cipher): CipherStorageBase {
    cachedCipher = cipher
    return this
  }

  /** Override the keystore instance cache. */
  @VisibleForTesting
  fun setKeyStore(keystore: KeyStore): CipherStorageBase {
    cachedKeyStore = keystore
    return this
  }

  // endregion

  // region Nested declarations

  /** Generic cipher initialization. */
  object Defaults {
    val encrypt = EncryptStringHandler { cipher, key, output ->
      cipher.init(Cipher.ENCRYPT_MODE, key)
    }
    val decrypt = DecryptBytesHandler { cipher, key, input ->
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

  /** Auto remove keystore key. */
  inner class SelfDestroyKey(val name: String, val key: Key) : Closeable {

    @Throws(GeneralSecurityException::class)
    constructor(name: String) : this(name, tryGenerateRegularSecurityKey(name, true))

    override fun close() {
      try {
        removeKey(name)
      } catch (ex: KeyStoreAccessException) {
        Log.w(LOG_TAG, "AutoClose remove key failed. Error: ${ex.message}", ex)
      }
    }
  }
  // endregion
}
