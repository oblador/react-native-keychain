package com.oblador.keychain.decryptionHandler;

import android.os.Build;

import androidx.biometric.BiometricPrompt;

import com.facebook.react.bridge.ReactApplicationContext;
import com.oblador.keychain.cipherStorage.CipherStorage;
import com.oblador.keychain.cipherStorage.CipherStorageBase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class DecryptionResultHandlerProviderTest {

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testBiometryNotSupported() {
    // GIVEN
    final ReactApplicationContext mockContext = mock(ReactApplicationContext.class);
    final CipherStorage storage = mock(CipherStorage.class);
    when(storage.isBiometrySupported()).thenReturn(false);

    final BiometricPrompt.PromptInfo promptInfo = mock(BiometricPrompt.PromptInfo.class);

    // WHEN
    DecryptionResultHandler handler = DecryptionResultHandlerProvider.getHandler(mockContext, storage, promptInfo);

    //THEN
    assertThat(handler, instanceOf(DecryptionResultHandlerNonInteractive.class));
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testBiometryWithOnePlusBug() {
    // GIVEN
    ReflectionHelpers.setStaticField(android.os.Build.class, "BRAND", "OnePlus");
    ReflectionHelpers.setStaticField(android.os.Build.class, "MODEL", "ONEPLUS A6010"); // OnePlus 6T

    final ReactApplicationContext mockContext = mock(ReactApplicationContext.class);
    final CipherStorage storage = mock(CipherStorageBase.class);
    when(storage.isBiometrySupported()).thenReturn(true);

    final BiometricPrompt.PromptInfo promptInfo = mock(BiometricPrompt.PromptInfo.class);

    // WHEN
    DecryptionResultHandler handler = DecryptionResultHandlerProvider.getHandler(mockContext, storage, promptInfo);

    //THEN
    assertThat(handler, instanceOf(DecryptionResultHandlerInteractiveBiometricManualRetry.class));
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testBiometryWithoutBug() {
    // GIVEN
    ReflectionHelpers.setStaticField(android.os.Build.class, "BRAND", "OnePlus");
    ReflectionHelpers.setStaticField(android.os.Build.class, "MODEL", "ONEPLUS A6000"); // OnePlus 6

    final ReactApplicationContext mockContext = mock(ReactApplicationContext.class);
    final CipherStorage storage = mock(CipherStorageBase.class);
    when(storage.isBiometrySupported()).thenReturn(true);

    final BiometricPrompt.PromptInfo promptInfo = mock(BiometricPrompt.PromptInfo.class);

    // WHEN
    DecryptionResultHandler handler = DecryptionResultHandlerProvider.getHandler(mockContext, storage, promptInfo);

    //THEN
    assertThat(handler, instanceOf(DecryptionResultHandlerInteractiveBiometric.class));
  }
}
