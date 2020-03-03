package com.oblador.keychain;

import javax.crypto.KeyGeneratorSpi;
import javax.crypto.SecretKey;

public abstract class FakeKeyGeneratorSpi extends KeyGeneratorSpi {
  @Override
  protected SecretKey engineGenerateKey() {
    return doEngineGenerateKey();
  }

  public abstract SecretKey doEngineGenerateKey();
}
