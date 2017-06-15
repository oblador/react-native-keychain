package com.oblador.keychain;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
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
import com.facebook.crypto.exception.CryptoInitializationException;
import com.facebook.crypto.exception.KeyChainException;
import com.facebook.crypto.keychain.KeyChain;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.oblador.keychain.PrefsUtils.ResultSet;
import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.EmptyParameterException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;

public class KeychainModule extends ReactContextBaseJavaModule {

    public static final String E_EMPTY_PARAMETERS = "E_EMPTY_PARAMETERS";
    public static final String E_CRYPTO_FAILED = "E_CRYPTO_FAILED";
    public static final String E_UNSUPPORTED_KEYSTORE = "E_UNSUPPORTED_KEYSTORE";
    public static final String E_KEYSTORE_ACCESS_ERROR = "E_KEYSTORE_ACCESS_ERROR";

    public static final String KEYCHAIN_MODULE = "RNKeychainManager";
    public static final String KEYCHAIN_DATA = "RN_KEYCHAIN";
    public static final String EMPTY_STRING = "";
    public static final String DEFAULT_ALIAS = "RN_KEYCHAIN_DEFAULT_ALIAS";
    public static final String LEGACY_DELIMITER = ":";
    public static final String DELIMITER = "_";
    public static final String KEYSTORE_TYPE = "AndroidKeyStore";
    public static final String ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES;
    public static final String ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC;
    public static final String ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7;
    public static final String ENCRYPTION_TRANSFORMATION =
            ENCRYPTION_ALGORITHM + "/" +
            ENCRYPTION_BLOCK_MODE + "/" +
            ENCRYPTION_PADDING;
    public static final int ENCRYPTION_KEY_SIZE = 256;

    private final Crypto crypto;
    private final SharedPreferences prefs;
    private final PrefsUtils prefsUtils;

    @Override
    public String getName() {
        return KEYCHAIN_MODULE;
    }

    public KeychainModule(ReactApplicationContext reactContext) {
        super(reactContext);

        KeyChain keyChain = new SharedPrefsBackedKeyChain(getReactApplicationContext(), CryptoConfig.KEY_256);
        crypto = AndroidConceal.get().createDefaultCrypto(keyChain);
        prefs = this.getReactApplicationContext().getSharedPreferences(KEYCHAIN_DATA, Context.MODE_PRIVATE);
        prefsUtils = new PrefsUtils(this.prefs);
    }

    @ReactMethod
    public void setGenericPasswordForOptions(String service, String username, String password, Promise promise) {
        try {
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                throw new EmptyParameterException("you passed empty or null username/password");
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setGenericPasswordForOptions(service, username, password);

                // Clean legacy values (if any)
                resetGenericPasswordForOptionsLegacy(service);
            }
            else {
                setGenericPasswordForOptionsUsingConceal(service, username, password);
            }
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

    @TargetApi(Build.VERSION_CODES.M)
    private void setGenericPasswordForOptions(String service, String username, String password) throws CryptoFailedException, KeyStoreException, KeyStoreAccessException {
        service = service == null ? DEFAULT_ALIAS : service;

        KeyStore keyStore = getKeyStoreAndLoad();

        try {
            if (!keyStore.containsAlias(service)) {
                AlgorithmParameterSpec spec;
                    spec = new KeyGenParameterSpec.Builder(
                            service,
                            KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT)
                            .setBlockModes(ENCRYPTION_BLOCK_MODE)
                            .setEncryptionPaddings(ENCRYPTION_PADDING)
                            .setRandomizedEncryptionRequired(true)
                            //.setUserAuthenticationRequired(true) // Will throw InvalidAlgorithmParameterException if there is no fingerprint enrolled on the device
                            .setKeySize(ENCRYPTION_KEY_SIZE)
                            .build();

                KeyGenerator generator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, KEYSTORE_TYPE);
                generator.init(spec);

                generator.generateKey();
            }

            Key key = keyStore.getKey(service, null);

            String encryptedUsername = encryptString(key, service, username);
            String encryptedPassword = encryptString(key, service, password);

            prefsUtils.storeEncryptedValues(service, DELIMITER, encryptedUsername, encryptedPassword);
            Log.d(KEYCHAIN_MODULE, "saved the data");
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException | UnrecoverableKeyException e) {
            throw new CryptoFailedException("Could not encrypt data for service " + service, e);
        }
    }

