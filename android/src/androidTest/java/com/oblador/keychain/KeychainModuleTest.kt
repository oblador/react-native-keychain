package com.oblador.keychain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.react.bridge.*
import com.facebook.react.soloader.OpenSourceMergedSoMapping
import com.facebook.soloader.SoLoader
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class KeychainModuleTest {
  private lateinit var context: StorageTestContext
  private lateinit var module: KeychainModule
  private val service = "keychain-module-${UUID.randomUUID()}"

  @Before fun setUp() {
    context = StorageTestContext(ApplicationProvider.getApplicationContext<Context>())
    SoLoader.init(context, OpenSourceMergedSoMapping)
    module = KeychainModule(NativeTestContext(context))
  }

  @After fun tearDown() {
    call { module.resetGenericPasswordForOptions(options(), it) }
    module.invalidate()
    KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(service) }
    context.close()
  }

  private fun options() = Arguments.createMap().apply { putString("service", service); putString("server", service) }
  private data class Rejected(val code: String?)
  private fun pending(action: (Promise) -> Unit): CompletableFuture<Any?> {
    val future = CompletableFuture<Any?>()
    action(PromiseImpl(
      Callback { args -> check(future.complete(args.firstOrNull())) { "Promise resolved twice" } },
      Callback { args -> check(future.complete(Rejected((args[0] as ReadableMap).getString("code")))) { "Promise rejected twice" } },
    ))
    return future
  }
  private fun call(action: (Promise) -> Unit): Any? = pending(action).get(10, TimeUnit.SECONDS)

  @Test fun bothCredentialApisPreserveMissingAndInvalidParameterResults() {
    for (internet in listOf(false, true)) {
      assertEquals(false, call {
        if (internet) module.getInternetCredentialsForServer(service, null, it)
        else module.getGenericPasswordForOptions(options(), it)
      })
      assertEquals(Rejected("E_INVALID_PARAMETERS"), call {
        if (internet) module.setInternetCredentialsForServer(service, "", "password", null, it)
        else module.setGenericPasswordForOptions(options(), "", "password", it)
      })
      assertEquals(false, call {
        if (internet) module.hasInternetCredentialsForOptions(options(), it)
        else module.hasGenericPasswordForOptions(options(), it)
      })
    }
  }

  @Test fun concurrentWritesKeepCredentialPairsTogetherAndResetIsIdempotent() {
    val writes = (1..4).map { index -> pending {
      module.setGenericPasswordForOptions(options(), "user-$index", "password-$index", it)
    } }
    writes.forEach { assertTrue(it.get(10, TimeUnit.SECONDS) is ReadableMap) }
    val value = call { module.getGenericPasswordForOptions(options(), it) } as ReadableMap
    val index = value.getString("username")!!.removePrefix("user-")
    assertTrue(value.getString("password") == "password-$index")
    repeat(2) { assertEquals(true, call { module.resetGenericPasswordForOptions(options(), it) }) }
    assertEquals(false, call { module.getGenericPasswordForOptions(options(), it) })
  }

  @Test fun softwareSecurityRequestRoundTripsAndReportsTheRecordedCipher() {
    for (cipher in listOf("KeystoreAESCBC", "KeystoreAESGCM_NoAuth")) {
      val options = options().apply { putString("storage", cipher); putString("securityLevel", "SECURE_SOFTWARE") }
      val saved = call { module.setGenericPasswordForOptions(options, "user", "password", it) } as ReadableMap
      assertEquals(cipher, saved.getString("storage"))
      // The read deliberately omits the storage option.
      val read = call { module.getGenericPasswordForOptions(options(), it) } as ReadableMap
      assertEquals(cipher, read.getString("storage"))
      assertTrue(read.getString("password") == "password")
      call { module.resetGenericPasswordForOptions(options(), it) }
    }
  }

  @Test fun unknownCipherRecordsRejectInsteadOfReportingMissing() {
    // DataStore's migration is lazy. Seed the previous on-disk format before the first read.
    context.getSharedPreferences("RN_KEYCHAIN", Context.MODE_PRIVATE).edit()
      .putString("$service:u", "AQ==")
      .putString("$service:p", "Ag==")
      .putString("$service:c", "UnknownCipher")
      .commit()
    assertEquals(Rejected("E_STORAGE_ACCESS_ERROR"), call {
      module.getGenericPasswordForOptions(options(), it)
    })
    assertEquals(Rejected("E_STORAGE_ACCESS_ERROR"), call {
      module.getInternetCredentialsForServer(service, null, it)
    })
  }

  @Test fun hardwareSecurityRequirementIsNeverSilentlyDowngraded() {
    val supported = call { module.getSecurityLevel(null, it) }
    val options = options().apply { putString("securityLevel", "SECURE_HARDWARE") }
    val result = call { module.setGenericPasswordForOptions(options, "user", "password", it) }
    if (supported == "SECURE_HARDWARE") {
      assertTrue(result is ReadableMap)
      assertTrue(call { module.getGenericPasswordForOptions(options(), it) } is ReadableMap)
    } else {
      assertEquals(Rejected("E_INVALID_PARAMETERS"), result)
      assertEquals(false, call { module.hasGenericPasswordForOptions(options(), it) })
    }
  }
}
