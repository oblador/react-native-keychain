package com.oblador.keychain;

import com.facebook.react.bridge.ReactApplicationContext;

public class KeychainModuleBuilder {
  public static final boolean DEFAULT_USE_WARM_UP = true;

  private ReactApplicationContext reactContext;
  private boolean useWarmUp = DEFAULT_USE_WARM_UP;

  public KeychainModuleBuilder withReactContext(ReactApplicationContext reactContext) {
    this.reactContext = reactContext;
    return this;
  }

  public KeychainModuleBuilder usingWarmUp() {
    useWarmUp = true;
    return this;
  }

  public KeychainModuleBuilder withoutWarmUp() {
    useWarmUp = false;
    return this;
  }

  public KeychainModule build() {
    validate();
    if (useWarmUp) {
      return KeychainModule.withWarming(reactContext);
    } else {
      return new KeychainModule(reactContext);
    }
  }

  private void validate() {
    if (reactContext == null) {
      throw new Error("React Context was not provided");
    }
  }
}
