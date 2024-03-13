package com.oblador.keychain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oblador.keychain.cipherStorage.CipherStorage;

import java.util.Set;

public interface PrefsStorageBase {
  String KEYCHAIN_DATA = "RN_KEYCHAIN";

  class ResultSet extends CipherStorage.CipherResult<byte[]> {
    @KeychainModule.KnownCiphers
    public final String cipherStorageName;

    public ResultSet(@KeychainModule.KnownCiphers final String cipherStorageName, final byte[] usernameBytes, final byte[] passwordBytes) {
      super(usernameBytes, passwordBytes);

      this.cipherStorageName = cipherStorageName;
    }
  }

  @Nullable
  ResultSet getEncryptedEntry(@NonNull final String service);

  void removeEntry(@NonNull final String service);

  void storeEncryptedEntry(@NonNull final String service, @NonNull final CipherStorage.EncryptionResult encryptionResult);

  Set<String> getUsedCipherNames();

  @NonNull
  static String getKeyForUsername(@NonNull final String service) {
    return service + ":" + "u";
  }

  @NonNull
  static String getKeyForPassword(@NonNull final String service) {
    return service + ":" + "p";
  }

  @NonNull
  static String getKeyForCipherStorage(@NonNull final String service) {
    return service + ":" + "c";
  }

  static boolean isKeyForCipherStorage(@NonNull final String key) {
    return key.endsWith(":c");
  }
}
