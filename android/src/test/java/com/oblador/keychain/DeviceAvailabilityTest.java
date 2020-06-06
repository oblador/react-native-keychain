package com.oblador.keychain;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class DeviceAvailabilityTest {


  /**
   *     Fingerprint hardware not available. Minimal API for fingerprint is api23 (Android 6.0).
   *     https://developer.android.com/about/versions/marshmallow/android-6.0
   */
  @Test
  @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
  public void isFingerprintAuthAvailable_beforeApi23() {
    // GIVEN
    final Context context = ApplicationProvider.getApplicationContext();

    // WHEN
    final boolean result = DeviceAvailability.isFingerprintAuthAvailable(context);

    // THEN
    assertThat(result, is(false));
  }

    @Test
    @Config(sdk = Build.VERSION_CODES.M)
    public void isFingerprintAuthAvailable_Api23() {
      // GIVEN
      Context context = ApplicationProvider.getApplicationContext();

      FingerprintManager fm = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
      shadowOf(fm).setIsHardwareDetected(true);

      // WHEN
      boolean result = DeviceAvailability.isFingerprintAuthAvailable(context);

      // THEN
      assertThat(result, is(true));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.P)
    public void isFingerprintAuthAvailable_afterApi23() {
      // GIVEN
      Context context = mock(Context.class);
      PackageManager mockPackageManager = mock(PackageManager.class);
      when(context.getPackageManager()).thenReturn(mockPackageManager);
      when(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)).thenReturn(true);

      // WHEN
      boolean result = DeviceAvailability.isFingerprintAuthAvailable(context);

      // THEN
      assertThat(result, is(true));
    }



}
