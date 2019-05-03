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
import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.EmptyParameterException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

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

    public KeychainModule(ReactApplicationContext reactContext) {
        super(reactContext);
        prefsStorage = new PrefsStorage(reactContext);

        addCipherStorageToMap(new CipherStorageFacebookConceal(reactContext));
        addCipherStorageToMap(new CipherStorageKeystoreAESCBC());
    }

    private void addCipherStorageToMap(CipherStorage cipherStorage) {
        cipherStorageMap.put(cipherStorage.getCipherStorageName(), cipherStorage);
    }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(SecurityLevel.ANY.jsName(), SecurityLevel.ANY.name());
        constants.put(SecurityLevel.SECURE_SOFTWARE.jsName(), SecurityLevel.SECURE_SOFTWARE.name());
        constants.put(SecurityLevel.SECURE_HARDWARE.jsName(), SecurityLevel.SECURE_HARDWARE.name());
        return constants;
    }

    @ReactMethod
    public void getSecurityLevel(Promise promise) {
        promise.resolve(getSecurityLevel().name());
    }

    @ReactMethod
    public void setGenericPasswordForOptions(String service, String username, String password, String minimumSecurityLevel, Promise promise) {
        try {
            SecurityLevel level = SecurityLevel.valueOf(minimumSecurityLevel);
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                throw new EmptyParameterException("you passed empty or null username/password");
            }
            service = getDefaultServiceIfNull(service);

            CipherStorage currentCipherStorage = getCipherStorageForCurrentAPILevel();
            validateCipherStorageSecurityLevel(currentCipherStorage, level);

            EncryptionResult result = currentCipherStorage.encrypt(service, username, password, level);
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
    public void getGenericPasswordForOptions(String service, Promise promise) {
        try {
            service = getDefaultServiceIfNull(service);

            CipherStorage currentCipherStorage = getCipherStorageForCurrentAPILevel();

            ResultSet resultSet = prefsStorage.getEncryptedEntry(service);
            if (resultSet == null) {
                Log.e(KEYCHAIN_MODULE, "No entry found for service: " + service);
                promise.resolve(false);
                return;
            }

            final DecryptionResult decryptionResult = decryptCredentials(service, currentCipherStorage, resultSet);

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

    private DecryptionResult decryptCredentials(String service, CipherStorage currentCipherStorage, ResultSet resultSet) throws CryptoFailedException, KeyStoreAccessException {
        if (resultSet.cipherStorageName.equals(currentCipherStorage.getCipherStorageName())) {
            // The encrypted data is encrypted using the current CipherStorage, so we just decrypt and return
            return currentCipherStorage.decrypt(service, resultSet.usernameBytes, resultSet.passwordBytes);
        }

        // The encrypted data is encrypted using an older CipherStorage, so we need to decrypt the data first, then encrypt it using the current CipherStorage, then store it again and return
        CipherStorage oldCipherStorage = getCipherStorageByName(resultSet.cipherStorageName);
        // decrypt using the older cipher storage

        DecryptionResult decryptionResult = oldCipherStorage.decrypt(service, resultSet.usernameBytes, resultSet.passwordBytes);
        // encrypt using the current cipher storage

        try {
            migrateCipherStorage(service, currentCipherStorage, oldCipherStorage, decryptionResult);
        } catch (CryptoFailedException e) {
            Log.e(KEYCHAIN_MODULE, "Migrating to a less safe storage is not allowed. Keeping the old one");
        }

        return decryptionResult;
    }

    private void migrateCipherStorage(String service, CipherStorage newCipherStorage, CipherStorage oldCipherStorage, DecryptionResult decryptionResult) throws KeyStoreAccessException, CryptoFailedException {
        // don't allow to degrade security level when transferring, the new storage should be as safe as the old one.
        EncryptionResult encryptionResult = newCipherStorage.encrypt(service, decryptionResult.username, decryptionResult.password, decryptionResult.getSecurityLevel());
        // store the encryption result
        prefsStorage.storeEncryptedEntry(service, encryptionResult);
        // clean up the old cipher storage
        oldCipherStorage.removeKey(service);
    }

    @ReactMethod
    public void resetGenericPasswordForOptions(String service, Promise promise) {
        try {
            service = getDefaultServiceIfNull(service);

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
    public void hasInternetCredentialsForServer(@NonNull String server, Promise promise) {
        final String defaultService = getDefaultServiceIfNull(server);

        ResultSet resultSet = prefsStorage.getEncryptedEntry(defaultService);
        if (resultSet == null) {
            Log.e(KEYCHAIN_MODULE, "No entry found for service: " + defaultService);
            promise.resolve(false);
            return;
        }

        promise.resolve(true);
    }

    @ReactMethod
    public void setInternetCredentialsForServer(@NonNull String server, String username, String password, String minimumSecurityLevel, ReadableMap unusedOptions, Promise promise) {
        setGenericPasswordForOptions(server, username, password, minimumSecurityLevel, promise);
    }

    @ReactMethod
    public void getInternetCredentialsForServer(@NonNull String server, ReadableMap unusedOptions, Promise promise) {
        getGenericPasswordForOptions(server, promise);
    }

    @ReactMethod
    public void resetInternetCredentialsForServer(@NonNull String server, ReadableMap unusedOptions, Promise promise) {
        resetGenericPasswordForOptions(server, promise);
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
    private CipherStorage getCipherStorageForCurrentAPILevel() throws CryptoFailedException {
        int currentAPILevel = Build.VERSION.SDK_INT;
        CipherStorage currentCipherStorage = null;
        for (CipherStorage cipherStorage : cipherStorageMap.values()) {
            int cipherStorageAPILevel = cipherStorage.getMinSupportedApiLevel();
            // Is the cipherStorage supported on the current API level?
            boolean isSupported = (cipherStorageAPILevel <= currentAPILevel);
            if (!isSupported) {
                continue;
            }
            // Is the API level better than the one we previously selected (if any)?
            if (currentCipherStorage == null || cipherStorageAPILevel > currentCipherStorage.getMinSupportedApiLevel()) {
                currentCipherStorage = cipherStorage;
            }
        }
        if (currentCipherStorage == null) {
            throw new CryptoFailedException("Unsupported Android SDK " + Build.VERSION.SDK_INT);
        }
        return currentCipherStorage;
    }

    private void validateCipherStorageSecurityLevel(CipherStorage cipherStorage, SecurityLevel requiredLevel) throws CryptoFailedException {
        if (cipherStorage.securityLevel().satisfiesSafetyThreshold(requiredLevel)) {
            return;
        }

        throw new CryptoFailedException(
                String.format(
                    "Cipher Storage is too weak. Required security level is: %s, but only %s is provided",
                    requiredLevel.name(),
                    cipherStorage.securityLevel().name()));
    }


    private CipherStorage getCipherStorageByName(String cipherStorageName) {
        return cipherStorageMap.get(cipherStorageName);
    }

    private boolean isFingerprintAuthAvailable() {
        return DeviceAvailability.isFingerprintAuthAvailable(getReactApplicationContext());
    }

    private boolean isSecureHardwareAvailable() {
        try {
            return getCipherStorageForCurrentAPILevel().supportsSecureHardware();
        } catch (CryptoFailedException e) {
            return false;
        }
    }

    private SecurityLevel getSecurityLevel() {
        try {
            CipherStorage storage = getCipherStorageForCurrentAPILevel();
            if (!storage.securityLevel().satisfiesSafetyThreshold(SecurityLevel.SECURE_SOFTWARE)) {
                return SecurityLevel.ANY;
            }

            if (isSecureHardwareAvailable()) {
                return SecurityLevel.SECURE_HARDWARE;
            } else {
                return SecurityLevel.SECURE_SOFTWARE;
            }
        } catch (CryptoFailedException e) {
            return SecurityLevel.ANY;
        }
    }



    @NonNull
    private String getDefaultServiceIfNull(String service) {
        return service == null ? EMPTY_STRING : service;
    }
}
