package com.oblador.keychain;

import android.content.SharedPreferences;
import android.util.Base64;

public class PrefsUtils {
    static public class ResultSet {
        final String service;
        final byte[] usernameBytes;
        final byte[] passwordBytes;

        public ResultSet(String service, byte[] usernameBytes, byte[] passwordBytes) {
            this.service = service;
            this.usernameBytes = usernameBytes;
            this.passwordBytes = passwordBytes;
        }
    }

    private final SharedPreferences prefs;

    public PrefsUtils(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public ResultSet getBytesForUsernameAndPassword(String service, String delimiter) {
        String keyForUsername = getKeyForUsername(service, delimiter);
        String keyForPassword = getKeyForPassword(service, delimiter);
        byte[] bytesForUsername = getBytes(keyForUsername);
        byte[] bytesForPassword = getBytes(keyForPassword);
        if (bytesForUsername != null && bytesForPassword != null) {
            return new ResultSet(service, bytesForUsername, bytesForPassword);
        }
        return null;
    }

    public void resetPassword(String service, String delimiter) {
        SharedPreferences.Editor prefsEditor = prefs.edit();
        String keyForUsername = getKeyForUsername(service, delimiter);
        String keyForPassword = getKeyForPassword(service, delimiter);

        if (prefs.contains(keyForUsername) || prefs.contains(keyForPassword)) {
            prefsEditor.remove(keyForUsername);
            prefsEditor.remove(keyForPassword);
            prefsEditor.apply();
        }
    }

    public void storeEncryptedValues(String service, String delimiter, String encryptedUsername, String encryptedPassword) {
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString(getKeyForUsername(service, delimiter), encryptedUsername);
        prefsEditor.putString(getKeyForPassword(service, delimiter), encryptedPassword);
        prefsEditor.apply();
    }

    private byte[] getBytesForUsername(String service, String delimiter) {
        String key = getKeyForUsername(service, delimiter);
        return getBytes(key);
    }

    private byte[] getBytesForPassword(String service, String delimiter) {
        String key = getKeyForPassword(service, delimiter);
        return getBytes(key);
    }

    private String getKeyForUsername(String service, String delimiter) {
        return service + delimiter + "u";
    }

    private String getKeyForPassword(String service, String delimiter) {
        return service + delimiter + "p";
    }

    private byte[] getBytes(String key) {
        String value = this.prefs.getString(key, null);
        if (value != null) {
            return Base64.decode(value, Base64.DEFAULT);
        }
        return null;
    }
}
