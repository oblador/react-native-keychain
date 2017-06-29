package com.oblador.keychain;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Base64;

import com.facebook.react.bridge.ReactApplicationContext;
import com.oblador.keychain.cipherStorage.CipherStorage.EncryptionResult;
import com.oblador.keychain.cipherStorage.CipherStorageFacebookConceal;

public class PrefsStorage {
    public static final String KEYCHAIN_DATA = "RN_KEYCHAIN";

    static public class ResultSet {
        public final String cipherStorageName;
        public final byte[] usernameBytes;
        public final byte[] passwordBytes;

        public ResultSet(String cipherStorageName, byte[] usernameBytes, byte[] passwordBytes) {
            this.cipherStorageName = cipherStorageName;
            this.usernameBytes = usernameBytes;
            this.passwordBytes = passwordBytes;
        }
    }

    private final SharedPreferences prefs;

    public PrefsStorage(ReactApplicationContext reactContext) {
        this.prefs = reactContext.getSharedPreferences(KEYCHAIN_DATA, Context.MODE_PRIVATE);
    }

    public ResultSet getEncryptedEntry(@NonNull String service) {
        byte[] bytesForUsername = getBytesForUsername(service);
        byte[] bytesForPassword = getBytesForPassword(service);
        String cipherStorageName = getCipherStorageName(service);
        if (bytesForUsername != null && bytesForPassword != null) {
            if (cipherStorageName == null) {
                // If the CipherStorage name is not found, we assume it is because the entry was written by an older version of this library. The older version used Facebook Conceal, so we default to that.
                cipherStorageName = CipherStorageFacebookConceal.CIPHER_STORAGE_NAME;
            }
            return new ResultSet(cipherStorageName, bytesForUsername, bytesForPassword);
        }
        return null;
    }

    public void removeEntry(@NonNull String service) {
        String keyForUsername = getKeyForUsername(service);
        String keyForPassword = getKeyForPassword(service);
        String keyForCipherStorage = getKeyForCipherStorage(service);

        prefs.edit()
                .remove(keyForUsername)
                .remove(keyForPassword)
                .remove(keyForCipherStorage).apply();
    }

    public void storeEncryptedEntry(@NonNull String service, @NonNull EncryptionResult encryptionResult) {
        String keyForUsername = getKeyForUsername(service);
        String keyForPassword = getKeyForPassword(service);
        String keyForCipherStorage = getKeyForCipherStorage(service);

        prefs.edit()
                .putString(keyForUsername, Base64.encodeToString(encryptionResult.username, Base64.DEFAULT))
                .putString(keyForPassword, Base64.encodeToString(encryptionResult.password, Base64.DEFAULT))
                .putString(keyForCipherStorage, encryptionResult.cipherStorage.getCipherStorageName())
                .apply();
    }

    private byte[] getBytesForUsername(String service) {
        String key = getKeyForUsername(service);
        return getBytes(key);
    }

    private byte[] getBytesForPassword(String service) {
        String key = getKeyForPassword(service);
        return getBytes(key);
    }

    private String getCipherStorageName(String service) {
        String key = getKeyForCipherStorage(service);
        return this.prefs.getString(key, null);
    }

    private String getKeyForUsername(String service) {
        return service + ":" + "u";
    }

    private String getKeyForPassword(String service) {
        return service + ":" + "p";
    }

    private String getKeyForCipherStorage(String service) {
        return service + ":" + "c";
    }

    private byte[] getBytes(String key) {
        String value = this.prefs.getString(key, null);
        if (value != null) {
            return Base64.decode(value, Base64.DEFAULT);
        }
        return null;
    }
}
