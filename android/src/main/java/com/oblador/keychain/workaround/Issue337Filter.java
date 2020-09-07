package com.oblador.keychain.workaround;

import android.os.Build;

/**
 * This filter disables the Cipher implementation for certain devices which reportedly suffer bad performance.
 *
 * From https://github.com/oblador/react-native-keychain/issues/337:
 * "getGenericPassword is too slow on android 10"
 *
 * Known affected devices:
 * Samsung SM-J730F (API28) it takes way beyond 10 seconds
 * Samsung Galaxy S10+, running Android 10 - 3637ms average over 5 runs.
 *
 * @deprecated Must be removed as soon as Issue 314 is fixed.
 */
@Deprecated
public class Issue337Filter implements IDeviceFilter {
  private static final int API28 = 28;
  private static final int ANDROID_10 = 29;

  @Override
  public boolean isDeviceAffected() {
    // Samsung SM-J730F (API28)
    if ("SAMSUNG".equalsIgnoreCase(Build.MANUFACTURER) && "SM-J730F".equalsIgnoreCase(Build.MODEL) && Build.VERSION.SDK_INT == API28) {
      return true;
    }

    // Samsung Galaxy S10+, running Android 10
    if ("SAMSUNG".equalsIgnoreCase(Build.MANUFACTURER) && "SM-G975F".equalsIgnoreCase(Build.MODEL) && Build.VERSION.SDK_INT == ANDROID_10) {
      return true;
    }

    return false;
  }
}
