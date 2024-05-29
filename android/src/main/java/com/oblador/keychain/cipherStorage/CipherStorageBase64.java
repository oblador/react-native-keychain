package com.oblador.keychain.cipherStorage;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.oblador.keychain.KeychainModule;
import com.oblador.keychain.SecurityLevel;
import com.oblador.keychain.decryptionHandler.DecryptionResultHandler;
import com.oblador.keychain.exceptions.CryptoFailedException;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/** When using Google BlockStore API storage encrytpion is not required as data will be stored safely by BlockStore.
 * This storage just encode/decode data using Base64.
 * @see <a href="https://developers.google.com/identity/blockstore/android">BlockStore Docs</a>
 * */
public class CipherStorageBase64 implements CipherStorage {

  private static final String TAG = CipherStorageBase64.class.getSimpleName();


  //region Overrides
  @Override
  @NonNull
  public EncryptionResult encrypt(@NonNull final String alias,
                                  @NonNull final String username,
                                  @NonNull final String password,
                                  @NonNull final SecurityLevel level) {
    byte[] usernameBytes = username.getBytes(StandardCharsets.UTF_8);
    byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);

    byte[] usernameEncodedBytes = Base64.encode(usernameBytes, Base64.DEFAULT);
    byte[] passwordEncodedBytes = Base64.encode(passwordBytes, Base64.DEFAULT);
    return new EncryptionResult(usernameEncodedBytes, passwordEncodedBytes, this);
  }

  @NonNull
  @Override
  public DecryptionResult decrypt(@NonNull String alias,
                                  @NonNull byte[] username,
                                  @NonNull byte[] password,
                                  @NonNull final SecurityLevel level)
    throws CryptoFailedException {
    byte[] usernameDecodedBytes = Base64.decode(username, Base64.DEFAULT);
    byte[] passwordDecodedBytes = Base64.decode(password, Base64.DEFAULT);

    String usernameDecoded = new String(usernameDecodedBytes, StandardCharsets.UTF_8);
    String passwordDecoded = new String(passwordDecodedBytes, StandardCharsets.UTF_8);
    return new DecryptionResult(usernameDecoded, passwordDecoded);
  }

  @Override
  public void removeKey(@NonNull String alias) {
    // nothing to remove, as we don't store decryption keys
  }

  @Override
  public Set<String> getAllKeys() {
    return new HashSet<>(); // return empty set, as we don't store decryption keys
  }

  @Override
  public SecurityLevel securityLevel() {
    return SecurityLevel.SECURE_SOFTWARE;
  }

  @Override
  public boolean supportsSecureHardware() {
    return false;
  }

  /**
   * COPY PASTE FROM [com.oblador.keychain.cipherStorage.CipherStorageBase]
   * The higher value means better capabilities. Range: [19..1129].
   * Formula: `1000 * isBiometrySupported() + 100 * isSecureHardware() + minSupportedApiLevel()`
   */
  @Override
  public int getCapabilityLevel() {
    return
      (1000 * (isBiometrySupported() ? 1 : 0)) + // 0..1000
        (getMinSupportedApiLevel()); // 19..29
  }

  @Override
  public String getDefaultAliasServiceName() {
    return KeychainModule.KnownCiphers.BS;
  }

  @Override
  public String getCipherStorageName() {
    return KeychainModule.KnownCiphers.BS;
  }

  @Override
  @SuppressLint("NewApi")
  public void decrypt(@NonNull DecryptionResultHandler handler,
                      @NonNull String alias,
                      @NonNull byte[] username,
                      @NonNull byte[] password,
                      @NonNull final SecurityLevel level)
    throws CryptoFailedException {
    handler.onDecrypt(decrypt(alias, username, password, level), null);
  }

  /** API28 is a requirement for End-to-end Google BlockStore encryption, so don't support lower levels. */
  @Override
  public int getMinSupportedApiLevel() {
    return Build.VERSION_CODES.P;
  }

  @Override
  public boolean isBiometrySupported() {
    return false;
  }
}
