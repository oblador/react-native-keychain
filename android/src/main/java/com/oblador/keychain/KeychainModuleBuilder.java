package com.oblador.keychain;

import com.facebook.react.bridge.ReactApplicationContext;
import com.oblador.keychain.workaround.IDeviceFilter;
import com.oblador.keychain.workaround.ListDeviceFilter;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class KeychainModuleBuilder {
  private final ReactApplicationContext reactContext;
  private final List<IDeviceFilter> filters = new ArrayList<>();
  private boolean useWarmingUp = true;

  public KeychainModuleBuilder(ReactApplicationContext reactContext) {
    this.reactContext = reactContext;
  }

  public KeychainModuleBuilder workaroundAffectedDevices(IDeviceFilter... filters) {
    this.filters.addAll(asList(filters));
    return this;
  }

  public KeychainModuleBuilder usingWarmUp(boolean useWarmUp) {
    this.useWarmingUp = useWarmUp;
    return this;
  }

  public KeychainModule build() {
    ListDeviceFilter filter = new ListDeviceFilter(filters);
    if (useWarmingUp) {
      return new KeychainModuleWithWarmUp(reactContext, filter);
    } else {
      return new KeychainModule(reactContext, filter);
    }
  }
}
