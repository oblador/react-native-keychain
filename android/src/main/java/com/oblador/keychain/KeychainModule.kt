package com.oblador.keychain

import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.annotation.StringDef
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt.PromptInfo
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.module.annotations.ReactModule
import com.oblador.keychain.cipherStorage.CipherStorage
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionResult
import com.oblador.keychain.cipherStorage.CipherStorageBase
import com.oblador.keychain.cipherStorage.CipherStorageFacebookConceal
import com.oblador.keychain.cipherStorage.CipherStorageKeystoreAesCbc
import com.oblador.keychain.cipherStorage.CipherStorageKeystoreAesGcm
import com.oblador.keychain.cipherStorage.CipherStorageKeystoreRsaEcb
import com.oblador.keychain.resultHandler.ResultHandler
import com.oblador.keychain.resultHandler.ResultHandlerProvider
import com.oblador.keychain.exceptions.CryptoFailedException
import com.oblador.keychain.exceptions.EmptyParameterException
import com.oblador.keychain.exceptions.KeyStoreAccessException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@ReactModule(name = KeychainModule.KEYCHAIN_MODULE)
@Suppress("unused")
class KeychainModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {
  @StringDef(
      AccessControl.NONE,
      AccessControl.USER_PRESENCE,
      AccessControl.BIOMETRY_ANY,
      AccessControl.BIOMETRY_CURRENT_SET,
      AccessControl.DEVICE_PASSCODE,
      AccessControl.APPLICATION_PASSWORD,
      AccessControl.BIOMETRY_ANY_OR_DEVICE_PASSCODE,
      AccessControl.BIOMETRY_CURRENT_SET_OR_DEVICE_PASSCODE)
  internal annotation class AccessControl {
    companion object {
      const val NONE = "None"
      const val USER_PRESENCE = "UserPresence"
      const val BIOMETRY_ANY = "BiometryAny"
      const val BIOMETRY_CURRENT_SET = "BiometryCurrentSet"
      const val DEVICE_PASSCODE = "DevicePasscode"
      const val APPLICATION_PASSWORD = "ApplicationPassword"
      const val BIOMETRY_ANY_OR_DEVICE_PASSCODE = "BiometryAnyOrDevicePasscode"
      const val BIOMETRY_CURRENT_SET_OR_DEVICE_PASSCODE = "BiometryCurrentSetOrDevicePasscode"
    }
  }

  internal annotation class AuthPromptOptions {
    companion object {
      const val TITLE = "title"
      const val SUBTITLE = "subtitle"
      const val DESCRIPTION = "description"
      const val CANCEL = "cancel"
    }
  }

  /** Options mapping keys. */
  internal annotation class Maps {
    companion object {
      const val ACCESS_CONTROL = "accessControl"
      const val ACCESS_GROUP = "accessGroup"
      const val ACCESSIBLE = "accessible"
      const val AUTH_PROMPT = "authenticationPrompt"
      const val AUTH_TYPE = "authenticationType"
      const val SERVICE = "service"
      const val SERVER = "server"
      const val SECURITY_LEVEL = "securityLevel"
      const val RULES = "rules"
      const val USERNAME = "username"
      const val PASSWORD = "password"
      const val STORAGE = "storage"
    }
  }

  /** Known error codes. */
  internal annotation class Errors {
    companion object {
      const val E_EMPTY_PARAMETERS = "E_EMPTY_PARAMETERS"
      const val E_CRYPTO_FAILED = "E_CRYPTO_FAILED"
      const val E_KEYSTORE_ACCESS_ERROR = "E_KEYSTORE_ACCESS_ERROR"
      const val E_SUPPORTED_BIOMETRY_ERROR = "E_SUPPORTED_BIOMETRY_ERROR"

      /** Raised for unexpected errors. */
      const val E_UNKNOWN_ERROR = "E_UNKNOWN_ERROR"
    }
  }

