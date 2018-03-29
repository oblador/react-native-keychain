package com.oblador.keychain;

import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.oblador.keychain.PrefsStorage.ResultSet;
import com.oblador.keychain.cipherStorage.CipherStorage;
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionResult;
import com.oblador.keychain.cipherStorage.CipherStorage.EncryptionResult;
import com.oblador.keychain.cipherStorage.CipherStorageFacebookConceal;
import com.oblador.keychain.cipherStorage.CipherStorageKeystoreAESCBC;
import com.oblador.keychain.cipherStorage.CipherStorageKeystoreAESGCM;
import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.EmptyParameterException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;
import com.oblador.keychain.DeviceAvailability;

import java.util.HashMap;
import java.util.Map;

public class KeychainModule extends ReactContextBaseJavaModule {

    public static final String E_EMPTY_PARAMETERS = "E_EMPTY_PARAMETERS";
    public static final String E_CRYPTO_FAILED = "E_CRYPTO_FAILED";
    public static final String E_KEYSTORE_ACCESS_ERROR = "E_KEYSTORE_ACCESS_ERROR";
    public static final String E_SUPPORTED_BIOMETRY_ERROR = "E_SUPPORTED_BIOMETRY_ERROR";
    public static final String KEYCHAIN_MODULE = "RNKeychainManager";
    public static final String FINGERPRINT_SUPPORTED_NAME = "Fingerprint";
    public static final String EMPTY_STRING = "";

    private final Map<String, CipherStorage> cipherStorageMap = new HashMap<>();
    private final PrefsStorage prefsStorage;

    @Override
    public String getName() {
        return KEYCHAIN_MODULE;
    }

    @Override
    public Map<String, Object> getConstants() {
      final Map<String, Object> constants = new HashMap();
      constants.put("CIPHER_OPTION_AESCBC", CipherStorageKeystoreAESCBC.CIPHER_OPTION_NAME);
      constants.put("CIPHER_OPTION_AESGCM", CipherStorageKeystoreAESGCM.CIPHER_OPTION_NAME);
      return constants;
    }

    public KeychainModule(ReactApplicationContext reactContext) {
        super(reactContext);
        prefsStorage = new PrefsStorage(reactContext);

        addCipherStorageToMap(new CipherStorageFacebookConceal(reactContext));
        addCipherStorageToMap(new CipherStorageKeystoreAESCBC());
        addCipherStorageToMap(new CipherStorageKeystoreAESGCM());
    }

    private String getPreferedCipherStorage(ReadableMap options) {
        String preferedCipher = options.hasKey("preferedCipher") ? options.getString("preferedCipher") : CipherStorageKeystoreAESGCM.CIPHER_OPTION_NAME;
        return preferedCipher;
    }

    private void addCipherStorageToMap(CipherStorage cipherStorage) {
        cipherStorageMap.put(cipherStorage.getCipherStorageName(), cipherStorage);
    }

