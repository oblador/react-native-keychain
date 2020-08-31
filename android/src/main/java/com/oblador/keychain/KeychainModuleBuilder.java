package com.oblador.keychain;

import com.facebook.react.bridge.ReactApplicationContext;

public class KeychainModuleBuilder {
  private ReactApplicationContext reactContext;

  public KeychainModuleBuilder withReactContext(ReactApplicationContext reactContext) {
    this.reactContext = reactContext;
    return this;
  }

  public KeychainModule build() {
    validate();
    return KeychainModule.withWarming(reactContext);
  }

  private void validate() {
    if (reactContext == null) {
      throw new Error("React Context was not provided");
    }
  }
}
