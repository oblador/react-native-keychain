package com.oblador.keychain.decryptionHandler;

import androidx.annotation.NonNull;

import com.oblador.keychain.cipherStorage.CipherStorage;

public interface DecryptionResultListener {
  /** Triggered when decryption has finished successfully */
  void onDecrypt(@NonNull final CipherStorage.DecryptionResult decryptionResult);
  /** Triggered when error occured during decryption */
  void onError(@NonNull final Throwable error);
}
