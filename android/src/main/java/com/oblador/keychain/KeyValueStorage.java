package com.oblador.keychain;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.google.android.gms.auth.blockstore.Blockstore;
import com.google.android.gms.auth.blockstore.BlockstoreClient;
import com.google.android.gms.auth.blockstore.DeleteBytesRequest;
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest;
import com.google.android.gms.auth.blockstore.RetrieveBytesResponse;
import com.google.android.gms.auth.blockstore.StoreBytesData;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.oblador.keychain.KeychainModule.KnownCiphers;
import com.oblador.keychain.cipherStorage.CipherStorage;
import com.oblador.keychain.cipherStorage.CipherStorage.EncryptionResult;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
  static String getKeyForUsername(@NonNull final String service) {
    return service + ":u";
  }

  @NonNull
  static String getKeyForPassword(@NonNull final String service) {
    return service + ":p";
  }

  @NonNull
  static String getKeyForCipherStorage(@NonNull final String service) {
    return service + ":c";
  }

  static boolean isKeyForCipherStorage(@NonNull final String key) {
    return key.endsWith(":c");
  }
}
