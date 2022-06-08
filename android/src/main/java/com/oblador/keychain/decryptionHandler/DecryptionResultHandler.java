package com.oblador.keychain.decryptionHandler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionContext;
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionResult;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/** Handler that allows to inject some actions during decrypt operations. */
public interface DecryptionResultHandler {
  /** Ask user for interaction, often its unlock of keystore by biometric data providing. */
  CompletableFuture<DecryptionResult> authenticate(@NonNull final DecryptionResultJob job);
}
