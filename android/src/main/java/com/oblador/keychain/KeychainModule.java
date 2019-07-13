package com.oblador.keychain;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.facebook.react.bridge.Callback;
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
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionResultHandler;
import com.oblador.keychain.cipherStorage.CipherStorageFacebookConceal;
import com.oblador.keychain.cipherStorage.CipherStorageKeystoreAESCBC;
import com.oblador.keychain.cipherStorage.CipherStorageKeystoreRSAECB;
import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.EmptyParameterException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

import java.security.InvalidKeyException;
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


    public static final String AUTHENTICATION_TYPE_KEY = "authenticationType";
    public static final String AUTHENTICATION_TYPE_DEVICE_PASSCODE_OR_BIOMETRICS = "AuthenticationWithBiometricsDevicePasscode";
    public static final String AUTHENTICATION_TYPE_BIOMETRICS = "AuthenticationWithBiometrics";

    public static final String ACCESS_CONTROL_KEY = "accessControl";
    public static final String ACCESS_CONTROL_BIOMETRY_ANY = "BiometryAny";
    public static final String ACCESS_CONTROL_BIOMETRY_CURRENT_SET = "BiometryCurrentSet";

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            addCipherStorageToMap(new CipherStorageKeystoreRSAECB(reactContext));
        }
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
    public void getSecurityLevel(ReadableMap options, Promise promise) {
        String accessControl = null;
        if (options != null && options.hasKey(ACCESS_CONTROL_KEY)) {
            accessControl = options.getString(ACCESS_CONTROL_KEY);
        }

        boolean useBiometry = getUseBiometry(accessControl);

        promise.resolve(getSecurityLevel(useBiometry).name());
    }

    @ReactMethod
    public void setGenericPasswordForOptions(String service, String username, String password, ReadableMap options, String minimumSecurityLevel, Promise promise) {
        try {
            SecurityLevel level = SecurityLevel.valueOf(minimumSecurityLevel);
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                throw new EmptyParameterException("you passed empty or null username/password");
            }

            String accessControl = null;
            if (options != null && options.hasKey(ACCESS_CONTROL_KEY)) {
                accessControl = options.getString(ACCESS_CONTROL_KEY);
            }

            service = getDefaultServiceIfNull(service);

            CipherStorage currentCipherStorage = getCipherStorageForCurrentAPILevel(getUseBiometry(accessControl));
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
    public void getGenericPasswordForOptions(String service, final Promise promise) {
        final String serviceOrDefault = getDefaultServiceIfNull(service);
        CipherStorage cipherStorage = null;
        try {
            ResultSet resultSet = prefsStorage.getEncryptedEntry(serviceOrDefault);
            if (resultSet == null) {
                Log.e(KEYCHAIN_MODULE, "No entry found for service: " + serviceOrDefault);
                promise.resolve(false);
                return;
            }

            // Android < M will throw an exception as biometry is not supported.
            CipherStorage biometryCipherStorage = null;
            try {
                biometryCipherStorage = getCipherStorageForCurrentAPILevel(true);
            } catch(Exception e) { }
            final CipherStorage nonBiometryCipherStorage = getCipherStorageForCurrentAPILevel(false);
            if (biometryCipherStorage != null && resultSet.cipherStorageName.equals(biometryCipherStorage.getCipherStorageName())) {
                cipherStorage = biometryCipherStorage;
            } else if (nonBiometryCipherStorage != null && resultSet.cipherStorageName.equals(nonBiometryCipherStorage.getCipherStorageName())) {
                cipherStorage = nonBiometryCipherStorage;
            }

            final CipherStorage currentCipherStorage = cipherStorage;
            if (currentCipherStorage != null) {
                DecryptionResultHandler decryptionHandler = new DecryptionResultHandler() {
                    @Override
                    public void onDecrypt(DecryptionResult decryptionResult, String error) {
                        if (decryptionResult != null) {
                            WritableMap credentials = Arguments.createMap();

                            credentials.putString("service", serviceOrDefault);
                            credentials.putString("username", decryptionResult.username);
                            credentials.putString("password", decryptionResult.password);

                            promise.resolve(credentials);
                        } else {
                            promise.reject(E_CRYPTO_FAILED, error);
                        }
                    }
                };
                // The encrypted data is encrypted using the current CipherStorage, so we just decrypt and return
                currentCipherStorage.decrypt(decryptionHandler, serviceOrDefault, resultSet.usernameBytes, resultSet.passwordBytes);
            }
            else {
                // The encrypted data is encrypted using an older CipherStorage, so we need to decrypt the data first, then encrypt it using the current CipherStorage, then store it again and return
                final CipherStorage oldCipherStorage = getCipherStorageByName(resultSet.cipherStorageName);

                DecryptionResultHandler decryptionHandler = new DecryptionResultHandler() {
                    @Override
                    public void onDecrypt(DecryptionResult decryptionResult, String error) {
                        if (decryptionResult != null) {
                            WritableMap credentials = Arguments.createMap();

                            credentials.putString("service", serviceOrDefault);
                            credentials.putString("username", decryptionResult.username);
                            credentials.putString("password", decryptionResult.password);

                            try {
                                migrateCipherStorage(serviceOrDefault, nonBiometryCipherStorage, oldCipherStorage, decryptionResult);
                            } catch (CryptoFailedException e) {
                                Log.e(KEYCHAIN_MODULE, "Migrating to a less safe storage is not allowed. Keeping the old one");
                            } catch (KeyStoreAccessException e) {
                                Log.e(KEYCHAIN_MODULE, e.getMessage());
                                promise.reject(E_KEYSTORE_ACCESS_ERROR, e);
                            }

                            promise.resolve(credentials);
                        } else {
                            promise.reject(E_CRYPTO_FAILED, error);
                        }
                    }
                };
                // decrypt using the older cipher storage
                oldCipherStorage.decrypt(decryptionHandler, serviceOrDefault, resultSet.usernameBytes, resultSet.passwordBytes);
            }
          } catch (InvalidKeyException e) {
              Log.e(KEYCHAIN_MODULE, String.format("Key for service %s permanently invalidated", serviceOrDefault));
               try {
                   cipherStorage.removeKey(serviceOrDefault);
              } catch (Exception error) {
                  Log.e(KEYCHAIN_MODULE, "Failed removing invalidated key: " + error.getMessage());
              }
              promise.resolve(false);
        } catch (CryptoFailedException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_CRYPTO_FAILED, e);
        }
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
    public void setInternetCredentialsForServer(@NonNull String server, String username, String password, ReadableMap options, String minimumSecurityLevel, Promise promise) {
        setGenericPasswordForOptions(server, username, password, options, minimumSecurityLevel, promise);
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
    public void canCheckAuthentication(ReadableMap options, Promise promise) {
        String authenticationType = null;
        if (options != null && options.hasKey(AUTHENTICATION_TYPE_KEY)) {
            authenticationType = options.getString(AUTHENTICATION_TYPE_KEY);
        }

        if (authenticationType == null
                || (!authenticationType.equals(AUTHENTICATION_TYPE_DEVICE_PASSCODE_OR_BIOMETRICS)
                && !authenticationType.equals(AUTHENTICATION_TYPE_BIOMETRICS))) {
            promise.resolve(false);
            return;
        }

        try {
            boolean fingerprintAuthAvailable = isFingerprintAuthAvailable();
            promise.resolve(fingerprintAuthAvailable);
        } catch (Exception e) {
            promise.resolve(false);
        }
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

    private boolean getUseBiometry(String accessControl) {
        return accessControl != null
            && (accessControl.equals(ACCESS_CONTROL_BIOMETRY_ANY)
            || accessControl.equals(ACCESS_CONTROL_BIOMETRY_CURRENT_SET));
    }

    // The "Current" CipherStorage is the cipherStorage with the highest API level that is lower than or equal to the current API level
    private CipherStorage getCipherStorageForCurrentAPILevel(boolean useBiometry) throws CryptoFailedException {
        int currentAPILevel = Build.VERSION.SDK_INT;
        CipherStorage currentCipherStorage = null;
        for (CipherStorage cipherStorage : cipherStorageMap.values()) {
            int cipherStorageAPILevel = cipherStorage.getMinSupportedApiLevel();
            boolean biometrySupported = cipherStorage.getCipherBiometrySupported();
            // Is the cipherStorage supported on the current API level?
            boolean isSupported = (cipherStorageAPILevel <= currentAPILevel)
                    && (biometrySupported == useBiometry);
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

        currentCipherStorage.setCurrentActivity(getCurrentActivity());

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
        CipherStorage storage = cipherStorageMap.get(cipherStorageName);

        storage.setCurrentActivity(getCurrentActivity());

        return storage;
    }

    private boolean isFingerprintAuthAvailable() {
        return DeviceAvailability.isFingerprintAuthAvailable(getReactApplicationContext());
    }

    private boolean isSecureHardwareAvailable(boolean useBiometry) {
        try {
            return getCipherStorageForCurrentAPILevel(useBiometry).supportsSecureHardware();
        } catch (CryptoFailedException e) {
            return false;
        }
    }

    private SecurityLevel getSecurityLevel(boolean useBiometry) {
        try {
            CipherStorage storage = getCipherStorageForCurrentAPILevel(useBiometry);
            if (!storage.securityLevel().satisfiesSafetyThreshold(SecurityLevel.SECURE_SOFTWARE)) {
                return SecurityLevel.ANY;
            }

            if (isSecureHardwareAvailable(useBiometry)) {
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
