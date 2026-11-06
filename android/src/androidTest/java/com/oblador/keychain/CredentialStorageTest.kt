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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.After
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import javax.crypto.KeyGenerator

@RunWith(AndroidJUnit4::class)
class CredentialStorageTest {
  private lateinit var context: StorageTestContext
  private lateinit var job: kotlinx.coroutines.Job
  private lateinit var storage: DataStorePrefsStorage

  @Before fun setUp() {
    context = StorageTestContext(ApplicationProvider.getApplicationContext<Context>())
    openStorage()
  }

  private fun openStorage() {
    job = SupervisorJob()
    storage = DataStorePrefsStorage(context, CoroutineScope(Dispatchers.IO + job))
  }

  private fun reopenStorage() {
    runBlocking { job.cancelAndJoin() }
    openStorage()
  }

  @After fun tearDown() {
    runBlocking { job.cancelAndJoin() }
    context.close()
  }

  @Test fun enumerationIgnoresUnrelatedKeystoreKeysAndPreservesDefaultService() {
    val unrelated = "other-library-${UUID.randomUUID()}"
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
      reopenStorage()
      assertEquals(setOf("server"), storage.getInternetServers(false))
      assertEquals(15, storage.getEncryptedEntry("server")!!.authenticators)
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
  @Test fun migratesLegacyPreferencesOnceAndIgnoresIncompleteRecords() {
    val old = context.getSharedPreferences("RN_KEYCHAIN", Context.MODE_PRIVATE)
    old.edit()
      .putString("https://host:8443:u", "AQI=")
      .putString("https://host:8443:p", "AwQ=")
      .putString("https://host:8443:c", "KeystoreAESCBC")
      .putString("partial:u", "AQI=")
      .putString("partial:c", "KeystoreAESCBC")
      .commit()
    assertEquals(setOf("https://host:8443"), storage.getAllServices())
    assertNull(storage.getEncryptedEntry("partial"))
    val migrated = storage.getEncryptedEntry("https://host:8443")!!
    assertArrayEquals(byteArrayOf(1, 2), migrated.username)
    assertArrayEquals(byteArrayOf(3, 4), migrated.password)
    assertEquals("KeystoreAESCBC", migrated.cipherStorageName)
    assertNull(migrated.authenticators)
    assertEquals(setOf("https://host:8443"), storage.getInternetServers(true))
    assertTrue(storage.getInternetServers(false).isEmpty())
    assertTrue(old.all.isEmpty())
    storage.removeEntry("https://host:8443")
    reopenStorage()
    assertNull(storage.getEncryptedEntry("https://host:8443"))
  }

  @Test fun replacementAndRemovalDoNotAffectOtherRecords() {
    val entry = EncryptionResult(byteArrayOf(1), byteArrayOf(2), "KeystoreAESGCM")
    storage.storeEncryptedEntry("first", entry, 15, CredentialMetadata.INTERNET)
    storage.storeEncryptedEntry("second", entry, 32768, CredentialMetadata.INTERNET)
    storage.storeEncryptedEntry("first", entry, null, CredentialMetadata.GENERIC)
    reopenStorage()
    assertNull(storage.getEncryptedEntry("first")!!.authenticators)
    assertEquals(setOf("second"), storage.getInternetServers(true))
    storage.removeEntry("first")
    storage.removeEntry("first")
    assertEquals(setOf("second"), storage.getAllServices())
    assertEquals(32768, storage.getEncryptedEntry("second")!!.authenticators)
  }

}
