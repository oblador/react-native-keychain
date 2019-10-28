package com.oblador.keychain;

import java.security.Key;
import java.security.KeyFactorySpi;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public abstract class FakeKeyFactorySpi extends KeyFactorySpi {
  @Override
  protected <T extends KeySpec> T engineGetKeySpec(Key key, Class<T> keySpec) throws InvalidKeySpecException {
    return doEngineGetKeySpec(key, keySpec);
  }

  public abstract <T extends KeySpec> T doEngineGetKeySpec(Key key, Class<T> keySpec);
}
