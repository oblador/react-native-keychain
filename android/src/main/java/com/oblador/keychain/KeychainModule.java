package com.oblador.keychain;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import com.facebook.android.crypto.keychain.AndroidConceal;
import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain;
import com.facebook.crypto.Crypto;
import com.facebook.crypto.CryptoConfig;
import com.facebook.crypto.Entity;
import com.facebook.crypto.keychain.KeyChain;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.EmptyParameterException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;

public class KeychainModule extends ReactContextBaseJavaModule {

    public static final String E_EMPTY_PARAMETERS = "E_EMPTY_PARAMETERS";
    public static final String E_CRYPTO_FAILED = "E_CRYPTO_FAILED";
    public static final String E_UNSUPPORTED_KEYSTORE = "E_UNSUPPORTED_KEYSTORE";
    public static final String E_KEYSTORE_ACCESS_ERROR = "E_KEYSTORE_ACCESS_ERROR";

    public static final String KEYCHAIN_MODULE = "RNKeychainManager";
    public static final String KEYCHAIN_DATA = "RN_KEYCHAIN";
    public static final String EMPTY_STRING = "";
    public static final String DEFAULT_ALIAS = "RN_KEYCHAIN_DEFAULT_ALIAS";
    public static final int YEARS_TO_LAST = 15;
    public static final String LEGACY_DELIMITER = ":";
    public static final String DELIMITER = "_";
    public static final String KEYSTORE_TYPE = "AndroidKeyStore";
    public static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";

    private final Crypto crypto;
    private final SharedPreferences prefs;

    private class ResultSet {
        final String service;
        final byte[] decryptedUsername;
        final byte[] decryptedPassword;

        public ResultSet(String service, byte[] decryptedUsername, byte[] decryptedPassword) {
            this.service = service;
            this.decryptedUsername = decryptedUsername;
            this.decryptedPassword = decryptedPassword;
        }
    }

    @Override
    public String getName() {
        return KEYCHAIN_MODULE;
    }

    public KeychainModule(ReactApplicationContext reactContext) {
        super(reactContext);

        KeyChain keyChain = new SharedPrefsBackedKeyChain(getReactApplicationContext(), CryptoConfig.KEY_256);
        crypto = AndroidConceal.get().createDefaultCrypto(keyChain);
        prefs = this.getReactApplicationContext().getSharedPreferences(KEYCHAIN_DATA, Context.MODE_PRIVATE);
    }

