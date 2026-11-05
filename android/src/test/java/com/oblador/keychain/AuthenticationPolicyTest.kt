package com.oblador.keychain

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import com.oblador.keychain.KeychainModule.AccessControl
import com.oblador.keychain.KeychainModule.Errors
import com.oblador.keychain.exceptions.KeychainException
import org.junit.Assert.*
import org.junit.Test

class AuthenticationPolicyTest {
  @Test fun omittedReadOptionsPreserveSavedPolicies() {
    for (policy in listOf(AccessControl.BIOMETRY_ANY, AccessControl.DEVICE_PASSCODE,
      AccessControl.BIOMETRY_ANY_OR_DEVICE_PASSCODE)) {
      val stored = AuthenticationPolicy.forWrite(policy, true, 35)
      assertEquals(stored, AuthenticationPolicy.forRead(null, stored, true, 35))
      assertEquals(stored, AuthenticationPolicy.forRead(AccessControl.NONE, stored, true, 35))
    }
  }

  @Test fun explicitReadCannotBroadenBiometricOnlyPolicy() {
    assertEquals(BIOMETRIC_STRONG, AuthenticationPolicy.forRead(
      AccessControl.BIOMETRY_ANY_OR_DEVICE_PASSCODE, BIOMETRIC_STRONG, true, 35))
    val error = assertThrows(KeychainException::class.java) {
      AuthenticationPolicy.forRead(AccessControl.DEVICE_PASSCODE, BIOMETRIC_STRONG, true, 35)
    }
    assertEquals(Errors.E_INVALID_PARAMETERS, error.errorCode)
  }

  @Test fun legacyEntriesUseCompatibleStrongAuthenticators() {
    assertEquals(BIOMETRIC_STRONG or DEVICE_CREDENTIAL,
      AuthenticationPolicy.forRead(null, null, true, 30))
    for (sdk in listOf(24, 28, 29)) {
      assertEquals(BIOMETRIC_STRONG, AuthenticationPolicy.forRead(null, null, true, sdk))
    }
    assertEquals(DEVICE_CREDENTIAL,
      AuthenticationPolicy.forRead(AccessControl.DEVICE_PASSCODE, null, true, 35))
  }

  @Test fun unauthenticatedStorageDoesNotAcquireAuthentication() {
    assertNull(AuthenticationPolicy.forRead(null, null, false, 35))
    assertNull(AuthenticationPolicy.forWrite(null, false, 35))
  }
}