    private String encryptString(Key key, String service, String value) throws CryptoFailedException {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // write initialization vector to the beginning of the stream
            byte[] iv = cipher.getIV();
            outputStream.write(iv, 0, iv.length);
            // encrypt the value using a CipherOutputStream
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
            cipherOutputStream.write(value.getBytes("UTF-8"));
            cipherOutputStream.close();
            // return a Base64 encoded String of the stream
            byte[] encryptedBytes = outputStream.toByteArray();
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            throw new CryptoFailedException("Could not encrypt value for service " + service, e);
        }
    }

    @ReactMethod
    public void getGenericPasswordForOptions(String service, Promise promise) {
        try {
            final ResultSet resultSet;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                resultSet = getGenericPasswordForOptions(service);
            }
            else {
                resultSet = getGenericPasswordForOptionsUsingConceal(service);
            }
            if (resultSet == null) {
                Log.e(KEYCHAIN_MODULE, "no keychain entry found for service: " + service);
                promise.resolve(false);
                return;
            }
            WritableMap credentials = Arguments.createMap();

            credentials.putString("service", resultSet.service);
            credentials.putString("username", new String(resultSet.usernameBytes, Charset.forName("UTF-8")));
            credentials.putString("password", new String(resultSet.passwordBytes, Charset.forName("UTF-8")));

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
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private ResultSet getGenericPasswordForOptions(String service) throws CryptoFailedException, KeyStoreException, KeyStoreAccessException, UnrecoverableKeyException, NoSuchAlgorithmException {
        String originalService = service;
        service = service == null ? DEFAULT_ALIAS : service;

        final byte[] decryptedUsername;
        final byte[] decryptedPassword;
        ResultSet encryptedResultSet = prefsUtils.getBytesForUsernameAndPassword(service, DELIMITER);
        if (encryptedResultSet == null) {
            // Check if the values are stored using the LEGACY_DELIMITER and thus encrypted using FaceBook's Conceal
            ResultSet legacyResultSet = getGenericPasswordForOptionsUsingConceal(originalService);
            if (legacyResultSet != null) {
                // Store the values using the new delimiter and the KeyStore
                setGenericPasswordForOptions(
                        originalService,
                        new String(legacyResultSet.usernameBytes, Charset.forName("UTF-8")),
                        new String(legacyResultSet.passwordBytes, Charset.forName("UTF-8")));
                // Remove the legacy value(s)
                resetGenericPasswordForOptionsLegacy(originalService);
                decryptedUsername = legacyResultSet.usernameBytes;
                decryptedPassword = legacyResultSet.passwordBytes;
            } else {
                return null;
            }
        }
        else {
            KeyStore keyStore = getKeyStoreAndLoad();

            Key key = keyStore.getKey(service, null);

            decryptedUsername = decryptBytes(key, encryptedResultSet.usernameBytes);
            decryptedPassword = decryptBytes(key, encryptedResultSet.passwordBytes);
        }
        return new ResultSet(service, decryptedUsername, decryptedPassword);
    }

    private byte[] decryptBytes(Key key, byte[] bytes) throws CryptoFailedException {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            // read the initialization vector from the beginning of the stream
            IvParameterSpec ivParams = readIvFromStream(inputStream);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParams);
            // decrypt the bytes using a CipherInputStream
            CipherInputStream cipherInputStream = new CipherInputStream(
                    inputStream, cipher);
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

    private IvParameterSpec readIvFromStream(ByteArrayInputStream inputStream) {
        byte[] iv = new byte[16];
        inputStream.read(iv, 0, iv.length);
        return new IvParameterSpec(iv);
    }

    private ResultSet getGenericPasswordForOptionsUsingConceal(String service) throws CryptoFailedException {
        if (!crypto.isAvailable()) {
            throw new CryptoFailedException("Crypto is missing");
        }
        service = service == null ? EMPTY_STRING : service;

        ResultSet legacyResultSet = prefsUtils.getBytesForUsernameAndPassword(service, LEGACY_DELIMITER);
        if (legacyResultSet == null) {
            return null;
        }

        Entity userentity = Entity.create(KEYCHAIN_DATA + ":" + service + "user");
        Entity pwentity = Entity.create(KEYCHAIN_DATA + ":" + service + "pass");

        try {
            byte[] decryptedUsername = crypto.decrypt(legacyResultSet.usernameBytes, userentity);
            byte[] decryptedPassword = crypto.decrypt(legacyResultSet.passwordBytes, pwentity);

            return new ResultSet(service, decryptedUsername, decryptedPassword);
        } catch (Exception e) {
            throw new CryptoFailedException("Decryption failed for service " + service, e);
        }
    }

    private void setGenericPasswordForOptionsUsingConceal(String service, String username, String password) throws CryptoFailedException {
        if (!crypto.isAvailable()) {
            throw new CryptoFailedException("Crypto is missing");
        }
        service = service == null ? EMPTY_STRING : service;

        Entity userentity = Entity.create(KEYCHAIN_DATA + ":" + service + "user");
        Entity pwentity = Entity.create(KEYCHAIN_DATA + ":" + service + "pass");

        try {
            String encryptedUsername = encryptWithEntity(username, userentity);
            String encryptedPassword = encryptWithEntity(password, pwentity);

            prefsUtils.storeEncryptedValues(service, LEGACY_DELIMITER, encryptedUsername, encryptedPassword);
        } catch (Exception e) {
            throw new CryptoFailedException("Encryption failed for service " + service, e);
        }
    }

    private String encryptWithEntity(String toEncypt, Entity entity) throws KeyChainException, CryptoInitializationException, IOException {
        byte[] encryptedBytes = crypto.encrypt(toEncypt.getBytes(Charset.forName("UTF-8")), entity);
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
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

        prefsUtils.resetPassword(service, DELIMITER);
    }

    private void resetGenericPasswordForOptionsLegacy(String service) throws KeyStoreException, KeyStoreAccessException {
        service = service == null ? EMPTY_STRING : service;

        prefsUtils.resetPassword(service, LEGACY_DELIMITER);
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