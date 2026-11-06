package com.oblador.keychain

import android.content.Context
import android.content.ContextWrapper
import com.facebook.react.bridge.*
import com.facebook.react.turbomodule.core.interfaces.CallInvokerHolder
import java.io.File
import java.util.UUID

/** Real Android storage, isolated per test. No React runtime is needed by native storage tests. */
internal class StorageTestContext(base: Context) : ContextWrapper(base) {
  private val id = "keychain-test-${UUID.randomUUID()}"
  private val directory = File(base.cacheDir, id).apply { mkdirs() }
  override fun getApplicationContext(): Context = this
  override fun getFilesDir(): File = directory
  override fun getSharedPreferences(name: String, mode: Int) =
    super.getSharedPreferences("$id-$name", mode)

  fun close() {
    getSharedPreferences(PrefsStorageBase.KEYCHAIN_DATA, MODE_PRIVATE).edit().clear().commit()
    directory.deleteRecursively()
  }
}

/** Fail loudly if a native test accidentally starts depending on a JS runtime. */
internal class NativeTestContext(context: Context) : ReactApplicationContext(context) {
  override fun <T : JavaScriptModule> getJSModule(type: Class<T>): T = error("No JS runtime")
  override fun <T : NativeModule> hasNativeModule(type: Class<T>) = false
  override fun getNativeModules(): Collection<NativeModule> = emptyList()
  override fun <T : NativeModule> getNativeModule(type: Class<T>): T = error("No JS runtime")
  override fun getNativeModule(name: String): NativeModule? = null
  override fun getCatalystInstance(): CatalystInstance = error("No legacy bridge")
  override fun hasActiveCatalystInstance() = false
  override fun hasActiveReactInstance() = false
  override fun hasCatalystInstance() = false
  override fun hasReactInstance() = false
  override fun destroy() = Unit
  override fun handleException(exception: Exception): Unit = throw exception
  override fun isBridgeless() = true
  override fun getJavaScriptContextHolder(): JavaScriptContextHolder? = null
  override fun getJSCallInvokerHolder(): CallInvokerHolder? = null
  override fun getFabricUIManager(): UIManager? = null
  override fun getSourceURL(): String? = null
  override fun registerSegment(id: Int, path: String, callback: Callback): Unit = error("No JS runtime")
}
