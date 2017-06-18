package com.oblador.keychain;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.facebook.react.bridge.ReactApplicationContext;
import com.oblador.keychain.cipherStorage.CipherStorage.EncryptionResult;

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

    public ResultSet getEncryptedEntry(String service) {
        byte[] bytesForUsername = getBytesForUsername(service);
        byte[] bytesForPassword = getBytesForPassword(service);
        String cipherStorageName = getCipherStorageName(service);
        if (bytesForUsername != null && bytesForPassword != null) {
            return new ResultSet(cipherStorageName, bytesForUsername, bytesForPassword);
        }
        return null;
    }

    public void resetPassword(String service) {
        String keyForUsername = getKeyForUsername(service);
        String keyForPassword = getKeyForPassword(service);
        String keyForCipherStorage = getKeyForCipherStorage(service);

        prefs.edit().remove(keyForUsername).remove(keyForPassword).remove(keyForCipherStorage).apply();
    }

    public void storeEncryptedEntry(String service, EncryptionResult encryptionResult) {
        prefs.edit().putString(getKeyForUsername(service), Base64.encodeToString(encryptionResult.username, Base64.DEFAULT))
                .putString(getKeyForPassword(service), Base64.encodeToString(encryptionResult.password, Base64.DEFAULT))
                .putString(getKeyForCipherStorage(service), encryptionResult.cipherStorage.getCipherStorageName())
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
        return service + ":" + "i";
    }

    private byte[] getBytes(String key) {
        String value = this.prefs.getString(key, null);
        if (value != null) {
            return Base64.decode(value, Base64.DEFAULT);
        }
        return null;
    }
}
