package com.oblador.keychain.cipherStorage;

import android.os.Build;
import android.support.annotation.NonNull;

import com.facebook.android.crypto.keychain.AndroidConceal;
import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain;
import com.facebook.crypto.Crypto;
import com.facebook.crypto.CryptoConfig;
import com.facebook.crypto.Entity;
import com.facebook.crypto.keychain.KeyChain;
import com.facebook.react.bridge.ReactApplicationContext;
import com.oblador.keychain.exceptions.CryptoFailedException;

import java.nio.charset.Charset;

public class CipherStorageFacebookConceal implements CipherStorage {
    public static final String CIPHER_STORAGE_NAME = "FacebookConceal";
    public static final String KEYCHAIN_DATA = "RN_KEYCHAIN";
    private final Crypto crypto;

    public CipherStorageFacebookConceal(ReactApplicationContext reactContext) {
        KeyChain keyChain = new SharedPrefsBackedKeyChain(reactContext, CryptoConfig.KEY_256);
        this.crypto = AndroidConceal.get().createDefaultCrypto(keyChain);
    }

    @Override
    public String getCipherStorageName() {
        return CIPHER_STORAGE_NAME;
    }

    @Override
    public int getMinSupportedApiLevel() {
        return Build.VERSION_CODES.JELLY_BEAN;
    }

    @Override
    public EncryptionResult encrypt(@NonNull String service, @NonNull String username, @NonNull String password) throws CryptoFailedException {
        if (!crypto.isAvailable()) {
            throw new CryptoFailedException("Crypto is missing");
        }
        Entity usernameEntity = createUsernameEntity(service);
        Entity passwordEntity = createPasswordEntity(service);

        try {
            byte[] encryptedUsername = crypto.encrypt(username.getBytes(Charset.forName("UTF-8")), usernameEntity);
            byte[] encryptedPassword = crypto.encrypt(password.getBytes(Charset.forName("UTF-8")), passwordEntity);

            return new EncryptionResult(encryptedUsername, encryptedPassword, this);
        } catch (Exception e) {
            throw new CryptoFailedException("Encryption failed for service " + service, e);
        }
    }

    @Override
    public DecryptionResult decrypt(@NonNull String service, @NonNull byte[] username, @NonNull byte[] password) throws CryptoFailedException {
        if (!crypto.isAvailable()) {
            throw new CryptoFailedException("Crypto is missing");
        }
        Entity usernameEntity = createUsernameEntity(service);
        Entity passwordEntity = createPasswordEntity(service);

        try {
            byte[] decryptedUsername = crypto.decrypt(username, usernameEntity);
            byte[] decryptedPassword = crypto.decrypt(password, passwordEntity);

            return new DecryptionResult(
                    new String(decryptedUsername, Charset.forName("UTF-8")),
                    new String(decryptedPassword, Charset.forName("UTF-8")));
        } catch (Exception e) {
            throw new CryptoFailedException("Decryption failed for service " + service, e);
        }
    }

    @Override
    public void removeKey(@NonNull String service) {
        // Facebook Conceal stores only one key across all services, so we cannot delete the key (otherwise decryption will fail for encrypted data of other services).
    }

    private Entity createUsernameEntity(String service) {
        String prefix = getEntityPrefix(service);
        return Entity.create(prefix + "user");
    }

    private Entity createPasswordEntity(String service) {
        String prefix = getEntityPrefix(service);
        return Entity.create(prefix + "pass");
    }

    private String getEntityPrefix(String service) {
        return KEYCHAIN_DATA + ":" + service;
    }
}
