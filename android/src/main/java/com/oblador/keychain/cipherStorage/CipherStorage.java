package com.oblador.keychain.cipherStorage;

import androidx.annotation.NonNull;

import com.oblador.keychain.SecurityLevel;
import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

public interface CipherStorage {
    abstract class CipherResult<T> {
        public final T username;
        public final T password;

        public CipherResult(T username, T password) {
            this.username = username;
            this.password = password;
        }
    }

    class EncryptionResult extends CipherResult<byte[]> {
        public CipherStorage cipherStorage;

        public EncryptionResult(byte[] username, byte[] password, CipherStorage cipherStorage) {
            super(username, password);
            this.cipherStorage = cipherStorage;
        }
    }

    class DecryptionResult extends CipherResult<String> {
      private SecurityLevel securityLevel;

      public DecryptionResult(String username, String password, SecurityLevel level) {
            super(username, password);
            securityLevel = level;
        }

      public SecurityLevel getSecurityLevel() {
        return securityLevel;
      }
    }

    EncryptionResult encrypt(@NonNull String service, @NonNull String username, @NonNull String password, SecurityLevel level, boolean useStrongBox) throws CryptoFailedException;

    DecryptionResult decrypt(@NonNull String service, @NonNull byte[] username, @NonNull byte[] password) throws CryptoFailedException;

    void removeKey(@NonNull String service) throws KeyStoreAccessException;

    String getCipherStorageName();

    int getMinSupportedApiLevel();

    SecurityLevel securityLevel();

    boolean supportsSecureHardware();
}
