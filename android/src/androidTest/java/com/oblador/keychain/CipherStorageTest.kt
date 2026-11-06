package com.oblador.keychain

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.oblador.keychain.cipherStorage.*
import com.oblador.keychain.resultHandler.ResultHandlerNonInteractive
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CipherStorageTest {
  private lateinit var context: StorageTestContext
  private lateinit var reactContext: NativeTestContext
  private val aliases = mutableListOf<String>()
  private val jobs = mutableListOf<Job>()

  @Before fun setUp() {
    context = StorageTestContext(ApplicationProvider.getApplicationContext<Context>())
    reactContext = NativeTestContext(context)
  }

  @After fun tearDown() {
    jobs.forEach { runBlocking { it.cancelAndJoin() } }
    KeyStore.getInstance("AndroidKeyStore").apply {
      load(null)
      aliases.forEach { deleteEntry(it) }
    }
    CipherCache.clearCache()
    context.close()
  }

  private fun alias() = "keychain-test-${UUID.randomUUID()}".also { aliases.add(it) }
  private fun storage(): DataStorePrefsStorage {
    val job = SupervisorJob().also { jobs.add(it) }
    return DataStorePrefsStorage(context, CoroutineScope(Dispatchers.IO + job))
  }

  private fun encrypt(cipher: CipherStorage, alias: String): CipherStorage.EncryptionResult {
    val handler = ResultHandlerNonInteractive()
    cipher.encrypt(handler, alias, "użytkownik 🔑", "密碼\u0000secret", SecurityLevel.ANY)
    assertNull(handler.error)
    return requireNotNull(handler.encryptionResult)
  }

  @Test fun recordedCipherDecryptsAfterReopeningStorageAndCreatingANewCipher() {
    val first = storage()
    // CBC remains a supported reader even though new writes default to GCM.
    for (cipher in listOf(CipherStorageKeystoreAesCbc(reactContext), CipherStorageKeystoreAesGcm(reactContext, false))) {
      val service = alias()
      val entry = encrypt(cipher, service)
      if (cipher is CipherStorageKeystoreAesCbc) {
        // Previous releases wrote these literal keys and Base64 values to SharedPreferences.
        context.getSharedPreferences("RN_KEYCHAIN", Context.MODE_PRIVATE).edit()
          .putString("$service:u", Base64.encodeToString(entry.username, Base64.DEFAULT))
          .putString("$service:p", Base64.encodeToString(entry.password, Base64.DEFAULT))
          .putString("$service:c", "KeystoreAESCBC")
          .commit()
      } else {
        first.storeEncryptedEntry(service, entry)
      }
    }
    runBlocking { jobs.last().cancelAndJoin() }
    CipherCache.clearCache()
    val reopened = storage()
    for (service in aliases) {
      val entry = reopened.getEncryptedEntry(service)!!
      val cipher = when (entry.cipherStorageName) {
        "KeystoreAESCBC" -> CipherStorageKeystoreAesCbc(reactContext)
        "KeystoreAESGCM_NoAuth" -> CipherStorageKeystoreAesGcm(reactContext, false)
        else -> error("Unexpected recorded cipher: ${entry.cipherStorageName}")
      }
      val handler = ResultHandlerNonInteractive()
      cipher.decrypt(handler, service, entry.username!!, entry.password!!, SecurityLevel.ANY)
      assertNull(handler.error)
      assertEquals("użytkownik 🔑", handler.decryptionResult!!.username)
      assertEquals("密碼\u0000secret", handler.decryptionResult!!.password)
    }
  }

  @Test fun tamperingAndDeletedKeysFailWithoutReturningCredentials() {
    val cipher = CipherStorageKeystoreAesGcm(reactContext, false)
    val service = alias()
    val entry = encrypt(cipher, service)
    val modified = entry.password.copyOf().apply { this[lastIndex] = (last().toInt() xor 1).toByte() }
    val tampered = ResultHandlerNonInteractive()
    cipher.decrypt(tampered, service, entry.username, modified, SecurityLevel.ANY)
    assertNotNull(tampered.error)
    assertNull(tampered.decryptionResult)
    // A failed operation must not poison subsequent valid reads.
    val recovered = ResultHandlerNonInteractive()
    cipher.decrypt(recovered, service, entry.username, entry.password, SecurityLevel.ANY)
    assertNull(recovered.error)
    assertNotNull(recovered.decryptionResult)
    cipher.removeKey(service)
    CipherCache.clearCache()
    val missingKey = ResultHandlerNonInteractive()
    CipherStorageKeystoreAesGcm(reactContext, false).decrypt(
      missingKey, service, entry.username, entry.password, SecurityLevel.ANY
    )
    assertNotNull(missingKey.error)
    assertNull(missingKey.decryptionResult)
  }
}
