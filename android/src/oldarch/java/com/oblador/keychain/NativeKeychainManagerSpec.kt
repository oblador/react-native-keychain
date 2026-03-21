package com.oblador.keychain

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReadableMap

abstract class NativeKeychainManagerSpec(context: ReactApplicationContext) :
  ReactContextBaseJavaModule(context) {
  abstract fun setGenericPasswordForOptions(
    options: ReadableMap?,
    username: String,
    password: String,
    promise: Promise
  )
  abstract fun getGenericPasswordForOptions(options: ReadableMap?, promise: Promise)
  abstract fun hasGenericPasswordForOptions(options: ReadableMap?, promise: Promise)
  abstract fun resetGenericPasswordForOptions(options: ReadableMap?, promise: Promise)
  abstract fun getAllGenericPasswordServices(options: ReadableMap?, promise: Promise)
  abstract fun hasInternetCredentialsForOptions(options: ReadableMap, promise: Promise)
  abstract fun setInternetCredentialsForServer(
    server: String,
    username: String,
    password: String,
    options: ReadableMap?,
    promise: Promise
  )
  abstract fun getInternetCredentialsForServer(server: String, options: ReadableMap?, promise: Promise)
  abstract fun resetInternetCredentialsForOptions(options: ReadableMap, promise: Promise)
  abstract fun getSupportedBiometryType(promise: Promise)
  abstract fun getSecurityLevel(options: ReadableMap?, promise: Promise)
  abstract fun isPasscodeAuthAvailable(promise: Promise)
}
