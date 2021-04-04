package com.oblador.keychain.decryptionHandler;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;

import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import com.facebook.react.bridge.ReactApplicationContext;
import com.oblador.keychain.cipherStorage.CipherStorage;
import com.oblador.keychain.cipherStorage.CipherStorageBase;
import com.oblador.keychain.cipherStorage.CipherStorageKeystoreRsaEcb;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static androidx.biometric.BiometricConstants.ERROR_USER_CANCELED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class DecryptionResultHandlerInteractiveBiometricManualRetryTest {

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testBiometryNotRecognized() {
    // GIVEN
    final KeyguardManager keyguardManager = mock(KeyguardManager.class);
    when(keyguardManager.isKeyguardSecure()).thenReturn(true);

    final FragmentActivity mockActivity = mock(FragmentActivity.class);

    final BiometricPrompt mockBiometricPrompt = mock(BiometricPrompt.class);

    final ReactApplicationContext mockContext = mock(ReactApplicationContext.class);
    when(mockContext.getSystemService(Context.KEYGUARD_SERVICE)).thenReturn(keyguardManager);
    when(mockContext.checkSelfPermission(Manifest.permission.USE_FINGERPRINT)).thenReturn(PERMISSION_GRANTED);
    when(mockContext.getCurrentActivity()).thenReturn(mockActivity);

    final CipherStorageBase storage = mock(CipherStorageKeystoreRsaEcb.class);

    final BiometricPrompt.PromptInfo promptInfo = mock(BiometricPrompt.PromptInfo.class);
    final BiometricPrompt.AuthenticationResult mockAuthResult = mock(BiometricPrompt.AuthenticationResult.class);

    DecryptionResultHandlerInteractiveBiometricManualRetry handler = new DecryptionResultHandlerInteractiveBiometricManualRetry(mockContext, storage, promptInfo);

    // WHEN
    DecryptionResultHandlerInteractiveBiometricManualRetry spy = spy(handler);
    doReturn(mockBiometricPrompt).when(spy).authenticateWithPrompt(mockActivity);
    // Do not retry
    doNothing().when(spy).retryAuthentication();

    spy.startAuthentication();
    spy.onAuthenticationFailed();

    //THEN
    verify(mockBiometricPrompt, times(1)).cancelAuthentication();
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testBiometryNotRecognizedWithRetry() {
    // GIVEN
    final KeyguardManager keyguardManager = mock(KeyguardManager.class);
    when(keyguardManager.isKeyguardSecure()).thenReturn(true);

    final FragmentActivity mockActivity = mock(FragmentActivity.class);

    final BiometricPrompt mockBiometricPrompt = mock(BiometricPrompt.class);

    final ReactApplicationContext mockContext = mock(ReactApplicationContext.class);
    when(mockContext.getSystemService(Context.KEYGUARD_SERVICE)).thenReturn(keyguardManager);
    when(mockContext.checkSelfPermission(Manifest.permission.USE_FINGERPRINT)).thenReturn(PERMISSION_GRANTED);
    when(mockContext.getCurrentActivity()).thenReturn(mockActivity);

    final CipherStorageBase storage = mock(CipherStorageKeystoreRsaEcb.class);

    final BiometricPrompt.PromptInfo promptInfo = mock(BiometricPrompt.PromptInfo.class);
    final BiometricPrompt.AuthenticationResult mockAuthResult = mock(BiometricPrompt.AuthenticationResult.class);

    DecryptionResultHandlerInteractiveBiometricManualRetry handler = new DecryptionResultHandlerInteractiveBiometricManualRetry(mockContext, storage, promptInfo);

    // WHEN
    DecryptionResultHandlerInteractiveBiometricManualRetry spy = spy(handler);
    doReturn(mockBiometricPrompt).when(spy).authenticateWithPrompt(mockActivity);

    spy.startAuthentication();
    spy.onAuthenticationFailed();
    spy.onAuthenticationError(ERROR_USER_CANCELED, "Authentication cancelled.");

    //THEN
    verify(mockBiometricPrompt, times(1)).cancelAuthentication();
    verify(spy, times(2)).authenticateWithPrompt(mockActivity);

    assertThat(spy.getResult(), is(nullValue()));
    assertThat(spy.getError(), is(nullValue()));
  }
}
