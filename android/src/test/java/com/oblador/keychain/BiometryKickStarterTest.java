package com.oblador.keychain;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BiometryKickStarterTest {

    @Test
    public void tryStartBiometry_fast() throws InterruptedException {
      // GIVEN:
      // the init is quicker than the timeout
      InhibitableBiometricCapabilitiesHelper mockHelper = mock(InhibitableBiometricCapabilitiesHelper.class);
      doNothing().when(mockHelper).initialize();

      BiometryKickStarter kickStarter = new BiometryKickStarter(mockHelper);

      // WHEN
      kickStarter.tryStartBiometry(100);

      // THEN:
      // the helper is never inhibited; one call to resume is allowed
      kickStarter.thread.join();
      ArgumentCaptor<Boolean> invocation = ArgumentCaptor.forClass(Boolean.class);
      verify(mockHelper, atMostOnce()).setInhibited(invocation.capture());
      assertFalse(invocation.getValue());
    }

    @Test
    public void tryStartBiometry_slow() throws InterruptedException {
      // GIVEN:
      // the init takes longer than the timeout
      InhibitableBiometricCapabilitiesHelper mockHelper = mock(InhibitableBiometricCapabilitiesHelper.class);
      doAnswer(new AnswersWithDelay(200, invocation -> null)).when(mockHelper).initialize();

      BiometryKickStarter kickStarter = new BiometryKickStarter(mockHelper);

      // WHEN
      kickStarter.tryStartBiometry(100);

      // THEN 1:
      // the helper is inhibited until the init completes
      InOrder inOrder = Mockito.inOrder(mockHelper);
      ArgumentCaptor<Boolean> invocation1 = ArgumentCaptor.forClass(Boolean.class);
      inOrder.verify(mockHelper).setInhibited(invocation1.capture());
      assertTrue(invocation1.getValue());

      // THEN 2:
      // the init completes, and the helper is resumed
      kickStarter.thread.join();
      ArgumentCaptor<Boolean> invocation2 = ArgumentCaptor.forClass(Boolean.class);
      inOrder.verify(mockHelper).setInhibited(invocation2.capture());
      assertFalse(invocation2.getValue());

    }
}