  /** Supported ciphers. */
  @StringDef(KnownCiphers.FB, KnownCiphers.AES_CBC, KnownCiphers.AES_GCM, KnownCiphers.RSA)
  annotation class KnownCiphers {
    companion object {
      /** Facebook conceal compatibility lib in use. */
      const val FB = "FacebookConceal"

      /** AES CBC encryption. */
      const val AES_CBC = "KeystoreAESCBC"

      /** Biometric Auth + AES GCM encryption. */
      const val AES_GCM = "KeystoreAESGCM"

      /** AES GCM encryption. */
      const val AES_GCM_NO_AUTH = "KeystoreAESGCM_NoAuth"

      /** Biometric Auth + RSA ECB encryption */
      const val RSA = "KeystoreRSAECB"
    }
  }

  /** Secret manipulation rules. */
  @StringDef(Rules.AUTOMATIC_UPGRADE, Rules.NONE)
  internal annotation class Rules {
    companion object {
      const val NONE = "none"
      const val AUTOMATIC_UPGRADE = "automaticUpgradeToMoreSecuredStorage"
    }
  }

  // endregion
  // region Members
  /** Name-to-instance lookup map. */
  private val cipherStorageMap: MutableMap<String, CipherStorage> = HashMap()

  /** Shared preferences storage. */
  private val prefsStorage: PrefsStorageBase

  /** Launches a coroutine to perform non-blocking UI operations */
  private val coroutineScope = CoroutineScope(Dispatchers.Default)

  /** Mutex to prevent concurrent calls to Cipher, which doesn't support multi-threading */
  private val mutex = Mutex()

