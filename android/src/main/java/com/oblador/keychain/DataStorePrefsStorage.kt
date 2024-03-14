package com.oblador.keychain

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.facebook.react.bridge.ReactApplicationContext
import com.oblador.keychain.KeychainModule.KnownCiphers
import com.oblador.keychain.PrefsStorageBase.KEYCHAIN_DATA
import com.oblador.keychain.PrefsStorageBase.ResultSet
import com.oblador.keychain.PrefsStorageBase.getKeyForCipherStorage
import com.oblador.keychain.PrefsStorageBase.getKeyForPassword
import com.oblador.keychain.PrefsStorageBase.getKeyForUsername
import com.oblador.keychain.PrefsStorageBase.isKeyForCipherStorage
import com.oblador.keychain.cipherStorage.CipherStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Suppress("unused")
class DataStorePrefsStorage(reactContext: ReactApplicationContext) : PrefsStorageBase {

  private val Context.prefs: DataStore<Preferences> by preferencesDataStore(
    name = KEYCHAIN_DATA,
    produceMigrations = ::sharedPreferencesMigration
  )
  private val prefs: DataStore<Preferences> = reactContext.prefs
  private val prefsData: Preferences get() = callSuspendable { prefs.data.first() }

  private fun sharedPreferencesMigration(context: Context): List<DataMigration<Preferences>> {
    return listOf(SharedPreferencesMigration(context, KEYCHAIN_DATA))
  }

  override fun getEncryptedEntry(service: String): ResultSet? {
    val bytesForUsername = getBytesForUsername(service)
    val bytesForPassword = getBytesForPassword(service)
    var cipherStorageName = getCipherStorageName(service)

    // in case of wrong password or username
    if (bytesForUsername == null || bytesForPassword == null) return null
    if (cipherStorageName == null) {
      // If the CipherStorage name is not found, we assume it is because the entry was written by an older
      // version of this library. The older version used Facebook Conceal, so we default to that.
      cipherStorageName = KnownCiphers.FB
    }
    return ResultSet(cipherStorageName, bytesForUsername, bytesForPassword)
  }

  override fun removeEntry(service: String) {
    val keyForUsername = stringPreferencesKey(getKeyForUsername(service))
    val keyForPassword = stringPreferencesKey(getKeyForPassword(service))
    val keyForCipherStorage = stringPreferencesKey(getKeyForCipherStorage(service))
    callSuspendable {
      prefs.edit {
        it.remove(keyForUsername)
        it.remove(keyForPassword)
        it.remove(keyForCipherStorage)
      }
    }
  }

  override fun storeEncryptedEntry(
    service: String,
    encryptionResult: CipherStorage.EncryptionResult,
  ) {
    val keyForUsername = stringPreferencesKey(getKeyForUsername(service))
    val keyForPassword = stringPreferencesKey(getKeyForPassword(service))
    val keyForCipherStorage = stringPreferencesKey(getKeyForCipherStorage(service))
    callSuspendable {
      prefs.edit {
        it[keyForUsername] = Base64.encodeToString(encryptionResult.username, Base64.DEFAULT)
        it[keyForPassword] = Base64.encodeToString(encryptionResult.password, Base64.DEFAULT)
        it[keyForCipherStorage] = encryptionResult.cipherName
      }
    }
  }

  /**
   * List all types of cipher which are involved in en/decryption of the data stored herein.
   *
   * A cipher type is stored together with the datum upon encryption so the datum can later be decrypted using correct
   * cipher. This way, a [DataStorePrefsStorage] can involve different ciphers for different data. This method returns all
   * ciphers involved with this storage.
   *
   * @return set of cipher names
   */
  override fun getUsedCipherNames(): Set<String?> {
    val result: MutableSet<String?> = HashSet()
    val keys = prefsData.asMap().keys.map { it.name }
    for (key in keys) {
      if (isKeyForCipherStorage(key)) {
        val cipher = prefsData[stringPreferencesKey(key)]
        result.add(cipher)
      }
    }
    return result
  }

  private fun <T> callSuspendable(block: suspend () -> T): T {
    // TODO actually use concurrency to prevent blocking the ui thread
    return runBlocking {
      block()
    }
  }

  private fun getBytesForUsername(service: String): ByteArray? {
    val key = stringPreferencesKey(getKeyForUsername(service))
    return getBytes(key)
  }

  private fun getBytesForPassword(service: String): ByteArray? {
    val key = stringPreferencesKey(getKeyForPassword(service))
    return getBytes(key)
  }

  private fun getCipherStorageName(service: String): String? {
    val key = stringPreferencesKey(getKeyForCipherStorage(service))
    return prefsData[key]
  }

  private fun getBytes(prefKey: Preferences.Key<String>): ByteArray? {
    return prefsData[prefKey]?.let { Base64.decode(it, Base64.DEFAULT) }
  }
}
