package com.oblador.keychain.cipherStorage;

import android.support.annotation.NonNull;

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
        public DecryptionResult(String username, String password) {
            super(username, password);
        }
    }

    interface EncryptionResultHandler {
        public void onEncryptionResult(EncryptionResult encryptionResult, String info, String error);
    }

    interface DecryptionResultHandler {
        public void onDecryptionResult(DecryptionResult decryptionResult, String info, String error);
    }

    void encrypt(@NonNull EncryptionResultHandler encryptionResultHandler, @NonNull String service, @NonNull String username, @NonNull String password, @NonNull boolean useFingerprint) throws CryptoFailedException;

    void decrypt(@NonNull DecryptionResultHandler decryptionResultHandler, @NonNull String service, @NonNull byte[] username, @NonNull byte[] password, @NonNull boolean useFingerprint) throws CryptoFailedException;

    void removeKey(@NonNull String service) throws KeyStoreAccessException;

    String getCipherStorageName();

    int getMinSupportedApiLevel();
}
