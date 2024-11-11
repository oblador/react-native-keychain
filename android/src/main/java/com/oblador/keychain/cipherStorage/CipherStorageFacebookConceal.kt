package com.oblador.keychain.cipherStorage

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.util.Log
import com.facebook.android.crypto.keychain.AndroidConceal
import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain
import com.facebook.crypto.Crypto
import com.facebook.crypto.CryptoConfig
import com.facebook.crypto.Entity
import com.facebook.react.bridge.AssertionException
import com.facebook.react.bridge.ReactApplicationContext
import com.oblador.keychain.KeychainModule.KnownCiphers
import com.oblador.keychain.SecurityLevel
import com.oblador.keychain.resultHandler.ResultHandler
import com.oblador.keychain.exceptions.CryptoFailedException
import java.security.GeneralSecurityException
import java.security.Key

/**
 * @see [Conceal Project](https://github.com/facebook/conceal)
 * @see
 *   [Fast Cryptographics](https://medium.com/@ssaurel/make-fast-cryptographic-operations-on-android-with-conceal-77a751e89b8e)
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class CipherStorageFacebookConceal(reactContext: ReactApplicationContext) :
  CipherStorageBase(reactContext) {

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

  @Throws(CryptoFailedException::class)
  override fun encrypt(
    handler: ResultHandler,
    alias: String,
    username: String,
    password: String,
    level: SecurityLevel
  ) {

    throwIfInsufficientLevel(level)
    throwIfNoCryptoAvailable()

    val usernameEntity = createUsernameEntity(alias)
    val passwordEntity = createPasswordEntity(alias)

    try {
      val encryptedUsername = crypto.encrypt(username.toByteArray(UTF8), usernameEntity)
      val encryptedPassword = crypto.encrypt(password.toByteArray(UTF8), passwordEntity)

      val result = CipherStorage.EncryptionResult(encryptedUsername, encryptedPassword, this)
      handler.onEncrypt(result, null)
    } catch (fail: Throwable) {
      throw CryptoFailedException("Encryption failed for alias: $alias", fail)
    }
  }

  /** Redirect call to default [decrypt] method. */
  override fun decrypt(
    handler: ResultHandler,
    alias: String,
    username: ByteArray,
    password: ByteArray,
    level: SecurityLevel
  ) {
    throwIfInsufficientLevel(level)
    throwIfNoCryptoAvailable()

    val usernameEntity = createUsernameEntity(alias)
    val passwordEntity = createPasswordEntity(alias)

    try {
      val decryptedUsername = crypto.decrypt(username, usernameEntity)
      val decryptedPassword = crypto.decrypt(password, passwordEntity)

      val results = CipherStorage.DecryptionResult(
        String(decryptedUsername, UTF8), String(decryptedPassword, UTF8), SecurityLevel.ANY
      )
      handler.onDecrypt(results, null)
    } catch (fail: Throwable) {
      handler.onDecrypt(null, fail)
    }
  }

  override fun removeKey(alias: String) {
    // Facebook Conceal stores only one key across all services, so we cannot
    // delete the key (otherwise decryption will fail for encrypted data of other services).
    Log.w(LOG_TAG, "CipherStorageFacebookConceal removeKey called. alias: $alias")
  }


  @Throws(GeneralSecurityException::class)
  override fun getKeyGenSpecBuilder(alias: String): KeyGenParameterSpec.Builder {
    throw CryptoFailedException("Not designed for a call")
  }


  @Throws(GeneralSecurityException::class)
  override fun getKeyGenSpecBuilder(
    alias: String,
    isForTesting: Boolean
  ): KeyGenParameterSpec.Builder {
    throw CryptoFailedException("Not designed for a call")
  }


  @Throws(GeneralSecurityException::class)
  override fun getKeyInfo(key: Key): KeyInfo {
    throw CryptoFailedException("Not designed for a call")
  }


  @Throws(GeneralSecurityException::class)
  override fun generateKey(spec: KeyGenParameterSpec): Key {
    throw CryptoFailedException("Not designed for a call")
  }


  override fun getEncryptionAlgorithm(): String {
    throw AssertionException("Not designed for a call")
  }


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

  private fun createUsernameEntity(alias: String): Entity {
    val prefix = getEntityPrefix(alias)
    return Entity.create("$prefix user")
  }


  private fun createPasswordEntity(alias: String): Entity {
    val prefix = getEntityPrefix(alias)
    return Entity.create("$prefix pass")
  }


  private fun getEntityPrefix(alias: String): String = "$KEYCHAIN_DATA:$alias"
  // endregion
}
