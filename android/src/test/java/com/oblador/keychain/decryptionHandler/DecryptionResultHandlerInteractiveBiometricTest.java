package com.oblador.keychain.decryptionHandler;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;

import androidx.biometric.BiometricPrompt;

import com.facebook.react.bridge.ReactApplicationContext;
import com.oblador.keychain.cipherStorage.CipherStorage;
import com.oblador.keychain.cipherStorage.CipherStorageBase;
import com.oblador.keychain.cipherStorage.CipherStorageKeystoreRsaEcb;
import com.oblador.keychain.exceptions.CryptoFailedException;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class DecryptionResultHandlerInteractiveBiometricTest {

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testBiometryNotPermitted() {
    // GIVEN
    final KeyguardManager keyguardManager = mock(KeyguardManager.class);
    when(keyguardManager.isKeyguardSecure()).thenReturn(false);

    final ReactApplicationContext mockContext = mock(ReactApplicationContext.class);
    when(mockContext.getSystemService(Context.KEYGUARD_SERVICE)).thenReturn(keyguardManager);

    final CipherStorage storage = mock(CipherStorageBase.class);
    final BiometricPrompt.PromptInfo promptInfo = mock(BiometricPrompt.PromptInfo.class);
    final CipherStorage.DecryptionContext decryptionContext = mock(CipherStorage.DecryptionContext.class);

    // WHEN
    DecryptionResultHandlerInteractiveBiometric handler = new DecryptionResultHandlerInteractiveBiometric(mockContext, storage, promptInfo);
    MockDecryptionHandlerListener listener = new MockDecryptionHandlerListener();
    handler.askAccessPermissions(decryptionContext, listener);

    //THEN
    assertThat(listener.getResult(), is(nullValue()));
    assertThat(listener.getError(), Matchers.instanceOf(CryptoFailedException.class));
  }

  @Test(expected= NullPointerException.class)
  @Config(sdk = Build.VERSION_CODES.M)
  public void testBiometryAuthenticationErrorNoActivity() {
    // GIVEN
    final KeyguardManager keyguardManager = mock(KeyguardManager.class);
    when(keyguardManager.isKeyguardSecure()).thenReturn(true);

    final ReactApplicationContext mockContext = mock(ReactApplicationContext.class);
    when(mockContext.getSystemService(Context.KEYGUARD_SERVICE)).thenReturn(keyguardManager);
    when(mockContext.checkSelfPermission(Manifest.permission.USE_FINGERPRINT)).thenReturn(PERMISSION_GRANTED);

    final CipherStorage storage = mock(CipherStorageBase.class);
    final BiometricPrompt.PromptInfo promptInfo = mock(BiometricPrompt.PromptInfo.class);
    final CipherStorage.DecryptionContext decryptionContext = mock(CipherStorage.DecryptionContext.class);

    // WHEN
    DecryptionResultHandlerInteractiveBiometric handler = new DecryptionResultHandlerInteractiveBiometric(mockContext, storage, promptInfo);
    MockDecryptionHandlerListener listener = new MockDecryptionHandlerListener();
    handler.askAccessPermissions(decryptionContext, listener);
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testBiometryAuthenticationCancelled() {
    // GIVEN
    final KeyguardManager keyguardManager = mock(KeyguardManager.class);
    when(keyguardManager.isKeyguardSecure()).thenReturn(true);

    final ReactApplicationContext mockContext = mock(ReactApplicationContext.class);
    when(mockContext.getSystemService(Context.KEYGUARD_SERVICE)).thenReturn(keyguardManager);
    when(mockContext.checkSelfPermission(Manifest.permission.USE_FINGERPRINT)).thenReturn(PERMISSION_GRANTED);

    final CipherStorage storage = mock(CipherStorageBase.class);
    final BiometricPrompt.PromptInfo promptInfo = mock(BiometricPrompt.PromptInfo.class);

    // WHEN
    DecryptionResultHandlerInteractiveBiometric handler = new DecryptionResultHandlerInteractiveBiometric(mockContext, storage, promptInfo);
    MockDecryptionHandlerListener listener = new MockDecryptionHandlerListener();
    handler.listener = listener;
    handler.onAuthenticationError(ERROR_USER_CANCELED, "Authentication cancelled.");

    ShadowLooper shadowLooper = Shadows.shadowOf(handler.handler.getLooper());
    shadowLooper.runToEndOfTasks();

    //THEN
    assertThat(listener.getResult(), is(nullValue()));
    assertThat(listener.getError(), Matchers.instanceOf(CryptoFailedException.class));
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testBiometryAuthenticationSuccessfulNoContext() {
    // GIVEN
    final KeyguardManager keyguardManager = mock(KeyguardManager.class);
    when(keyguardManager.isKeyguardSecure()).thenReturn(true);

    final ReactApplicationContext mockContext = mock(ReactApplicationContext.class);
    when(mockContext.getSystemService(Context.KEYGUARD_SERVICE)).thenReturn(keyguardManager);
    when(mockContext.checkSelfPermission(Manifest.permission.USE_FINGERPRINT)).thenReturn(PERMISSION_GRANTED);

    final CipherStorageBase storage = mock(CipherStorageBase.class);

    final BiometricPrompt.PromptInfo promptInfo = mock(BiometricPrompt.PromptInfo.class);
    final BiometricPrompt.AuthenticationResult mockAuthResult = mock(BiometricPrompt.AuthenticationResult.class);

    // WHEN
    DecryptionResultHandlerInteractiveBiometric handler = new DecryptionResultHandlerInteractiveBiometric(mockContext, storage, promptInfo);
    MockDecryptionHandlerListener listener = new MockDecryptionHandlerListener();
    handler.listener = listener;
    handler.onAuthenticationSucceeded(mockAuthResult);

    ShadowLooper shadowLooper = Shadows.shadowOf(handler.handler.getLooper());
    shadowLooper.runToEndOfTasks();

    //THEN
    assertThat(listener.getResult(), is(nullValue()));
    assertThat(listener.getError(), Matchers.instanceOf(NullPointerException.class));
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testBiometryAuthenticationSuccessful() throws IOException, GeneralSecurityException {
    // GIVEN
    final String keyAlias = "key";
    final Key key = null;
    final String decryptedUsername = "username";
    final String decryptedPassword = "password";
    final byte[] username = decryptedUsername.getBytes();
    final byte[] password = decryptedPassword.getBytes();
    final CipherStorage.DecryptionContext decryptionContext = new CipherStorage.DecryptionContext(
      keyAlias, key, password, username);

    final KeyguardManager keyguardManager = mock(KeyguardManager.class);
    when(keyguardManager.isKeyguardSecure()).thenReturn(true);

    final ReactApplicationContext mockContext = mock(ReactApplicationContext.class);
    when(mockContext.getSystemService(Context.KEYGUARD_SERVICE)).thenReturn(keyguardManager);
    when(mockContext.checkSelfPermission(Manifest.permission.USE_FINGERPRINT)).thenReturn(PERMISSION_GRANTED);

    final CipherStorageBase storage = mock(CipherStorageKeystoreRsaEcb.class);
    when(storage.decryptBytes(key, username)).thenReturn(decryptedUsername);
    when(storage.decryptBytes(key, password)).thenReturn(decryptedPassword);

    final BiometricPrompt.PromptInfo promptInfo = mock(BiometricPrompt.PromptInfo.class);
    final BiometricPrompt.AuthenticationResult mockAuthResult = mock(BiometricPrompt.AuthenticationResult.class);

    DecryptionResultHandlerInteractiveBiometric handler = new DecryptionResultHandlerInteractiveBiometric(mockContext, storage, promptInfo);
    MockDecryptionHandlerListener listener = new MockDecryptionHandlerListener();
    // WHEN
    DecryptionResultHandlerInteractiveBiometric spy = spy(handler);
    // Can't mock BiometricPrompt stack at the moment
    doNothing().when(spy).startAuthentication();

    spy.askAccessPermissions(decryptionContext, listener);
    spy.onAuthenticationSucceeded(mockAuthResult);

    ShadowLooper shadowLooper = Shadows.shadowOf(handler.handler.getLooper());
    shadowLooper.runToEndOfTasks();

    //THEN
    CipherStorage.DecryptionResult result = listener.getResult();

    assertThat(result, is(notNullValue()));
    assertThat(result.username, is(decryptedUsername));
    assertThat(result.password, is(decryptedPassword));
    assertThat(listener.getError(), is(nullValue()));
  }
}
