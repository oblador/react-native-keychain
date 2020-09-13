package com.oblador.keychain;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.oblador.keychain.KeychainModule.KnownCiphers;
import com.oblador.keychain.cipherStorage.CipherStorage;
import com.oblador.keychain.cipherStorage.CipherStorage.EncryptionResult;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings({"unused", "WeakerAccess"})
public class PrefsStorage {
  public static final String KEYCHAIN_DATA = "RN_KEYCHAIN";

  static public class ResultSet extends CipherStorage.CipherResult<byte[]> {
    @KnownCiphers
    public final String cipherStorageName;

    public ResultSet(@KnownCiphers final String cipherStorageName, final byte[] usernameBytes, final byte[] passwordBytes) {
      super(usernameBytes, passwordBytes);

      this.cipherStorageName = cipherStorageName;
    }
  }

  @NonNull
  private final SharedPreferences prefs;

  public PrefsStorage(@NonNull final ReactApplicationContext reactContext) {
    this.prefs = reactContext.getSharedPreferences(KEYCHAIN_DATA, Context.MODE_PRIVATE);
  }

  @Nullable
  public ResultSet getEncryptedEntry(@NonNull final String service) {
    byte[] bytesForUsername = getBytesForUsername(service);
    byte[] bytesForPassword = getBytesForPassword(service);
    String cipherStorageName = getCipherStorageName(service);

    // in case of wrong password or username
    if (bytesForUsername == null || bytesForPassword == null) {
      return null;
    }

    if (cipherStorageName == null) {
      // If the CipherStorage name is not found, we assume it is because the entry was written by an older
      // version of this library. The older version used Facebook Conceal, so we default to that.
      cipherStorageName = KnownCiphers.FB;
    }

    return new ResultSet(cipherStorageName, bytesForUsername, bytesForPassword);

  }

  public void removeEntry(@NonNull final String service) {
    final String keyForUsername = getKeyForUsername(service);
    final String keyForPassword = getKeyForPassword(service);
    final String keyForCipherStorage = getKeyForCipherStorage(service);

    prefs.edit()
      .remove(keyForUsername)
      .remove(keyForPassword)
      .remove(keyForCipherStorage)
      .apply();
  }

  public void storeEncryptedEntry(@NonNull final String service, @NonNull final EncryptionResult encryptionResult) {
    final String keyForUsername = getKeyForUsername(service);
    final String keyForPassword = getKeyForPassword(service);
    final String keyForCipherStorage = getKeyForCipherStorage(service);

    prefs.edit()
      .putString(keyForUsername, Base64.encodeToString(encryptionResult.username, Base64.DEFAULT))
      .putString(keyForPassword, Base64.encodeToString(encryptionResult.password, Base64.DEFAULT))
      .putString(keyForCipherStorage, encryptionResult.cipherName)
      .apply();
  }

  /**
   * List all types of cipher which are involved in en/decryption of the data stored herein.
   *
   * A cipher type is stored together with the datum upon encryption so the datum can later be decrypted using correct
   * cipher. This way, a {@link PrefsStorage} can involve different ciphers for different data. This method returns all
   * ciphers involved with this storage.
   *
   * @return set of cipher names
   */
  public Set<String> getUsedCipherNames() {
    Set<String> result = new HashSet<>();

    Set<String> keys = prefs.getAll().keySet();
    for (String key : keys) {
      if (isKeyForCipherStorage(key)) {
        String cipher = prefs.getString(key, null);
        result.add(cipher);
      }
    }

    return result;
  }

  @Nullable
  private byte[] getBytesForUsername(@NonNull final String service) {
    final String key = getKeyForUsername(service);

    return getBytes(key);
  }

  @Nullable
  private byte[] getBytesForPassword(@NonNull final String service) {
    String key = getKeyForPassword(service);
    return getBytes(key);
  }

  @Nullable
  private String getCipherStorageName(@NonNull final String service) {
    String key = getKeyForCipherStorage(service);

    return this.prefs.getString(key, null);
  }

  @NonNull
  public static String getKeyForUsername(@NonNull final String service) {
    return service + ":" + "u";
  }

  @NonNull
  public static String getKeyForPassword(@NonNull final String service) {
    return service + ":" + "p";
  }

  @NonNull
  public static String getKeyForCipherStorage(@NonNull final String service) {
    return service + ":" + "c";
  }

  public static boolean isKeyForCipherStorage(@NonNull final String key) {
    return key.endsWith(":c");
  }

  @Nullable
  private byte[] getBytes(@NonNull final String key) {
    String value = this.prefs.getString(key, null);

    if (value != null) {
      return Base64.decode(value, Base64.DEFAULT);
    }

    return null;
  }
}
