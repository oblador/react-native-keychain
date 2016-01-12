package com.oblador.keychain;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;
import com.facebook.android.crypto.keychain.AndroidConceal;
import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain;
import com.facebook.crypto.Crypto;
import com.facebook.crypto.CryptoConfig;
import com.facebook.crypto.Entity;
import com.facebook.crypto.keychain.KeyChain;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.nio.charset.StandardCharsets;


public class KeychainModule extends ReactContextBaseJavaModule {

    public static final String REACT_CLASS = "RNKeychainManager";
    public static final String KEYCHAIN_DATA = "RN_KEYCHAIN";

    private final Crypto crypto;
    private final SharedPreferences prefs;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    public KeychainModule(ReactApplicationContext reactContext) {
        super(reactContext);

        KeyChain keyChain = new SharedPrefsBackedKeyChain(getReactApplicationContext(), CryptoConfig.KEY_256);
        crypto = AndroidConceal.get().createDefaultCrypto(keyChain);

        prefs = this.getReactApplicationContext().getSharedPreferences(KEYCHAIN_DATA, Context.MODE_PRIVATE);
    }

    @ReactMethod
    public void setGenericPasswordForService(String service, String username, String password, Callback callback) {
        if (!crypto.isAvailable()) {
            Log.e("KeychainModule", "Crypto is missing");
        }
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            Log.e("KeychainModule", "you passed empty or null username/password");
            callback.invoke("KeychainModule: you passed empty or null username/password");
            return;
        }
        service = service == null ? "" : service;
        //Log.d("Crypto", service + username + password);

        Entity userentity = Entity.create(KEYCHAIN_DATA + ":" + service + "user");
        Entity pwentity = Entity.create(KEYCHAIN_DATA + ":" + service + "pass");

        try {
            String encryptedUsername = encryptWithEntity(username, userentity, callback);
            String encryptedPassword = encryptWithEntity(password, pwentity, callback);

            SharedPreferences.Editor prefsEditor = prefs.edit();
            prefsEditor.putString(service + ":u", encryptedUsername);
            prefsEditor.putString(service + ":p", encryptedPassword);
            prefsEditor.apply();
            Log.d("KeychainModule saved: ", service + encryptedUsername + ":" + encryptedPassword);
        } catch (Exception e) {
            Log.e("KeychainModule ", e.getLocalizedMessage());
            callback.invoke(e.getLocalizedMessage());
        }
    }

    private String encryptWithEntity(String toEncypt, Entity entity, Callback callback) {
        try {
            byte[] encryptedBytes = crypto.encrypt(toEncypt.getBytes(StandardCharsets.UTF_8), entity);
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e("KeychainModule ", e.getLocalizedMessage());
            callback.invoke(e.getLocalizedMessage());
            return null;
        }
    }

    @ReactMethod
    public void getGenericPasswordForService(String service, Callback callback) {
        service = service == null ? "" : service;

        String username = prefs.getString(service + ":u", "user_not_found");
        String password = prefs.getString(service + ":p", "pass_not_found");
        if (username.equals("user_not_found") || password.equals("pass_not_found")) {
            Log.e("KeychainModule ", "no keychain entry found for service: " + service);
            callback.invoke("no keychain entry found for service: " + service);
            return;
        }

        Log.d("KeychainModule ", "will attempt to decrypt for " + service + username + ":" + password);

        byte[] recuser = Base64.decode(username, Base64.DEFAULT);
        byte[] recpass = Base64.decode(password, Base64.DEFAULT);


        Entity userentity = Entity.create(KEYCHAIN_DATA + ":" + service + "user");
        Entity pwentity = Entity.create(KEYCHAIN_DATA + ":" + service + "pass");

        try {
            byte[] decryptedUsername = crypto.decrypt(recuser, userentity);
            byte[] decryptedPass = crypto.decrypt(recpass, pwentity);

            callback.invoke("", new String(decryptedUsername, StandardCharsets.UTF_8), new String(decryptedPass, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.e("KeychainModule ", e.getLocalizedMessage());
            callback.invoke(e.getLocalizedMessage());
        }
    }

    @ReactMethod
    public void resetGenericPasswordForService(String service, Callback callback) {
        service = service == null ? "" : service;

        try {
            SharedPreferences.Editor prefsEditor = prefs.edit();
            prefsEditor.remove(service + ":u");
            prefsEditor.remove(service + ":p");
            prefsEditor.apply();
        } catch (Exception e) {
            //this probably never happens but it is here so that the android api is the same as on iOS
            callback.invoke(e.getLocalizedMessage());
        }
    }

    @ReactMethod
    public void setInternetCredentialsForServer(@NonNull String server, String username, String password, Callback callback) {
        setGenericPasswordForService(server, username, password, callback);
    }

    @ReactMethod
    public void getInternetCredentialsForServer(@NonNull String server, Callback callback) {
        getGenericPasswordForService(server, callback);
    }

    @ReactMethod
    public void resetInternetCredentialsForServer(@NonNull String server, Callback callback) {
        resetGenericPasswordForService(server, callback);
    }


}
