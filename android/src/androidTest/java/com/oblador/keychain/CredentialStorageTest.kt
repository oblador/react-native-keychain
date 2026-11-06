package com.oblador.keychain

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oblador.keychain.cipherStorage.CipherStorage.EncryptionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import javax.crypto.KeyGenerator

@RunWith(AndroidJUnit4::class)
class CredentialStorageTest {
  companion object {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val storage = DataStorePrefsStorage(context, scope)
  }

  @Test fun enumerationIgnoresUnrelatedKeystoreKeysAndPreservesDefaultService() {
    val unrelated = "other-library-key"
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
      init(KeyGenParameterSpec.Builder(unrelated, KeyProperties.PURPOSE_ENCRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
      generateKey()
    }
    try {
      storage.storeEncryptedEntry("", EncryptionResult(byteArrayOf(1), byteArrayOf(2), "KeystoreAESGCM"), 15)
      assertEquals(setOf(""), storage.getAllServices())
      assertEquals(15, storage.getEncryptedEntry("")!!.authenticators)
      storage.removeEntry("")
      assertTrue(storage.getAllServices().isEmpty())
      assertNull(storage.getEncryptedEntry(""))
      assertTrue(keyStore.containsAlias(unrelated))
    } finally {
      storage.removeEntry("")
      keyStore.deleteEntry(unrelated)
    }
  }

  @Test fun internetKindIsPersistedReplacedAndRemovedWithTheRecord() {
    val entry = EncryptionResult(byteArrayOf(1), byteArrayOf(2), "KeystoreAESGCM")
    try {
      storage.storeEncryptedEntry("server", entry)
      assertTrue(storage.getInternetServers(false).isEmpty())
      assertEquals(setOf("server"), storage.getInternetServers(true))
      storage.storeEncryptedEntry("server", entry, 15, CredentialMetadata.INTERNET)
      assertEquals(setOf("server"), storage.getInternetServers(false))
      storage.storeEncryptedEntry("server", entry, null, CredentialMetadata.GENERIC)
      assertTrue(storage.getInternetServers(true).isEmpty())
      storage.removeEntry("server")
      storage.storeEncryptedEntry("server", entry)
      assertTrue(storage.getInternetServers(false).isEmpty())
    } finally {
      storage.removeEntry("server")
    }
  }

  @Test fun promptButtonsMatchTheAllowedAuthenticators() {
    val biometric = KeychainModule.getPromptInfo(null, 15)
    assertEquals(15, biometric.allowedAuthenticators)
    assertEquals("Cancel", biometric.negativeButtonText.toString())
    if (android.os.Build.VERSION.SDK_INT >= 30) {
      for (authenticators in listOf(32768, 32768 or 15)) {
        val prompt = KeychainModule.getPromptInfo(null, authenticators)
        assertEquals(authenticators, prompt.allowedAuthenticators)
        assertTrue(prompt.negativeButtonText.isNullOrEmpty())
      }
    }
    assertEquals("Cancel", KeychainModule.getPromptInfo(null, null).negativeButtonText.toString())
  }

  @Test fun legacyRecordsWithoutPromptMetadataRemainReadable() {
    // The existing on-disk format is readable without the new optional :a field.
    val entry = EncryptionResult(byteArrayOf(3), byteArrayOf(4), "KeystoreAESGCM")
    try {
      storage.storeEncryptedEntry("legacy", entry)
      assertNull(storage.getEncryptedEntry("legacy")!!.authenticators)
      assertArrayEquals(entry.username, storage.getEncryptedEntry("legacy")!!.username)
      storage.storeEncryptedEntry("legacy", entry, 32768)
      assertEquals(32768, storage.getEncryptedEntry("legacy")!!.authenticators)
      storage.storeEncryptedEntry("legacy", entry)
      assertNull(storage.getEncryptedEntry("legacy")!!.authenticators)
    } finally {
      storage.removeEntry("legacy")
    }
  }
}
