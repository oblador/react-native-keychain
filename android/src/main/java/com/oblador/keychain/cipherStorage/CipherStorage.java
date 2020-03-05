package com.oblador.keychain.cipherStorage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oblador.keychain.SecurityLevel;
import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

import java.security.Key;

@SuppressWarnings({"unused", "WeakerAccess"})
public interface CipherStorage {
  //region Helper classes

  /** basis for storing credentials in different data type formats. */
  abstract class CipherResult<T> {
    public final T username;
    public final T password;

    public CipherResult(final T username, final T password) {
      this.username = username;
      this.password = password;
    }
  }

  /** Credentials in bytes array, often a result of encryption. */
  class EncryptionResult extends CipherResult<byte[]> {
    /** Name of used for encryption cipher storage. */
    public final String cipherName;

    /** Main constructor. */
    public EncryptionResult(final byte[] username, final byte[] password, final String cipherName) {
      super(username, password);
      this.cipherName = cipherName;
    }

    /** Helper constructor. Simplifies cipher name extraction. */
    public EncryptionResult(final byte[] username, final byte[] password, @NonNull final CipherStorage cipherStorage) {
      this(username, password, cipherStorage.getCipherStorageName());
    }
  }

  /** Credentials in string's, often a result of decryption. */
  class DecryptionResult extends CipherResult<String> {
    private final SecurityLevel securityLevel;

    public DecryptionResult(final String username, final String password) {
      this(username, password, SecurityLevel.ANY);
    }

    public DecryptionResult(final String username, final String password, final SecurityLevel level) {
      super(username, password);
      securityLevel = level;
    }

    public SecurityLevel getSecurityLevel() {
      return securityLevel;
    }
  }

  /** Ask access permission for decrypting credentials in provided context. */
  class DecryptionContext extends CipherResult<byte[]> {
    public final Key key;
    public final String keyAlias;

    public DecryptionContext(@NonNull final String keyAlias,
                             @NonNull final Key key,
                             @NonNull final byte[] password,
                             @NonNull final byte[] username) {
      super(username, password);
      this.keyAlias = keyAlias;
      this.key = key;
    }
  }

  /** Get access to the results of decryption via properties. */
  interface WithResults {
    /** Get reference on results. */
    @Nullable
    DecryptionResult getResult();

    /** Get reference on capture error. */
    @Nullable
    Throwable getError();

    /** Block thread and wait for any result of execution. */
    void waitResult();
  }

  /** Handler that allows to inject some actions during decrypt operations. */
  interface DecryptionResultHandler extends WithResults {
    /** Ask user for interaction, often its unlock of keystore by biometric data providing. */
    void askAccessPermissions(@NonNull final DecryptionContext context);

    /**
     *
     */
    void onDecrypt(@Nullable final DecryptionResult decryptionResult, @Nullable final Throwable error);
  }
  //endregion

  //region API

  /** Encrypt credentials with provided key (by alias) and required security level. */
  @NonNull
  EncryptionResult encrypt(@NonNull final String alias,
                           @NonNull final String username,
                           @NonNull final String password,
                           @NonNull final SecurityLevel level)
    throws CryptoFailedException;

  /**
   * Decrypt credentials with provided key (by alias) and required security level.
   * In case of key stored in weaker security level than required will be raised exception.
   * That can happens during migration from one version of library to another.
   */
  @NonNull
  DecryptionResult decrypt(@NonNull final String alias,
                           @NonNull final byte[] username,
                           @NonNull final byte[] password,
                           @NonNull final SecurityLevel level)
    throws CryptoFailedException;

  /** Decrypt the credentials but redirect results of operation to handler. */
  void decrypt(@NonNull final DecryptionResultHandler handler,
               @NonNull final String alias,
               @NonNull final byte[] username,
               @NonNull final byte[] password,
               @NonNull final SecurityLevel level)
    throws CryptoFailedException;

  /** Remove key (by alias) from storage. */
  void removeKey(@NonNull final String alias) throws KeyStoreAccessException;
  //endregion

  //region Configuration

  /** Storage name. */
  String getCipherStorageName();

  /** Minimal API level needed for using the storage. */
  int getMinSupportedApiLevel();

  /** Provided security level. */
  SecurityLevel securityLevel();

  /** True - based on secured hardware capabilities, otherwise False. */
  boolean supportsSecureHardware();

  /** True - based on biometric capabilities, otherwise false. */
  boolean isBiometrySupported();

  /**
   * The higher value means better capabilities.
   * Formula:
   * = 1000 * isBiometrySupported() +
   * 100 * isSecureHardware() +
   * minSupportedApiLevel()
   */
  int getCapabilityLevel();

  /** Get default name for alias/service. */
  String getDefaultAliasServiceName();
  //endregion
}
