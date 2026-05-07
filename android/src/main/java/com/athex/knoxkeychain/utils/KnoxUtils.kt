package com.athex.knoxkeychain.utils

import java.security.Security

object KnoxUtils {
    private const val TIMA_KEYSTORE = "TIMAKeyStore"
    private const val KNOX_ANDROID_KEYSTORE = "KnoxAndroidKeyStore"

    /** Checks if Samsung Knox (TIMA or Knox Vault) is available on the device. */
    fun isKnoxAvailable(): Boolean {
        // Check for TIMA KeyStore provider
        if (Security.getProvider(TIMA_KEYSTORE) != null) {
            return true
        }

        // Check for Knox Android KeyStore provider (newer devices)
        if (Security.getProvider(KNOX_ANDROID_KEYSTORE) != null) {
            return true
        }

        return false
    }

    /** Returns the name of the available Knox KeyStore provider, or null if none. */
    fun getKnoxKeyStoreName(): String? {
        if (Security.getProvider(TIMA_KEYSTORE) != null) {
            return TIMA_KEYSTORE
        }
        if (Security.getProvider(KNOX_ANDROID_KEYSTORE) != null) {
            return KNOX_ANDROID_KEYSTORE
        }
        return null
    }
}