  // endregion
  // region Initialization
  /** Default constructor. */
  init {
    prefsStorage = DataStorePrefsStorage(reactContext, coroutineScope)
    addCipherStorageToMap(CipherStorageFacebookConceal(reactContext))
    addCipherStorageToMap(CipherStorageKeystoreAesCbc(reactContext))
    addCipherStorageToMap(CipherStorageKeystoreAesGcm(reactContext, false))
    addCipherStorageToMap(CipherStorageKeystoreAesGcm(reactContext, true))

    // we have a references to newer api that will fail load of app classes in old androids OS
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      addCipherStorageToMap(CipherStorageKeystoreRsaEcb(reactContext))
    }
  }

  /** cipher (crypto api) warming up logic. force java load classes and initialization. */
  private fun internalWarmingBestCipher() {
    try {
      val startTime = System.nanoTime()
      Log.v(KEYCHAIN_MODULE, "warming up started at $startTime")
      val best = cipherStorageForCurrentAPILevel as CipherStorageBase
      val instance = best.getCachedInstance()
      val isSecure = best.supportsSecureHardware()
      val requiredLevel =
          if (isSecure) SecurityLevel.SECURE_HARDWARE else SecurityLevel.SECURE_SOFTWARE
      best.generateKeyAndStoreUnderAlias(WARMING_UP_ALIAS, requiredLevel)
      best.getKeyStoreAndLoad()
      Log.v(
          KEYCHAIN_MODULE,
          "warming up takes: " +
              TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) +
              " ms")
    } catch (ex: Throwable) {
      Log.e(KEYCHAIN_MODULE, "warming up failed!", ex)
    }
  }

  // endregion
  // region Overrides
  /** {@inheritDoc} */
  override fun getName(): String {
    return KEYCHAIN_MODULE
  }

  override fun invalidate() {
    super.invalidate()
    if (coroutineScope.isActive) {
      coroutineScope.cancel("$KEYCHAIN_MODULE has been destroyed.")
    }
  }

  /** {@inheritDoc} */
  override fun getConstants(): Map<String, Any> {
    val constants: MutableMap<String, Any> = HashMap()
    constants[SecurityLevel.ANY.jsName()] = SecurityLevel.ANY.name
    constants[SecurityLevel.SECURE_SOFTWARE.jsName()] = SecurityLevel.SECURE_SOFTWARE.name
    constants[SecurityLevel.SECURE_HARDWARE.jsName()] = SecurityLevel.SECURE_HARDWARE.name
    return constants
  }
  // endregion

  // region React Methods
  private fun setGenericPassword(
      alias: String,
      username: String,
      password: String,
      options: ReadableMap?,
      promise: Promise
  ) {
    coroutineScope.launch {
      try {
        throwIfEmptyLoginPassword(username, password)
        val level = getSecurityLevelOrDefault(options)
        val storage = getSelectedStorage(options)
        throwIfInsufficientLevel(storage, level)
        val promptInfo = getPromptInfo(options)
        val result = mutex.withLock { encryptToResult(alias, storage, username, password, level, promptInfo) }
        prefsStorage.storeEncryptedEntry(alias, result)
        val results = Arguments.createMap()
        results.putString(Maps.SERVICE, alias)
        results.putString(Maps.STORAGE, storage.getCipherStorageName())
        promise.resolve(results)
      } catch (e: EmptyParameterException) {
        Log.e(KEYCHAIN_MODULE, e.message, e)
        promise.reject(Errors.E_EMPTY_PARAMETERS, e)
      } catch (e: CryptoFailedException) {
        Log.e(KEYCHAIN_MODULE, e.message, e)
        promise.reject(Errors.E_CRYPTO_FAILED, e)
      } catch (fail: Throwable) {
        Log.e(KEYCHAIN_MODULE, fail.message, fail)
        promise.reject(Errors.E_UNKNOWN_ERROR, fail)
      }
    }
  }

  @ReactMethod
  fun setGenericPasswordForOptions(
      options: ReadableMap?,
      username: String,
      password: String,
      promise: Promise
  ) {
    val service = getServiceOrDefault(options)
    setGenericPassword(service, username, password, options, promise)
  }

  /** Get Cipher storage instance based on user provided options. */
  @Throws(CryptoFailedException::class)
  private fun getSelectedStorage(options: ReadableMap?): CipherStorage {
    val accessControl = getAccessControlOrDefault(options)
    val useBiometry = getUseBiometry(accessControl)
    val cipherName = getSpecificStorageOrDefault(options)
    var result: CipherStorage? = null
    if (null != cipherName) {
      result = getCipherStorageByName(cipherName)
    }

    // attempt to access none existing storage will force fallback logic.
    if (null == result) {
      result = getCipherStorageForCurrentAPILevel(useBiometry)
    }
    return result
  }

  private fun getGenericPassword(alias: String, options: ReadableMap?, promise: Promise) {
    coroutineScope.launch {
      try {
        val resultSet = prefsStorage.getEncryptedEntry(alias)
        if (resultSet == null) {
          Log.e(KEYCHAIN_MODULE, "No entry found for service: $alias")
          promise.resolve(false)
          return@launch
        }
        val storageName = resultSet.cipherStorageName
        val rules = getSecurityRulesOrDefault(options)
        val promptInfo = getPromptInfo(options)
        var cipher: CipherStorage? = null

        // Only check for upgradable ciphers for FacebookConseal as that
        // is the only cipher that can be upgraded
        cipher =
          if (rules == Rules.AUTOMATIC_UPGRADE && storageName == KnownCiphers.FB) {
            // get the best storage
            val accessControl = getAccessControlOrDefault(options)
            val useBiometry = getUseBiometry(accessControl)
            getCipherStorageForCurrentAPILevel(useBiometry)
          } else {
            getCipherStorageByName(storageName)
          }
        val decryptionResult = mutex.withLock { decryptCredentials(alias, cipher!!, resultSet, rules, promptInfo) }
        val credentials = Arguments.createMap()
        credentials.putString(Maps.SERVICE, alias)
        credentials.putString(Maps.USERNAME, decryptionResult.username)
        credentials.putString(Maps.PASSWORD, decryptionResult.password)
        credentials.putString(Maps.STORAGE, cipher?.getCipherStorageName())
        promise.resolve(credentials)
      } catch (e: KeyStoreAccessException) {
        Log.e(KEYCHAIN_MODULE, e.message!!)
        promise.reject(Errors.E_KEYSTORE_ACCESS_ERROR, e)
      } catch (e: CryptoFailedException) {
        Log.e(KEYCHAIN_MODULE, e.message!!)
        promise.reject(Errors.E_CRYPTO_FAILED, e)
      } catch (fail: Throwable) {
        Log.e(KEYCHAIN_MODULE, fail.message, fail)
        promise.reject(Errors.E_UNKNOWN_ERROR, fail)
      }
    }
  }

  @ReactMethod
  fun getAllGenericPasswordServices(promise: Promise) {
    try {
      val services = doGetAllGenericPasswordServices()
      promise.resolve(Arguments.makeNativeArray<Any>(services.toTypedArray()))
    } catch (e: KeyStoreAccessException) {
      promise.reject(Errors.E_KEYSTORE_ACCESS_ERROR, e)
    }
  }

  @Throws(KeyStoreAccessException::class)
  private fun doGetAllGenericPasswordServices(): Collection<String> {
    val cipherNames = prefsStorage.usedCipherNames
    val ciphers: MutableCollection<CipherStorage?> = ArrayList(cipherNames.size)
    for (storageName in cipherNames) {
      val cipherStorage = getCipherStorageByName(storageName!!)
      ciphers.add(cipherStorage)
    }
    val result: MutableSet<String> = HashSet()
    for (cipher in ciphers) {
      val aliases = cipher!!.getAllKeys()
      for (alias in aliases) {
        if (alias != WARMING_UP_ALIAS) {
          result.add(alias)
        }
      }
    }
    return result
  }

  @ReactMethod
  fun getGenericPasswordForOptions(options: ReadableMap?, promise: Promise) {
    val service = getServiceOrDefault(options)
    getGenericPassword(service, options, promise)
  }

  private fun resetGenericPassword(alias: String, promise: Promise) {
    try {
      // First we clean up the cipher storage (using the cipher storage that was used to store the
      // entry)
      val resultSet = prefsStorage.getEncryptedEntry(alias)
      if (resultSet != null) {
        val cipherStorage = getCipherStorageByName(resultSet.cipherStorageName)
        cipherStorage?.removeKey(alias)
      }
      // And then we remove the entry in the shared preferences
      prefsStorage.removeEntry(alias)
      promise.resolve(true)
    } catch (e: KeyStoreAccessException) {
      Log.e(KEYCHAIN_MODULE, e.message!!)
      promise.reject(Errors.E_KEYSTORE_ACCESS_ERROR, e)
    } catch (fail: Throwable) {
      Log.e(KEYCHAIN_MODULE, fail.message, fail)
      promise.reject(Errors.E_UNKNOWN_ERROR, fail)
    }
  }

  @ReactMethod
  fun resetGenericPasswordForOptions(options: ReadableMap?, promise: Promise) {
    val service = getServiceOrDefault(options)
    resetGenericPassword(service, promise)
  }

  @ReactMethod
  fun hasInternetCredentialsForOptions(options: ReadableMap, promise: Promise) {
    val server = options.getString(Maps.SERVER)
    val alias = getAliasOrDefault(server)
    val resultSet = prefsStorage.getEncryptedEntry(alias)
    if (resultSet == null) {
      Log.e(KEYCHAIN_MODULE, "No entry found for service: $alias")
      promise.resolve(false)
      return
    }
    promise.resolve(true)
  }

  @ReactMethod
  fun hasGenericPasswordForOptions(options: ReadableMap?, promise: Promise) {
    val service = getServiceOrDefault(options)
    val resultSet = prefsStorage.getEncryptedEntry(service)
    if (resultSet == null) {
      Log.e(KEYCHAIN_MODULE, "No entry found for service: $service")
      promise.resolve(false)
      return
    }
    promise.resolve(true)
  }

  @ReactMethod
  fun setInternetCredentialsForServer(
      server: String,
      username: String,
      password: String,
      options: ReadableMap?,
      promise: Promise
  ) {
    setGenericPassword(server, username, password, options, promise)
  }

  @ReactMethod
  fun getInternetCredentialsForServer(server: String, options: ReadableMap?, promise: Promise) {
    getGenericPassword(server, options, promise)
  }

  @ReactMethod
  fun resetInternetCredentialsForOptions(options: ReadableMap, promise: Promise) {
    val server = options.getString(Maps.SERVER)
    val alias = getAliasOrDefault(server)
    resetGenericPassword(alias, promise)
  }

  @ReactMethod
  fun getSupportedBiometryType(promise: Promise) {
    try {
      var reply: String? = null
      if (!DeviceAvailability.isStrongBiometricAuthAvailable(reactApplicationContext)) {
        reply = null
      } else {
        if (isFingerprintAuthAvailable) {
          reply = FINGERPRINT_SUPPORTED_NAME
        } else if (isFaceAuthAvailable) {
          reply = FACE_SUPPORTED_NAME
        } else if (isIrisAuthAvailable) {
          reply = IRIS_SUPPORTED_NAME
        }
      }
      promise.resolve(reply)
    } catch (e: Exception) {
      Log.e(KEYCHAIN_MODULE, e.message, e)
      promise.reject(Errors.E_SUPPORTED_BIOMETRY_ERROR, e)
    } catch (fail: Throwable) {
      Log.e(KEYCHAIN_MODULE, fail.message, fail)
      promise.reject(Errors.E_UNKNOWN_ERROR, fail)
    }
  }

  @ReactMethod
  fun getSecurityLevel(options: ReadableMap?, promise: Promise) {
    val accessControl = getAccessControlOrDefault(options)
    val useBiometry = getUseBiometry(accessControl)
    promise.resolve(getSecurityLevel(useBiometry).name)
  }

  private fun addCipherStorageToMap(cipherStorage: CipherStorage) {
    cipherStorageMap[cipherStorage.getCipherStorageName()] = cipherStorage
  }

  /**
   * Extract credentials from current storage. In case if current storage is not matching results
   * set then executed migration.
   */
  @Throws(CryptoFailedException::class, KeyStoreAccessException::class)
  private fun decryptCredentials(
      alias: String,
      current: CipherStorage,
      resultSet: PrefsStorageBase.ResultSet,
      @Rules rules: String,
      promptInfo: PromptInfo
  ): DecryptionResult {
    val storageName = resultSet.cipherStorageName

    // The encrypted data is encrypted using the current CipherStorage, so we just decrypt and
    // return
    if (storageName == current.getCipherStorageName()) {
      return decryptToResult(alias, current, resultSet, promptInfo)
    }

    // The encrypted data is encrypted using an older CipherStorage, so we need to decrypt the data
    // first,
    // then encrypt it using the current CipherStorage, then store it again and return
    val oldStorage =
        getCipherStorageByName(storageName)
            ?: throw KeyStoreAccessException(
                "Wrong cipher storage name '$storageName' or cipher not available")

    // decrypt using the older cipher storage
    val decryptionResult = decryptToResult(alias, oldStorage, resultSet, promptInfo)
    if (Rules.AUTOMATIC_UPGRADE == rules) {
      try {
        // encrypt using the current cipher storage
        migrateCipherStorage(alias, current, oldStorage, decryptionResult, promptInfo)
      } catch (e: CryptoFailedException) {
        Log.w(
            KEYCHAIN_MODULE, "Migrating to a less safe storage is not allowed. Keeping the old one")
      }
    }
    return decryptionResult
  }

  /** Try to decrypt with provided storage. */
  @Throws(CryptoFailedException::class)
  private fun decryptToResult(
    alias: String,
    storage: CipherStorage,
    resultSet: PrefsStorageBase.ResultSet,
    promptInfo: PromptInfo
  ): DecryptionResult {
    val handler = getInteractiveHandler(storage, promptInfo)
    storage.decrypt(handler, alias, resultSet.username!!, resultSet.password!!, SecurityLevel.ANY)
    CryptoFailedException.reThrowOnError(handler.error)
    if (null == handler.decryptionResult) {
      throw CryptoFailedException("No decryption results and no error. Something deeply wrong!")
    }
    return handler.decryptionResult!!
  }

  /** Try to encrypt with provided storage. */
  @Throws(CryptoFailedException::class)
  private fun encryptToResult(
    alias: String,
    storage: CipherStorage,
    username: String,
    password: String,
    securityLevel: SecurityLevel,
    promptInfo: PromptInfo
  ): CipherStorage.EncryptionResult {
    val handler = getInteractiveHandler(storage, promptInfo)
    storage.encrypt(handler, alias, username, password, securityLevel)
    CryptoFailedException.reThrowOnError(handler.error)
    if (null == handler.encryptionResult) {
      throw CryptoFailedException("No decryption results and no error. Something deeply wrong!")
    }
    return handler.encryptionResult!!
  }

  /** Get instance of handler that resolves access to the keystore on system request. */
  private fun getInteractiveHandler(
      current: CipherStorage,
      promptInfo: PromptInfo
  ): ResultHandler {
    val reactContext = reactApplicationContext
    return ResultHandlerProvider.getHandler(reactContext, current, promptInfo)
  }

  /** Remove key from old storage and add it to the new storage. */
  /* package */
  @Throws(
      KeyStoreAccessException::class, CryptoFailedException::class, IllegalArgumentException::class)
  fun migrateCipherStorage(
      service: String,
      newCipherStorage: CipherStorage,
      oldCipherStorage: CipherStorage,
      decryptionResult: DecryptionResult,
      promptInfo: PromptInfo
  ) {

    val username =
        decryptionResult.username ?: throw IllegalArgumentException("Username cannot be null")
    val password =
        decryptionResult.password ?: throw IllegalArgumentException("Password cannot be null")
    // don't allow to degrade security level when transferring, the new
    // storage should be as safe as the old one.
    val encryptionResult = encryptToResult(service, newCipherStorage, username, password, decryptionResult.getSecurityLevel(), promptInfo)

    // store the encryption result
    prefsStorage.storeEncryptedEntry(service, encryptionResult)

    // clean up the old cipher storage
    oldCipherStorage.removeKey(service)
  }

  @get:Throws(CryptoFailedException::class)
  val cipherStorageForCurrentAPILevel: CipherStorage
    /**
     * The "Current" CipherStorage is the cipherStorage with the highest API level that is lower
     * than or equal to the current API level
     */
    get() = getCipherStorageForCurrentAPILevel(true)

  /**
   * The "Current" CipherStorage is the cipherStorage with the highest API level that is lower than
   * or equal to the current API level. Parameter allow to reduce level.
   */
  @Throws(CryptoFailedException::class)
  fun /* package */ getCipherStorageForCurrentAPILevel(useBiometry: Boolean): CipherStorage {
    val currentApiLevel = Build.VERSION.SDK_INT
    val isBiometry =
        useBiometry && (isFingerprintAuthAvailable || isFaceAuthAvailable || isIrisAuthAvailable)
    var foundCipher: CipherStorage? = null
    for (variant in cipherStorageMap.values) {
      Log.d(KEYCHAIN_MODULE, "Probe cipher storage: " + variant.javaClass.simpleName)

      // Is the cipherStorage supported on the current API level?
      val minApiLevel = variant.getMinSupportedApiLevel()
      val capabilityLevel = variant.getCapabilityLevel()
      val isSupportedApi = minApiLevel <= currentApiLevel

      // API not supported
      if (!isSupportedApi) continue

      // Is the API level better than the one we previously selected (if any)?
      if (foundCipher != null && capabilityLevel < foundCipher.getCapabilityLevel()) continue

      // if biometric supported but not configured properly than skip
      if (variant.isBiometrySupported() && !isBiometry) continue

      // remember storage with the best capabilities
      foundCipher = variant
    }
    if (foundCipher == null) {
      throw CryptoFailedException("Unsupported Android SDK " + Build.VERSION.SDK_INT)
    }
    Log.d(KEYCHAIN_MODULE, "Selected storage: " + foundCipher.javaClass.simpleName)
    return foundCipher
  }

  /** Extract cipher by it unique name. [CipherStorage.getCipherStorageName]. */
  fun /* package */ getCipherStorageByName(@KnownCiphers knownName: String): CipherStorage? {
    return cipherStorageMap[knownName]
  }

  val isFingerprintAuthAvailable: Boolean
    /** True - if fingerprint hardware available and configured, otherwise false. */
    get() =
        DeviceAvailability.isStrongBiometricAuthAvailable(reactApplicationContext) &&
            DeviceAvailability.isFingerprintAuthAvailable(reactApplicationContext)

  val isFaceAuthAvailable: Boolean
    /** True - if face recognition hardware available and configured, otherwise false. */
    get() =
        DeviceAvailability.isStrongBiometricAuthAvailable(reactApplicationContext) &&
            DeviceAvailability.isFaceAuthAvailable(reactApplicationContext)

  val isIrisAuthAvailable: Boolean
    /** True - if iris recognition hardware available and configured, otherwise false. */
    get() =
        DeviceAvailability.isStrongBiometricAuthAvailable(reactApplicationContext) &&
            DeviceAvailability.isIrisAuthAvailable(reactApplicationContext)

  val isSecureHardwareAvailable: Boolean
    /** Is secured hardware a part of current storage or not. */
    get() =
        try {
          cipherStorageForCurrentAPILevel.supportsSecureHardware()
        } catch (e: CryptoFailedException) {
          false
        }

  /** Resolve storage to security level it provides. */
  private fun getSecurityLevel(useBiometry: Boolean): SecurityLevel {
    return try {
      val storage = getCipherStorageForCurrentAPILevel(useBiometry)
      if (!storage.securityLevel().satisfiesSafetyThreshold(SecurityLevel.SECURE_SOFTWARE)) {
        return SecurityLevel.ANY
      }
      if (storage.supportsSecureHardware()) {
        SecurityLevel.SECURE_HARDWARE
      } else SecurityLevel.SECURE_SOFTWARE
    } catch (e: CryptoFailedException) {
      Log.w(KEYCHAIN_MODULE, "Security Level Exception: " + e.message, e)
      SecurityLevel.ANY
    }
  }
  // endregion

  companion object {
    // region Constants
    const val KEYCHAIN_MODULE = "RNKeychainManager"
    const val FINGERPRINT_SUPPORTED_NAME = "Fingerprint"
    const val FACE_SUPPORTED_NAME = "Face"
    const val IRIS_SUPPORTED_NAME = "Iris"
    const val EMPTY_STRING = ""
    const val WARMING_UP_ALIAS = "warmingUp"
    private val LOG_TAG = KeychainModule::class.java.simpleName

    /** Allow initialization in chain. */
    fun withWarming(reactContext: ReactApplicationContext): KeychainModule {
      val instance = KeychainModule(reactContext)

      // force initialization of the crypto api in background thread
      val warmingUp = Thread({ instance.internalWarmingBestCipher() }, "keychain-warming-up")
      warmingUp.isDaemon = true
      warmingUp.start()
      return instance
    }

    // endregion
    // region Helpers
    /** Get service value from options. */
    private fun getServiceOrDefault(options: ReadableMap?): String {
      var service: String? = null
      if (null != options && options.hasKey(Maps.SERVICE)) {
        service = options.getString(Maps.SERVICE)
      }
      return getAliasOrDefault(service)
    }

    /** Get automatic secret manipulation rules, default: No upgrade. */
    @Rules
    private fun getSecurityRulesOrDefault(options: ReadableMap?): String {
      return getSecurityRulesOrDefault(options, Rules.NONE)
    }

    /** Get automatic secret manipulation rules. */
    @Rules
    private fun getSecurityRulesOrDefault(options: ReadableMap?, @Rules rule: String): String {
      var rules: String? = null
      if (null != options && options.hasKey(Maps.RULES)) {
        rules = options.getString(Maps.RULES)
      }
      return rules ?: rule
    }

    /** Extract user specified storage from options. */
    @KnownCiphers
    private fun getSpecificStorageOrDefault(options: ReadableMap?): String? {
      var storageName: String? = null
      if (null != options && options.hasKey(Maps.STORAGE)) {
        storageName = options.getString(Maps.STORAGE)
      }
      return storageName
    }

    /** Get access control value from options or fallback to [AccessControl.NONE]. */
    @AccessControl
    private fun getAccessControlOrDefault(options: ReadableMap?): String {
      return getAccessControlOrDefault(options, AccessControl.NONE)
    }

    /** Get access control value from options or fallback to default. */
    @AccessControl
    private fun getAccessControlOrDefault(
        options: ReadableMap?,
        @AccessControl fallback: String
    ): String {
      var accessControl: String? = null
      if (null != options && options.hasKey(Maps.ACCESS_CONTROL)) {
        accessControl = options.getString(Maps.ACCESS_CONTROL)
      }
      return accessControl ?: fallback
    }

    /** Get security level from options or fallback [SecurityLevel.ANY] value. */
    private fun getSecurityLevelOrDefault(options: ReadableMap?): SecurityLevel {
      return getSecurityLevelOrDefault(options, SecurityLevel.ANY.name)
    }

    /** Get security level from options or fallback to default value. */
    private fun getSecurityLevelOrDefault(options: ReadableMap?, fallback: String): SecurityLevel {
      var minimalSecurityLevel: String? = null
      if (null != options && options.hasKey(Maps.SECURITY_LEVEL)) {
        minimalSecurityLevel = options.getString(Maps.SECURITY_LEVEL)
      }
      if (null == minimalSecurityLevel) minimalSecurityLevel = fallback
      return SecurityLevel.valueOf(minimalSecurityLevel)
    }

    // endregion
    // region Implementation
    /** Is provided access control string matching biometry use request? */
    fun getUseBiometry(@AccessControl accessControl: String?): Boolean {
      return AccessControl.BIOMETRY_ANY == accessControl ||
          AccessControl.BIOMETRY_CURRENT_SET == accessControl ||
          AccessControl.BIOMETRY_ANY_OR_DEVICE_PASSCODE == accessControl ||
          AccessControl.BIOMETRY_CURRENT_SET_OR_DEVICE_PASSCODE == accessControl
    }

    /** Extract user specified prompt info from options. */
    private fun getPromptInfo(options: ReadableMap?): PromptInfo {
      val promptInfoOptionsMap =
          if (options != null && options.hasKey(Maps.AUTH_PROMPT)) options.getMap(Maps.AUTH_PROMPT)
          else null
      val promptInfoBuilder = PromptInfo.Builder()
      if (null != promptInfoOptionsMap && promptInfoOptionsMap.hasKey(AuthPromptOptions.TITLE)) {
        val promptInfoTitle = promptInfoOptionsMap.getString(AuthPromptOptions.TITLE)
        promptInfoBuilder.setTitle(promptInfoTitle!!)
      }
      if (null != promptInfoOptionsMap && promptInfoOptionsMap.hasKey(AuthPromptOptions.SUBTITLE)) {
        val promptInfoSubtitle = promptInfoOptionsMap.getString(AuthPromptOptions.SUBTITLE)
        promptInfoBuilder.setSubtitle(promptInfoSubtitle)
      }
      if (null != promptInfoOptionsMap &&
          promptInfoOptionsMap.hasKey(AuthPromptOptions.DESCRIPTION)) {
        val promptInfoDescription = promptInfoOptionsMap.getString(AuthPromptOptions.DESCRIPTION)
        promptInfoBuilder.setDescription(promptInfoDescription)
      }
      if (null != promptInfoOptionsMap && promptInfoOptionsMap.hasKey(AuthPromptOptions.CANCEL)) {
        val promptInfoNegativeButton = promptInfoOptionsMap.getString(AuthPromptOptions.CANCEL)
        promptInfoBuilder.setNegativeButtonText(promptInfoNegativeButton!!)
      }

      /* PromptInfo is only used in Biometric-enabled RSA storage and can only be unlocked by a strong biometric */ promptInfoBuilder
          .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)

      /* Bypass confirmation to avoid KeyStore unlock timeout being exceeded when using passive biometrics */ promptInfoBuilder
          .setConfirmationRequired(false)
      return promptInfoBuilder.build()
    }

    /** Throw exception in case of empty credentials providing. */
    @Throws(EmptyParameterException::class)
    fun throwIfEmptyLoginPassword(username: String?, password: String?) {
      if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
        throw EmptyParameterException("you passed empty or null username/password")
      }
    }

    /**
     * Throw exception if required security level does not match storage provided security level.
     */
    @Throws(CryptoFailedException::class)
    fun throwIfInsufficientLevel(storage: CipherStorage, level: SecurityLevel) {
      if (storage.securityLevel().satisfiesSafetyThreshold(level)) {
        return
      }
      throw CryptoFailedException(
          String.format(
              "Cipher Storage is too weak. Required security level is: %s, but only %s is provided",
              level.name,
              storage.securityLevel().name))
    }

    private fun getAliasOrDefault(alias: String?): String {
      return alias ?: EMPTY_STRING
    } // endregion
  }
}
