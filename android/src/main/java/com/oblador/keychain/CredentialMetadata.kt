package com.oblador.keychain

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.oblador.keychain.PrefsStorageBase.Companion.getKeyForPassword
import com.oblador.keychain.PrefsStorageBase.Companion.getKeyForUsername
import com.oblador.keychain.PrefsStorageBase.Companion.isKeyForCipherStorage

internal object CredentialMetadata {
  const val GENERIC = "generic"
  const val INTERNET = "internet"

  fun internetServers(snapshot: Preferences, includeLegacy: Boolean): Set<String> =
    services(snapshot).filter { service ->
      val kind = snapshot[stringPreferencesKey("$service:k")]
      kind == INTERNET || (includeLegacy && kind == null)
    }.toSet()

  // List persisted records, not app-wide Keystore aliases. Keep the original
  // service (including the empty default service) so every identifier is usable.
  fun services(snapshot: Preferences): Set<String> = snapshot.asMap().keys
    .map { it.name }
    .filter(::isKeyForCipherStorage)
    .map { it.removeSuffix(":c") }
    .filter { service ->
      snapshot[stringPreferencesKey(getKeyForUsername(service))] != null &&
        snapshot[stringPreferencesKey(getKeyForPassword(service))] != null &&
        !snapshot[stringPreferencesKey("$service:c")].isNullOrEmpty()
    }
    .toSet()
}
