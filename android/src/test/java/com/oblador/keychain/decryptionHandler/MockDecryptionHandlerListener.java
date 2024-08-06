package com.oblador.keychain.decryptionHandler;

import androidx.annotation.NonNull;

import com.oblador.keychain.cipherStorage.CipherStorage;

public class MockDecryptionHandlerListener implements DecryptionResultListener {
  private CipherStorage.DecryptionResult result = null;
  private Throwable error = null;

  @Override
  public void onDecrypt(@NonNull CipherStorage.DecryptionResult decryptionResult) {
    this.result = decryptionResult;
  }

  @Override
  public void onError(@NonNull Throwable error) {
    this.error = error;
  }

  public CipherStorage.DecryptionResult getResult() {
    return result;
  }

  public Throwable getError() {
    return error;
  }
}
