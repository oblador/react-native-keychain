package com.oblador.keychain.decryptionHandler;

import androidx.annotation.NonNull;

import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionContext;
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionResult;
import com.oblador.keychain.exceptions.CryptoFailedException;

public class DecryptionResultHandlerNonInteractive implements DecryptionResultHandler {
  @Override
  public void askAccessPermissions(@NonNull final DecryptionContext context,
                                   @NonNull final DecryptionResultListener listener) {
    final CryptoFailedException failure = new CryptoFailedException(
      "Non interactive decryption mode.");

    listener.onError(failure);
  }
}
