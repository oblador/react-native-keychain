package com.oblador.keychain

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.facebook.react.bridge.ReactApplicationContext
import com.oblador.keychain.KeychainModule.KnownCiphers
import com.oblador.keychain.PrefsStorageBase.Companion.KEYCHAIN_DATA
import com.oblador.keychain.PrefsStorageBase.Companion.getKeyForCipherStorage
import com.oblador.keychain.PrefsStorageBase.Companion.getKeyForPassword
import com.oblador.keychain.PrefsStorageBase.Companion.getKeyForUsername
import com.oblador.keychain.PrefsStorageBase.Companion.isKeyForCipherStorage
import com.oblador.keychain.PrefsStorageBase.ResultSet
import com.oblador.keychain.cipherStorage.CipherStorage.EncryptionResult

@Suppress("unused")
open class PrefsStorage(reactContext: ReactApplicationContext) : PrefsStorageBase {

  private val prefs: SharedPreferences

  init {
    prefs = reactContext.getSharedPreferences(KEYCHAIN_DATA, Context.MODE_PRIVATE)
  }

  override fun getEncryptedEntry(service: String): ResultSet? {
    val bytesForUsername = getBytesForUsername(service)
    val bytesForPassword = getBytesForPassword(service)
    var cipherStorageName = getCipherStorageName(service)

    // in case of wrong password or username
    if (bytesForUsername == null || bytesForPassword == null) {
      return null
    }
    if (cipherStorageName == null) {
      // If the CipherStorage name is not found, we assume it is because the entry was written by an
      // older
      // version of this library. The older version used Facebook Conceal, so we default to that.
      cipherStorageName = KnownCiphers.FB
    }
    return ResultSet(cipherStorageName, bytesForUsername, bytesForPassword)
  }

  override fun removeEntry(service: String) {
    val keyForUsername = getKeyForUsername(service)
    val keyForPassword = getKeyForPassword(service)
    val keyForCipherStorage = getKeyForCipherStorage(service)
    prefs.edit().remove(keyForUsername).remove(keyForPassword).remove(keyForCipherStorage).apply()
  }

  override fun storeEncryptedEntry(service: String, encryptionResult: EncryptionResult) {
    val keyForUsername = getKeyForUsername(service)
    val keyForPassword = getKeyForPassword(service)
    val keyForCipherStorage = getKeyForCipherStorage(service)
    prefs
        .edit()
        .putString(keyForUsername, Base64.encodeToString(encryptionResult.username, Base64.DEFAULT))
        .putString(keyForPassword, Base64.encodeToString(encryptionResult.password, Base64.DEFAULT))
        .putString(keyForCipherStorage, encryptionResult.cipherName)
        .apply()
  }

  override val usedCipherNames: Set<String?>
    get() {
      val result: MutableSet<String?> = HashSet()
      val keys: Set<String> = prefs.all.keys
      for (key in keys) {
        if (isKeyForCipherStorage(key)) {
          val cipher = prefs.getString(key, null)
          result.add(cipher)
        }
      }
      return result
    }

  private fun getBytesForUsername(service: String): ByteArray? {
    val key = getKeyForUsername(service)
    return getBytes(key)
  }

  private fun getBytesForPassword(service: String): ByteArray? {
    val key = getKeyForPassword(service)
    return getBytes(key)
  }

  private fun getCipherStorageName(service: String): String? {
    val key = getKeyForCipherStorage(service)
    return prefs.getString(key, null)
  }

  private fun getBytes(key: String): ByteArray? {
    val value = prefs.getString(key, null)
    return if (value != null) {
      Base64.decode(value, Base64.DEFAULT)
    } else null
  }
}
