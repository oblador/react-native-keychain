package com.oblador.keychain;

import android.content.Context;

import com.oblador.keychain.BiometricCapabilitiesHelper.CapabilitiesChangeListener;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BiometricCapabilitiesHelperTest {

    @Test
    public void testNotifyCapabilitiesChanged_callsListener() {
      // GIVEN
      Context mockContext = mock(Context.class);
      CapabilitiesChangeListener mockListener = mock(CapabilitiesChangeListener.class);

      BiometricCapabilitiesHelper helper = new BiometricCapabilitiesHelper(mockContext);
      helper.setCapabilitiesChangeListener(mockListener);

      // WHEN
      helper.notifyCapabilitiesChanged();

      // THEN
      verify(mockListener).onBiometricCapabilitiesChanged(helper);
    }
}
