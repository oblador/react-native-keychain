package com.oblador.keychain

import com.oblador.keychain.KeychainModule.KnownCiphers
import com.oblador.keychain.cipherStorage.CipherStorage.CipherResult
import com.oblador.keychain.cipherStorage.CipherStorage.EncryptionResult

interface PrefsStorageBase {
  class ResultSet(
    @JvmField @field:KnownCiphers @param:KnownCiphers val cipherStorageName: String,
    usernameBytes: ByteArray?,
    passwordBytes: ByteArray?,
  ) : CipherResult<ByteArray?>(usernameBytes, passwordBytes)

  fun getEncryptedEntry(service: String): ResultSet?

  fun removeEntry(service: String)

  fun storeEncryptedEntry(service: String, encryptionResult: EncryptionResult)

  /**
   * List all types of cipher which are involved in en/decryption of the data stored herein.
   *
   * A cipher type is stored together with the datum upon encryption so the datum can later be
   * decrypted using correct cipher. This way, a [PrefsStorageBase] can involve different ciphers
   * for different data. This method returns all ciphers involved with this storage.
   *
   * @return set of cipher names
   */
  val usedCipherNames: Set<String?>

  companion object {
    const val KEYCHAIN_DATA = "RN_KEYCHAIN"

    fun getKeyForUsername(service: String): String {
      return "$service:u"
    }

    fun getKeyForPassword(service: String): String {
      return "$service:p"
    }

    fun getKeyForCipherStorage(service: String): String {
      return "$service:c"
    }

    fun isKeyForCipherStorage(key: String): Boolean {
      return key.endsWith(":c")
    }
  }
}
