package com.oblador.keychain

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Test

class CredentialMetadataTest {
  @Test fun listsOnlyCompleteRecordsWithTheirOriginalServiceNames() {
    val prefs = mutablePreferencesOf()
    for (service in listOf("", "wallet", "https://example.com:p")) {
      prefs[stringPreferencesKey("$service:u")] = "dXNlcg=="
      prefs[stringPreferencesKey("$service:p")] = "c2VjcmV0"
      prefs[stringPreferencesKey("$service:c")] = "KeystoreAESGCM_NoAuth"
    }
    prefs[stringPreferencesKey("incomplete:c")] = "KeystoreAESGCM"
    prefs[stringPreferencesKey("incomplete:u")] = "dXNlcg=="
    prefs[stringPreferencesKey("unrelated")] = "value"
    assertEquals(setOf("", "wallet", "https://example.com:p"), CredentialMetadata.services(prefs))
    prefs.remove(stringPreferencesKey("wallet:p"))
    assertEquals(setOf("", "https://example.com:p"), CredentialMetadata.services(prefs))
  }

  @Test fun emptyStorageHasNoServices() {
    assertEquals(emptySet<String>(), CredentialMetadata.services(mutablePreferencesOf()))
  }
}
