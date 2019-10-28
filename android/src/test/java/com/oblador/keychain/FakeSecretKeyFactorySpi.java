package com.oblador.keychain;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactorySpi;

public abstract class FakeSecretKeyFactorySpi extends SecretKeyFactorySpi {

  @Override
  protected KeySpec engineGetKeySpec(SecretKey key, Class<?> keySpec) throws InvalidKeySpecException {
    return doEngineGetKeySpec(key, keySpec);
  }

  public abstract KeySpec doEngineGetKeySpec(SecretKey key, Class<?> keySpec);
}
