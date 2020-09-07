package com.oblador.keychain.workaround;

import android.os.Build;

/**
 * This filter disables the Cipher implementation for certain devices which reportedly suffer bad performance.
 *
 * From https://github.com/oblador/react-native-keychain/issues/314:
 * "Android UI freezing on launch"
 *
 * Known affected devices:
 * Samsung A20 Android 10  - warmup time was anything between 5k-40k ms. Android 8 and 9 were not visibly affected.
 * Huawei y7 pro - warmup time 20s
 * OnePlus 5T (A5010) running OxygenOS 9.0.10 - warming up takes: 17296 ms
 *
 * @deprecated Must be removed as soon as Issue 314 is fixed.
 */
@Deprecated
public class Issue314Filter implements IDeviceFilter {
  private static final int ANDROID_9  = 28;
  private static final int ANDROID_10 = 29;

  @Override
  public boolean isDeviceAffected() {
    // Samsung A20 running  Android 10
    if ("SAMSUNG".equalsIgnoreCase(Build.MANUFACTURER) && Build.MODEL.startsWith("SM-A205") && Build.VERSION.SDK_INT == ANDROID_10) {
      return true;
    }

    // Huawei y7 pro
    if ("HUAWEI".equalsIgnoreCase(Build.MANUFACTURER) && Build.MODEL.contains("HWY7PRO")) {
      return true;
    }

    // OnePlus 5T (A5010) running OxygenOS 9.0.10
    if ("OnePlus5".equalsIgnoreCase(Build.PRODUCT) && Build.VERSION.SDK_INT == ANDROID_9) {
      return true;
    }

    return false;
  }
}
