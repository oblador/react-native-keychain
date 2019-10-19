package com.oblador.keychain.cipherStorage;

import android.security.keystore.KeyPermanentlyInvalidatedException;

import com.oblador.keychain.SecurityLevel;
import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

import androidx.annotation.NonNull;

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

    interface DecryptionResultHandler {
        void onDecrypt(DecryptionResult decryptionResult, String error);
    }

    EncryptionResult encrypt(@NonNull String service, @NonNull String username, @NonNull String password, SecurityLevel level) throws CryptoFailedException;

    void decrypt(@NonNull DecryptionResultHandler decryptionResultHandler, @NonNull String service, @NonNull byte[] username, @NonNull byte[] password) throws CryptoFailedException, KeyPermanentlyInvalidatedException;

    void removeKey(@NonNull String service) throws KeyStoreAccessException;

    String getCipherStorageName();

    boolean getCipherBiometrySupported();

    int getMinSupportedApiLevel();

    SecurityLevel securityLevel();

    boolean supportsSecureHardware();
}
