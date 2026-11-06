package com.oblador.keychain

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
  reactContext: Context,
  private val coroutineScope: CoroutineScope,
) : PrefsStorageBase {

  private val prefs: DataStore<Preferences> = KeychainDataStore.get(reactContext)
  private val prefsData: Preferences get() = callSuspendable { prefs.data.first() }

  override fun getEncryptedEntry(service: String): ResultSet? {
    val snapshot = prefsData
    val username = snapshot[stringPreferencesKey(getKeyForUsername(service))] ?: return null
    val password = snapshot[stringPreferencesKey(getKeyForPassword(service))] ?: return null
    val cipher = snapshot[stringPreferencesKey(getKeyForCipherStorage(service))] ?: return null
    return ResultSet(
      cipher,
      Base64.decode(username, Base64.DEFAULT),
      Base64.decode(password, Base64.DEFAULT),
      snapshot[intPreferencesKey("$service:a")],
    )
  }

  override fun getAllServices(): Set<String> = CredentialMetadata.services(prefsData)

  override fun getInternetServers(includeLegacy: Boolean): Set<String> =
    CredentialMetadata.internetServers(prefsData, includeLegacy)

  override fun removeEntry(service: String) {
    val keyForUsername = stringPreferencesKey(getKeyForUsername(service))
    val keyForPassword = stringPreferencesKey(getKeyForPassword(service))
    val keyForCipherStorage = stringPreferencesKey(getKeyForCipherStorage(service))
    callSuspendable {
      prefs.edit {
        it.remove(keyForUsername)
        it.remove(keyForPassword)
        it.remove(keyForCipherStorage)
        it.remove(intPreferencesKey("$service:a"))
        it.remove(stringPreferencesKey("$service:k"))
      }
    }
  }

  override fun storeEncryptedEntry(
    service: String,
    encryptionResult: CipherStorage.EncryptionResult,
    authenticators: Int?,
    credentialKind: String?,
  ) {
    val keyForUsername = stringPreferencesKey(getKeyForUsername(service))
    val keyForPassword = stringPreferencesKey(getKeyForPassword(service))
    val keyForCipherStorage = stringPreferencesKey(getKeyForCipherStorage(service))
    callSuspendable {
      prefs.edit {
        it[keyForUsername] = Base64.encodeToString(encryptionResult.username, Base64.DEFAULT)
        it[keyForPassword] = Base64.encodeToString(encryptionResult.password, Base64.DEFAULT)
        it[keyForCipherStorage] = encryptionResult.cipherName
        val authKey = intPreferencesKey("$service:a")
        if (authenticators == null) it.remove(authKey) else it[authKey] = authenticators
        val kindKey = stringPreferencesKey("$service:k")
        if (credentialKind == null) it.remove(kindKey) else it[kindKey] = credentialKind
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

}
