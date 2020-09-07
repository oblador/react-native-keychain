package com.oblador.keychain;

import androidx.annotation.NonNull;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.oblador.keychain.workaround.Issue314Filter;
import com.oblador.keychain.workaround.Issue337Filter;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class KeychainPackage implements ReactPackage {

  public KeychainPackage() {

  }

  @Override
  @NonNull
  public List<NativeModule> createNativeModules(@NonNull final ReactApplicationContext reactContext) {
    return Collections.singletonList(new KeychainModuleBuilder(reactContext)
        .workaroundAffectedDevices(
          new Issue314Filter(),
          new Issue337Filter())
        .build());
  }

  @NonNull
  public List<Class<? extends JavaScriptModule>> createJSModules() {
    return Collections.emptyList();
  }

  @Override
  @NonNull
  public List<ViewManager> createViewManagers(@NonNull final ReactApplicationContext reactContext) {
    return Collections.emptyList();
  }
}
