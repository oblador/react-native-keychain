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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Process-wide owner of the RN_KEYCHAIN DataStore.
 *
 * Jetpack DataStore allows one active instance per file. A JS reload (DevSettings.reload,
 * expo-updates reloadAsync, error-boundary restart) recreates the ReactContext and therefore a
 * second KeychainModule / DataStorePrefsStorage while the first module's scope is merely cancelled,
 * so a per-instance DataStore means two DataStores on the same file ("There are multiple DataStores
 * active for the same file") and writes cancelled mid-flight. Keeping the DataStore and its owning
 * scope here for the life of the process makes module recreation and KeychainModule.invalidate()
 * irrelevant to it, and runs the SharedPreferences migration once per process.
 */
internal object KeychainDataStore {
  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  private val Context.prefs: DataStore<Preferences> by preferencesDataStore(
    name = KEYCHAIN_DATA,
    produceMigrations = ::sharedPreferencesMigration,
    scope = scope,
  )

  private fun sharedPreferencesMigration(context: Context): List<DataMigration<Preferences>> {
    return listOf(SharedPreferencesMigration(context, KEYCHAIN_DATA))
  }

  fun get(context: Context): DataStore<Preferences> = context.applicationContext.prefs
}

class DataStorePrefsStorage(
  reactContext: ReactApplicationContext,
  private val coroutineScope: CoroutineScope,
) : PrefsStorageBase {

  private val prefs: DataStore<Preferences> = KeychainDataStore.get(reactContext)
  private val prefsData: Preferences get() = callSuspendable { prefs.data.first() }

  override fun getEncryptedEntry(service: String): ResultSet? {
    val bytesForUsername = getBytesForUsername(service)
    val bytesForPassword = getBytesForPassword(service)
    val cipherStorageName = getCipherStorageName(service)

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
    return runBlocking(coroutineScope.coroutineContext) {
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
