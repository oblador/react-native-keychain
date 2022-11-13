package com.oblador.keychain.decryptionHandler;

import androidx.annotation.NonNull;

import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionContext;

/** Handler that allows to inject some actions during decrypt operations. */
public interface DecryptionResultHandler {
  /** Ask user for interaction, often its unlock of keystore by biometric data providing. */
  void askAccessPermissions(@NonNull final DecryptionContext context,
                            @NonNull final DecryptionResultListener listener);

}
