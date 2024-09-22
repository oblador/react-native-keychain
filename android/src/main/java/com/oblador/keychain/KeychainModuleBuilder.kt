package com.oblador.keychain

import com.facebook.react.bridge.ReactApplicationContext

class KeychainModuleBuilder {
    private var reactContext: ReactApplicationContext? = null
    private var useWarmUp = DEFAULT_USE_WARM_UP
    fun withReactContext(reactContext: ReactApplicationContext?): KeychainModuleBuilder {
        this.reactContext = reactContext
        return this
    }

    fun usingWarmUp(): KeychainModuleBuilder {
        useWarmUp = true
        return this
    }

    fun withoutWarmUp(): KeychainModuleBuilder {
        useWarmUp = false
        return this
    }

    fun build(): KeychainModule {
        validate()
        return if (useWarmUp) {
            KeychainModule.withWarming(reactContext!!)
        } else {
            KeychainModule(reactContext!!)
        }
    }

    private fun validate() {
        if (reactContext == null) {
            throw Error("React Context was not provided")
        }
    }

    companion object {
        const val DEFAULT_USE_WARM_UP = true
    }
}
