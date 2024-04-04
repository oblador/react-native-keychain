package com.oblador.keychain.decryptionHandler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionContext;
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionResult;
import com.oblador.keychain.exceptions.CryptoFailedException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;

public class DecryptionResultHandlerNonInteractive implements DecryptionResultHandler {
  @Override
  public CompletableFuture<DecryptionResult> authenticate(@NonNull DecryptionResultJob job) {
    final CompletableFuture<DecryptionResult> result = new CompletableFuture<>();
    try {
      result.complete(job.get());
    } catch (final Throwable fail) {
      result.completeExceptionally(fail);
    }
    return result;
  }
}
