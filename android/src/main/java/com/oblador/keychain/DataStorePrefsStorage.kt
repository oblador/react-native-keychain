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
import com.oblador.keychain.PrefsStorageBase.Companion.KEYCHAIN_DATA
import com.oblador.keychain.PrefsStorageBase.Companion.getKeyForCipherStorage
import com.oblador.keychain.PrefsStorageBase.Companion.getKeyForPassword
import com.oblador.keychain.PrefsStorageBase.Companion.getKeyForUsername
import com.oblador.keychain.PrefsStorageBase.Companion.isKeyForCipherStorage
import com.oblador.keychain.PrefsStorageBase.ResultSet
import com.oblador.keychain.cipherStorage.CipherStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class DataStorePrefsStorage(
  reactContext: ReactApplicationContext,
  private val coroutineScope: CoroutineScope,
) : PrefsStorageBase {

  private val Context.prefs: DataStore<Preferences> by preferencesDataStore(
    name = KEYCHAIN_DATA,
    produceMigrations = ::sharedPreferencesMigration,
    scope = coroutineScope,
  )
  private val prefs: DataStore<Preferences> = reactContext.prefs
  private val prefsData: Preferences get() = callSuspendable { prefs.data.first() }

  private fun sharedPreferencesMigration(context: Context): List<DataMigration<Preferences>> {
    return listOf(SharedPreferencesMigration(context, KEYCHAIN_DATA))
  }

  override fun getEncryptedEntry(service: String): ResultSet? {
    val data = prefsData
    val bytesForUsername = getBytes(data, stringPreferencesKey(getKeyForUsername(service)))
    val bytesForPassword = getBytes(data, stringPreferencesKey(getKeyForPassword(service)))
    val cipherStorageName = data[stringPreferencesKey(getKeyForCipherStorage(service))]

    if (bytesForUsername == null || bytesForPassword == null || cipherStorageName == null) return null

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

  override val usedCipherNames: Set<String?>
    get() {
      val result: MutableSet<String?> = HashSet()
      val data = prefsData
      for (key in data.asMap().keys) {
        if (isKeyForCipherStorage(key.name)) {
          result.add(data[stringPreferencesKey(key.name)])
        }
      }
      return result
    }

  override val serviceNames: Set<String>
    get() {
      val data = prefsData
      val services = HashSet<String>()
      for (key in data.asMap().keys) {
        if (!isKeyForCipherStorage(key.name)) continue
        val service = key.name.removeSuffix(":c")
        if (data[stringPreferencesKey(getKeyForUsername(service))] != null &&
          data[stringPreferencesKey(getKeyForPassword(service))] != null) {
          services.add(service)
        }
      }
      return services
    }

  private fun <T> callSuspendable(block: suspend () -> T): T {
    return runBlocking(coroutineScope.coroutineContext) {
      block()
    }
  }

  private fun getBytes(data: Preferences, prefKey: Preferences.Key<String>): ByteArray? {
    return data[prefKey]?.let { Base64.decode(it, Base64.DEFAULT) }
  }
}