    @ReactMethod
    public void setGenericPasswordForOptions(String service, String username, String password, Promise promise) {
        try {
            setGenericPasswordForOptions(service, username, password);

            // Clean legacy values (if any)
            resetGenericPasswordForOptionsLegacy(service);
            promise.resolve("KeychainModule saved the data");
        } catch (EmptyParameterException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_EMPTY_PARAMETERS, e);
        } catch (CryptoFailedException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_CRYPTO_FAILED, e);
        } catch (KeyStoreException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_UNSUPPORTED_KEYSTORE, e);
        } catch (KeyStoreAccessException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_KEYSTORE_ACCESS_ERROR, e);
        }
    }

    private void setGenericPasswordForOptions(String service, String username, String password) throws EmptyParameterException, CryptoFailedException, KeyStoreException, KeyStoreAccessException {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new EmptyParameterException("you passed empty or null username/password");
        }
        service = service == null ? DEFAULT_ALIAS : service;

        KeyStore keyStore = getKeyStoreAndLoad();

        try {
            if (!keyStore.containsAlias(service)) {
                AlgorithmParameterSpec spec;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    spec = new KeyGenParameterSpec.Builder(
                            service,
                            KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT)
                            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                            .setRandomizedEncryptionRequired(true)
                            //.setUserAuthenticationRequired(true) // Will throw InvalidAlgorithmParameterException if there is no fingerprint enrolled on the device
                            .setKeySize(2048)
                            .build();
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Calendar cal = Calendar.getInstance();
                    Date start = cal.getTime();
                    cal.add(Calendar.YEAR, YEARS_TO_LAST);
                    Date end = cal.getTime();
                    spec = new KeyPairGeneratorSpec.Builder(this.getReactApplicationContext())
                            .setAlias(service)
                            .setSubject(new X500Principal("CN=domain.com, O=security"))
                            .setSerialNumber(BigInteger.ONE)
                            .setStartDate(start)
                            .setEndDate(end)
                            .setKeySize(2048)
                            .build();
                } else {
                    throw new CryptoFailedException("Unsupported Android SDK " + Build.VERSION.SDK_INT);
                }

                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", KEYSTORE_TYPE);
                generator.initialize(spec);

                generator.generateKeyPair();
            }

            PublicKey publicKey = keyStore.getCertificate(service).getPublicKey();

            String encryptedUsername = encryptString(publicKey, service, username);
            String encryptedPassword = encryptString(publicKey, service, password);

            SharedPreferences.Editor prefsEditor = prefs.edit();
            prefsEditor.putString(service + DELIMITER + "u", encryptedUsername);
            prefsEditor.putString(service + DELIMITER + "p", encryptedPassword);
            prefsEditor.apply();
            Log.d(KEYCHAIN_MODULE, "saved the data");
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException e) {
            throw new CryptoFailedException("Could not encrypt data for service " + service, e);
        }
    }

    private String encryptString(PublicKey publicKey, String service, String value) throws CryptoFailedException {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
            cipherOutputStream.write(value.getBytes("UTF-8"));
            cipherOutputStream.close();
            byte[] encryptedBytes = outputStream.toByteArray();
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            throw new CryptoFailedException("Could not encrypt value for service " + service, e);
        }
    }

    @ReactMethod
    public void getGenericPasswordForOptions(String service, Promise promise) {
        String originalService = service;
        service = service == null ? DEFAULT_ALIAS : service;

        try {
            byte[] recuser = getBytesFromPrefs(service, DELIMITER + "u");
            byte[] recpass = getBytesFromPrefs(service, DELIMITER + "p");
            if (recuser == null || recpass == null) {
                // Check if the values are stored using the LEGACY_DELIMITER and thus encrypted using FaceBook's Conceal
                ResultSet resultSet = getGenericPasswordForOptionsUsingConceal(originalService);
                if (resultSet != null) {
                    // Store the values using the new delimiter and the KeyStore
                    setGenericPasswordForOptions(
                            originalService,
                            new String(resultSet.decryptedUsername, Charset.forName("UTF-8")),
                            new String(resultSet.decryptedUsername, Charset.forName("UTF-8")));
                    // Remove the legacy value(s)
                    resetGenericPasswordForOptionsLegacy(originalService);
                    recuser = resultSet.decryptedUsername;
                    recpass = resultSet.decryptedPassword;
                } else {
                    Log.e(KEYCHAIN_MODULE, "no keychain entry found for service: " + service);
                    promise.resolve(false);
                    return;
                }
            }

            KeyStore keyStore = getKeyStoreAndLoad();

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(service, null);

            byte[] decryptedUsername = decryptBytes(privateKey, recuser);
            byte[] decryptedPassword = decryptBytes(privateKey, recpass);

            WritableMap credentials = Arguments.createMap();

            credentials.putString("service", service);
            credentials.putString("username", new String(decryptedUsername, Charset.forName("UTF-8")));
            credentials.putString("password", new String(decryptedPassword, Charset.forName("UTF-8")));

            promise.resolve(credentials);
        } catch (KeyStoreException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_UNSUPPORTED_KEYSTORE, e);
        } catch (KeyStoreAccessException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_KEYSTORE_ACCESS_ERROR, e);
        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | CryptoFailedException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_CRYPTO_FAILED, e);
        } catch (EmptyParameterException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_EMPTY_PARAMETERS, e);
        }
    }

    private byte[] decryptBytes(PrivateKey privateKey, byte[] bytes) throws CryptoFailedException {
        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            CipherInputStream cipherInputStream = new CipherInputStream(
                    new ByteArrayInputStream(bytes), cipher);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while (true) {
                int n = cipherInputStream.read(buffer, 0, buffer.length);
                if (n <= 0) {
                    break;
                }
                output.write(buffer, 0, n);
            }
            return output.toByteArray();
        } catch (Exception e) {
            throw new CryptoFailedException("Could not decrypt bytes", e);
        }
    }

    private ResultSet getGenericPasswordForOptionsUsingConceal(String service) throws CryptoFailedException {
        if (!crypto.isAvailable()) {
            throw new CryptoFailedException("Crypto is missing");
        }
        service = service == null ? EMPTY_STRING : service;

        byte[] recuser = getBytesFromPrefs(service, LEGACY_DELIMITER + "u");
        byte[] recpass = getBytesFromPrefs(service, LEGACY_DELIMITER + "p");
        if (recuser == null || recpass == null) {
            return null;
        }

        Entity userentity = Entity.create(KEYCHAIN_DATA + ":" + service + "user");
        Entity pwentity = Entity.create(KEYCHAIN_DATA + ":" + service + "pass");

        try {
            byte[] decryptedUsername = crypto.decrypt(recuser, userentity);
            byte[] decryptedPassword = crypto.decrypt(recpass, pwentity);

            return new ResultSet(service, decryptedUsername, decryptedPassword);
        } catch (Exception e) {
            throw new CryptoFailedException("Decryption failed for service " + service, e);
        }
    }

    private byte[] getBytesFromPrefs(String service, String prefix) {
        String key = service + prefix;
        String value = prefs.getString(service + prefix, null);
        if (value != null) {
            return Base64.decode(value, Base64.DEFAULT);
        }
        return null;
    }

    @ReactMethod
    public void resetGenericPasswordForOptions(String service, Promise promise) {
        try {
            resetGenericPasswordForOptions(service);
            promise.resolve(true);
        } catch (KeyStoreException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_UNSUPPORTED_KEYSTORE, e);
        } catch (KeyStoreAccessException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_KEYSTORE_ACCESS_ERROR, e);
        }
    }

    private void resetGenericPasswordForOptions(String service) throws KeyStoreException, KeyStoreAccessException {
        service = service == null ? DEFAULT_ALIAS : service;

        KeyStore keyStore = getKeyStoreAndLoad();

        if (keyStore.containsAlias(service)) {
            keyStore.deleteEntry(service);
        }

        SharedPreferences.Editor prefsEditor = prefs.edit();

        if (prefs.contains(service + DELIMITER + "u")) {
            prefsEditor.remove(service + DELIMITER + "u");
            prefsEditor.remove(service + DELIMITER + "p");
            prefsEditor.apply();
        }
    }

    private void resetGenericPasswordForOptionsLegacy(String service) throws KeyStoreException, KeyStoreAccessException {
        service = service == null ? EMPTY_STRING : service;

        SharedPreferences.Editor prefsEditor = prefs.edit();

        if (prefs.contains(service + LEGACY_DELIMITER + "u")) {
            prefsEditor.remove(service + LEGACY_DELIMITER + "u");
            prefsEditor.remove(service + LEGACY_DELIMITER + "p");
            prefsEditor.apply();
        }
    }

    @ReactMethod
    public void setInternetCredentialsForServer(@NonNull String server, String username, String password, ReadableMap unusedOptions, Promise promise) {
        setGenericPasswordForOptions(server, username, password, promise);
    }

    @ReactMethod
    public void getInternetCredentialsForServer(@NonNull String server, ReadableMap unusedOptions, Promise promise) {
        getGenericPasswordForOptions(server, promise);
    }

    @ReactMethod
    public void resetInternetCredentialsForServer(@NonNull String server, ReadableMap unusedOptions, Promise promise) {
        resetGenericPasswordForOptions(server, promise);
    }

    private KeyStore getKeyStore() throws KeyStoreException {
        return KeyStore.getInstance(KEYSTORE_TYPE);
    }

    private KeyStore getKeyStoreAndLoad() throws KeyStoreException, KeyStoreAccessException {
        try {
            KeyStore keyStore = getKeyStore();
            keyStore.load(null);
            return keyStore;
        } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
            throw new KeyStoreAccessException("Could not access KeyStore", e);
        }
    }
}