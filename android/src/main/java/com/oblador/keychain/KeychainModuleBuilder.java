package com.oblador.keychain;

import com.facebook.react.bridge.ReactApplicationContext;

public class KeychainModuleBuilder {
  private final ReactApplicationContext reactContext;
  private boolean useWarmingUp = true;

  public KeychainModuleBuilder(ReactApplicationContext reactContext) {
    this.reactContext = reactContext;
  }

  public KeychainModuleBuilder usingWarmUp(boolean useWarmUp) {
    this.useWarmingUp = useWarmUp;
    return this;
  }

  public KeychainModule build() {
    if (useWarmingUp) {
      return new KeychainModuleWithWarmUp(reactContext);
    } else {
      return new KeychainModule(reactContext);
    }
  }
}
