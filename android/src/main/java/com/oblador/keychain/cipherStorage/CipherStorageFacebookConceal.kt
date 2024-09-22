package com.oblador.keychain.cipherStorage

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.util.Log
import androidx.annotation.NonNull
import com.facebook.android.crypto.keychain.AndroidConceal
import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain
import com.facebook.crypto.Crypto
import com.facebook.crypto.CryptoConfig
import com.facebook.crypto.Entity
import com.facebook.react.bridge.AssertionException
import com.facebook.react.bridge.ReactApplicationContext
import com.oblador.keychain.KeychainModule.KnownCiphers
import com.oblador.keychain.SecurityLevel
import com.oblador.keychain.decryptionHandler.DecryptionResultHandler
import com.oblador.keychain.exceptions.CryptoFailedException
import java.security.GeneralSecurityException
import java.security.Key

/**
 * @see [Conceal Project](https://github.com/facebook/conceal)
 * @see
 *   [Fast Cryptographics](https://medium.com/@ssaurel/make-fast-cryptographic-operations-on-android-with-conceal-77a751e89b8e)
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class CipherStorageFacebookConceal(@NonNull reactContext: ReactApplicationContext) :
    CipherStorageBase() {

  companion object {
    const val KEYCHAIN_DATA = "RN_KEYCHAIN"
  }

  private val crypto: Crypto

  init {
    val keyChain = SharedPrefsBackedKeyChain(reactContext, CryptoConfig.KEY_256)
    crypto = AndroidConceal.get().createDefaultCrypto(keyChain)
  }

  // region Configuration
  override fun getCipherStorageName(): String = KnownCiphers.FB

  override fun getMinSupportedApiLevel(): Int = Build.VERSION_CODES.JELLY_BEAN

  override fun securityLevel(): SecurityLevel = SecurityLevel.ANY

  override fun supportsSecureHardware(): Boolean = false

  override fun isBiometrySupported(): Boolean = false

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
    throwIfNoCryptoAvailable()

    val usernameEntity = createUsernameEntity(alias)
    val passwordEntity = createPasswordEntity(alias)

    return try {
      val encryptedUsername = crypto.encrypt(username.toByteArray(UTF8), usernameEntity)
      val encryptedPassword = crypto.encrypt(password.toByteArray(UTF8), passwordEntity)

      CipherStorage.EncryptionResult(encryptedUsername, encryptedPassword, this)
    } catch (fail: Throwable) {
      throw CryptoFailedException("Encryption failed for alias: $alias", fail)
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
    throwIfNoCryptoAvailable()

    val usernameEntity = createUsernameEntity(alias)
    val passwordEntity = createPasswordEntity(alias)

    return try {
      val decryptedUsername = crypto.decrypt(username, usernameEntity)
      val decryptedPassword = crypto.decrypt(password, passwordEntity)

      CipherStorage.DecryptionResult(
          String(decryptedUsername, UTF8), String(decryptedPassword, UTF8), SecurityLevel.ANY)
    } catch (fail: Throwable) {
      throw CryptoFailedException("Decryption failed for alias: $alias", fail)
    }
  }

  /** Redirect call to default [decrypt] method. */
  override fun decrypt(
      @NonNull handler: DecryptionResultHandler,
      @NonNull service: String,
      @NonNull username: ByteArray,
      @NonNull password: ByteArray,
      @NonNull level: SecurityLevel
  ) {
    try {
      val results = decrypt(service, username, password, level)
      handler.onDecrypt(results, null)
    } catch (fail: Throwable) {
      handler.onDecrypt(null, fail)
    }
  }

  override fun removeKey(@NonNull alias: String) {
    // Facebook Conceal stores only one key across all services, so we cannot
    // delete the key (otherwise decryption will fail for encrypted data of other services).
    Log.w(LOG_TAG, "CipherStorageFacebookConceal removeKey called. alias: $alias")
  }

  @NonNull
  @Throws(GeneralSecurityException::class)
  override fun getKeyGenSpecBuilder(@NonNull alias: String): KeyGenParameterSpec.Builder {
    throw CryptoFailedException("Not designed for a call")
  }

  @NonNull
  @Throws(GeneralSecurityException::class)
  override fun getKeyGenSpecBuilder(
      @NonNull alias: String,
      isForTesting: Boolean
  ): KeyGenParameterSpec.Builder {
    throw CryptoFailedException("Not designed for a call")
  }

  @NonNull
  @Throws(GeneralSecurityException::class)
  override fun getKeyInfo(@NonNull key: Key): KeyInfo {
    throw CryptoFailedException("Not designed for a call")
  }

  @NonNull
  @Throws(GeneralSecurityException::class)
  override fun generateKey(@NonNull spec: KeyGenParameterSpec): Key {
    throw CryptoFailedException("Not designed for a call")
  }

  @NonNull
  override fun getEncryptionAlgorithm(): String {
    throw AssertionException("Not designed for a call")
  }

  @NonNull
  override fun getEncryptionTransformation(): String {
    throw AssertionException("Not designed for a call")
  }

  /** Verify availability of the Crypto API. */
  @Throws(CryptoFailedException::class)
  private fun throwIfNoCryptoAvailable() {
    if (!crypto.isAvailable) {
      throw CryptoFailedException("Crypto is missing")
    }
  }

  // endregion

  // region Helper methods
  @NonNull
  private fun createUsernameEntity(@NonNull alias: String): Entity {
    val prefix = getEntityPrefix(alias)
    return Entity.create("$prefix user")
  }

  @NonNull
  private fun createPasswordEntity(@NonNull alias: String): Entity {
    val prefix = getEntityPrefix(alias)
    return Entity.create("$prefix pass")
  }

  @NonNull private fun getEntityPrefix(@NonNull alias: String): String = "$KEYCHAIN_DATA:$alias"
  // endregion
}
