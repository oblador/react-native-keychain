package com.oblador.keychain;

import java.security.Provider;
import java.util.HashMap;

public final class FakeProvider extends Provider {
  public static final String NAME = "AndroidKeyStore";
  public final HashMap<String, HashMap<String, MocksForProvider>> mocks = new HashMap<>();

  public FakeProvider() {
    super(NAME, 1.0, "Fake");

    put("KeyStore.AndroidKeyStore", FakeKeystore.class.getName());
  }

  @Override
  public synchronized Service getService(String type, String algorithm) {
    MocksForProvider mock;
    HashMap<String, MocksForProvider> inner;

    if (null == (inner = mocks.get(type))) {
      mocks.put(type, (inner = new HashMap<>()));
    }

    if (null == (mock = inner.get(algorithm))) {
      inner.put(algorithm, (mock = new MocksForProvider()));
    }

    mock.configure(type, this);

    return mock.service;
  }
}