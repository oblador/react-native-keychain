package com.oblador.keychain.cipherStorage

import androidx.annotation.NonNull
import com.oblador.keychain.SecurityLevel
import com.oblador.keychain.decryptionHandler.DecryptionResultHandler
import com.oblador.keychain.exceptions.CryptoFailedException
import com.oblador.keychain.exceptions.KeyStoreAccessException
import java.security.Key

@Suppress("unused", "MemberVisibilityCanBePrivate")
interface CipherStorage {

  // region Helper classes

  /** Basis for storing credentials in different data type formats. */
  abstract class CipherResult<T>(val username: T, val password: T)

  /** Credentials in bytes array, often a result of encryption. */
  class EncryptionResult(
      username: ByteArray,
      password: ByteArray,
      /** Name of cipher storage used for encryption. */
      val cipherName: String
  ) : CipherResult<ByteArray>(username, password) {
    /** Helper constructor. Simplifies cipher name extraction. */
    constructor(
        username: ByteArray,
        password: ByteArray,
        cipherStorage: CipherStorage
    ) : this(username, password, cipherStorage.getCipherStorageName())
  }

  /** Credentials in strings, often a result of decryption. */
  class DecryptionResult(
      username: String,
      password: String,
      private val securityLevel: SecurityLevel = SecurityLevel.ANY
  ) : CipherResult<String>(username, password) {
    fun getSecurityLevel(): SecurityLevel = securityLevel
  }

  /** Ask access permission for decrypting credentials in provided context. */
  class DecryptionContext(
      @NonNull val keyAlias: String,
      @NonNull val key: Key,
      password: ByteArray,
      username: ByteArray
  ) : CipherResult<ByteArray>(username, password)

  // endregion

  // region API

  /**
   * Encrypt credentials with provided key (by alias) and required security level.
   *
   * @throws CryptoFailedException If encryption fails.
   */
  @NonNull
  @Throws(CryptoFailedException::class)
  fun encrypt(
      @NonNull alias: String,
      @NonNull username: String,
      @NonNull password: String,
      @NonNull level: SecurityLevel
  ): EncryptionResult

  /**
   * Decrypt credentials with provided key (by alias) and required security level. In case of key
   * stored in weaker security level than required, an exception will be raised. That can happen
   * during migration from one version of library to another.
   *
   * @throws CryptoFailedException If decryption fails.
   */
  @NonNull
  @Throws(CryptoFailedException::class)
  fun decrypt(
      @NonNull alias: String,
      @NonNull username: ByteArray,
      @NonNull password: ByteArray,
      @NonNull level: SecurityLevel
  ): DecryptionResult

  /**
   * Decrypt the credentials but redirect results of operation to handler.
   *
   * @throws CryptoFailedException If decryption fails.
   */
  @Throws(CryptoFailedException::class)
  fun decrypt(
      @NonNull handler: DecryptionResultHandler,
      @NonNull alias: String,
      @NonNull username: ByteArray,
      @NonNull password: ByteArray,
      @NonNull level: SecurityLevel
  )

  /** Remove key (by alias) from storage. */
  @Throws(KeyStoreAccessException::class) fun removeKey(@NonNull alias: String)

  /**
   * Return all keys present in this storage.
   *
   * @return Set of key aliases.
   */
  @Throws(KeyStoreAccessException::class) fun getAllKeys(): Set<String>

  // endregion

  // region Configuration

  /** Storage name. */
  fun getCipherStorageName(): String

  /** Minimal API level needed for using the storage. */
  fun getMinSupportedApiLevel(): Int

  /** Provided security level. */
  fun securityLevel(): SecurityLevel

  /** True if based on secured hardware capabilities, otherwise False. */
  fun supportsSecureHardware(): Boolean

  /** True if based on biometric capabilities, otherwise False. */
  fun isBiometrySupported(): Boolean

  /**
   * The higher value means better capabilities. Formula: = 1000 * isBiometrySupported() + 100 *
   * supportsSecureHardware() + minSupportedApiLevel
   */
  fun getCapabilityLevel(): Int

  /** Get default name for alias/service. */
  fun getDefaultAliasServiceName(): String

  // endregion
}