    @ReactMethod
    public void setGenericPasswordForOptions(ReadableMap options, String username, String password, Promise promise) {
        try {
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                throw new EmptyParameterException("you passed empty or null username/password");
            }
          Log.w("CIPHER","setting generic password");
            String service = getDefaultServiceIfNull(options.getString("service"));
            String preferedCipher = getPreferedCipherStorage(options);

            CipherStorage currentCipherStorage = getCipherStorageForCurrentAPILevel(preferedCipher);
          Log.w("CIPHER","using "+currentCipherStorage.getCipherStorageName()+" to encrypt...");
            EncryptionResult result = currentCipherStorage.encrypt(service, username, password);
            prefsStorage.storeEncryptedEntry(service, result);

            promise.resolve(true);
        } catch (EmptyParameterException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_EMPTY_PARAMETERS, e);
        } catch (CryptoFailedException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_CRYPTO_FAILED, e);
        }
    }

    @ReactMethod
    public void getGenericPasswordForOptions(ReadableMap options, Promise promise) {
        try {
          Log.w("CIPHER","getting generic password");
            String service = getDefaultServiceIfNull(options.getString("service"));
            String preferedCipher = getPreferedCipherStorage(options);
            CipherStorage currentCipherStorage = getCipherStorageForCurrentAPILevel(preferedCipher);

            final DecryptionResult decryptionResult;
            ResultSet resultSet = prefsStorage.getEncryptedEntry(service);
            if (resultSet == null) {
                Log.e(KEYCHAIN_MODULE, "No entry found for service: " + service);
                promise.resolve(false);
                return;
            }

            if (resultSet.cipherStorageName.equals(currentCipherStorage.getCipherStorageName())) {
              Log.w("CIPHER","used cipher for "+service+":"+resultSet.cipherStorageName+", looks good");
                // The encrypted data is encrypted using the current CipherStorage, so we just decrypt and return
                decryptionResult = currentCipherStorage.decrypt(service, resultSet.usernameBytes, resultSet.passwordBytes);
            }
            else if (resultSet.cipherStorageName.equals(CipherStorageFacebookConceal.CIPHER_STORAGE_NAME)) {
              Log.w("CIPHER","used cipher for "+service+":"+resultSet.cipherStorageName+" is old, migrating to "+currentCipherStorage.getCipherStorageName());
                // The encrypted data is encrypted using an older CipherStorage, so we need to decrypt the data first, then encrypt it using the current CipherStorage, then store it again and return
                CipherStorage oldCipherStorage = getCipherStorageByName(resultSet.cipherStorageName);
                // decrypt using the older cipher storage
                decryptionResult = oldCipherStorage.decrypt(service, resultSet.usernameBytes, resultSet.passwordBytes);
                // encrypt using the current cipher storage
                EncryptionResult encryptionResult = currentCipherStorage.encrypt(service, decryptionResult.username, decryptionResult.password);
                // store the encryption result
                prefsStorage.storeEncryptedEntry(service, encryptionResult);
                // clean up the old cipher storage
                oldCipherStorage.removeKey(service);
            } else { // stored encryption data is different from the format we want to use, but we should not auto-migrate
              Log.w("CIPHER","used cipher for "+service+":"+resultSet.cipherStorageName+" is old, silently falling back to use it");
                CipherStorage oldCipherStorage = getCipherStorageByName(resultSet.cipherStorageName);
                decryptionResult = oldCipherStorage.decrypt(service, resultSet.usernameBytes, resultSet.passwordBytes);
            }

            WritableMap credentials = Arguments.createMap();

            credentials.putString("service", service);
            credentials.putString("username", decryptionResult.username);
            credentials.putString("password", decryptionResult.password);

            promise.resolve(credentials);
        } catch (KeyStoreAccessException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_KEYSTORE_ACCESS_ERROR, e);
        } catch (CryptoFailedException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_CRYPTO_FAILED, e);
        }
    }

    @ReactMethod
    public void resetGenericPasswordForOptions(ReadableMap options, Promise promise) {
        try {
            String service = getDefaultServiceIfNull(options.getString("service"));

            // First we clean up the cipher storage (using the cipher storage that was used to store the entry)
            ResultSet resultSet = prefsStorage.getEncryptedEntry(service);
            if (resultSet != null) {
                CipherStorage cipherStorage = getCipherStorageByName(resultSet.cipherStorageName);
                if (cipherStorage != null) {
                    cipherStorage.removeKey(service);
                }
            }
            // And then we remove the entry in the shared preferences
            prefsStorage.removeEntry(service);

            promise.resolve(true);
        } catch (KeyStoreAccessException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_KEYSTORE_ACCESS_ERROR, e);
        }
    }

    @ReactMethod
    public void setInternetCredentialsForServer(@NonNull String server, String username, String password, ReadableMap unusedOptions, Promise promise) {
        WritableMap optionsMap = Arguments.createMap();
        optionsMap.putString("service",server);
        setGenericPasswordForOptions(optionsMap, username, password, promise);
    }

    @ReactMethod
    public void getInternetCredentialsForServer(@NonNull String server, ReadableMap unusedOptions, Promise promise) {
        WritableMap optionsMap = Arguments.createMap();
        optionsMap.putString("service",server);
        getGenericPasswordForOptions(optionsMap, promise);
    }

    @ReactMethod
    public void resetInternetCredentialsForServer(@NonNull String server, ReadableMap unusedOptions, Promise promise) {
        WritableMap optionsMap = Arguments.createMap();
        optionsMap.putString("service",server);
        resetGenericPasswordForOptions(optionsMap, promise);
    }

    @ReactMethod
    public void getSupportedBiometryType(Promise promise) {
        try {
            boolean fingerprintAuthAvailable = isFingerprintAuthAvailable();
            if (fingerprintAuthAvailable) {
                promise.resolve(FINGERPRINT_SUPPORTED_NAME);
            } else {
                promise.resolve(null);
            }
        } catch (Exception e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_SUPPORTED_BIOMETRY_ERROR, e);
        }
    }

    // The "Current" CipherStorage is the cipherStorage with the highest API level that is lower than or equal to the current API level
    // or, the user requested cipher (if supported)
    private CipherStorage getCipherStorageForCurrentAPILevel(String preferedCipher) throws CryptoFailedException {
        int currentAPILevel = Build.VERSION.SDK_INT;
        CipherStorage currentCipherStorage = null;
        Map<String, CipherStorage> validMap = new HashMap();
        for (CipherStorage cipherStorage : cipherStorageMap.values()) {
            int cipherStorageAPILevel = cipherStorage.getMinSupportedApiLevel();
            // Is the cipherStorage supported on the current API level?
            boolean isSupported = (cipherStorageAPILevel <= currentAPILevel);
            if (isSupported) {
                // cipher is supported, save for later selection based on preferedCipher param
                validMap.put(cipherStorage.getCipherOptionName(), cipherStorage);

                // Is the API level better than the one we previously selected (if any)?
                // we use this as a fallback if user's preferedCipher is not available
                if (currentCipherStorage == null || cipherStorageAPILevel > currentCipherStorage.getMinSupportedApiLevel()) {
                    currentCipherStorage = cipherStorage;
                }
            }
        }

        // currentCipherStorage now contains cipher with highest API-level, check if user prefer something else
        if (preferedCipher != null && preferedCipher.length() > 0) {
          Log.w("CIPHER","user wants "+preferedCipher);
            if (validMap.containsKey(preferedCipher)) {
              Log.w("CIPHER", preferedCipher+" is supported");
                currentCipherStorage = validMap.get(preferedCipher);
            } else { // else we requested something not supported (for example we default to aesgcm which may be unsupported as well)
              Log.w("CIPHER","but cipher "+preferedCipher+" is not supported");
            }
        }
        if (currentCipherStorage == null) {
            throw new CryptoFailedException("Unsupported Android SDK " + Build.VERSION.SDK_INT);
        }
        Log.w("CIPHER","final cipher to use "+currentCipherStorage.getCipherStorageName());
        return currentCipherStorage;
    }

    private CipherStorage getCipherStorageByName(String cipherStorageName) {
        return cipherStorageMap.get(cipherStorageName);
    }

    private boolean isFingerprintAuthAvailable() {
        return DeviceAvailability.isFingerprintAuthAvailable(getCurrentActivity());
    }

    @NonNull
    private String getDefaultServiceIfNull(String service) {
        return service == null ? EMPTY_STRING : service;
    }
}
