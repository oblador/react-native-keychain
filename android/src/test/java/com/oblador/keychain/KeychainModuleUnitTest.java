package com.oblador.keychain;

import com.facebook.react.bridge.ReactApplicationContext;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class KeychainModuleUnitTest {
  @Test
  public void ensureCapabilitiesListenerInstalled() {
    // GIVEN
    ReactApplicationContext mockContext = mock(ReactApplicationContext.class);
    BiometricCapabilitiesHelper mockHelper = mock(BiometricCapabilitiesHelper.class);

    // WHEN
    KeychainModule module = new KeychainModule(mockContext, mockHelper);

    // THEN
    verify(mockHelper).setCapabilitiesChangeListener(module);
  }
}
