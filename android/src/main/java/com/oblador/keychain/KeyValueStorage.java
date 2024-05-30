package com.oblador.keychain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oblador.keychain.KeychainModule.KnownCiphers;
import com.oblador.keychain.cipherStorage.CipherStorage;
import com.oblador.keychain.cipherStorage.CipherStorage.EncryptionResult;

import java.util.Set;

@SuppressWarnings({"unused", "WeakerAccess"})
public interface KeyValueStorage {
  class ResultSet extends CipherStorage.CipherResult<byte[]> {
    @KnownCiphers
    public final String cipherStorageName;

    public ResultSet(@KnownCiphers final String cipherStorageName, final byte[] usernameBytes, final byte[] passwordBytes) {
      super(usernameBytes, passwordBytes);

      this.cipherStorageName = cipherStorageName;
    }
  }

  @Nullable
  ResultSet getEncryptedEntry(@NonNull final String service);

  void removeEntry(@NonNull final String service);

  void storeEncryptedEntry(@NonNull final String service, @NonNull final EncryptionResult encryptionResult);

  @NonNull
  Set<String> getUsedCipherNames();

  @NonNull
  default String getKeyForUsername(@NonNull final String service) {
    return service + ":u";
  }

  @NonNull
  default String getKeyForPassword(@NonNull final String service) {
    return service + ":p";
  }

  @NonNull
  default String getKeyForCipherStorage(@NonNull final String service) {
    return service + ":c";
  }

  default boolean isKeyForCipherStorage(@NonNull final String key) {
    return key.endsWith(":c");
  }
}
