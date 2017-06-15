package com.oblador.keychain;

import android.content.SharedPreferences;
import android.util.Base64;

/**
 * Created by pcoltau on 6/15/17.
 */

public class PrefsUtils {
    private final SharedPreferences prefs;

    public PrefsUtils(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public byte[] getBytesForUsername(String service, String delimiter) {
        String key = getKeyForUsername(service, delimiter);
        return getBytes(key);
    }

    public byte[] getBytesForPassword(String service, String delimiter) {
        String key = getKeyForPassword(service, delimiter);
        return getBytes(key);
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
