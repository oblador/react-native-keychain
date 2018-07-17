package com.oblador.keychain;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.oblador.keychain.PrefsStorage.ResultSet;
import com.oblador.keychain.cipherStorage.CipherStorage;
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionResult;
import com.oblador.keychain.cipherStorage.CipherStorage.EncryptionResult;
import com.oblador.keychain.cipherStorage.CipherStorage.EncryptionResultHandler;
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionResultHandler;
import com.oblador.keychain.cipherStorage.CipherStorageFacebookConceal;
import com.oblador.keychain.cipherStorage.CipherStorageKeystoreAESCBC;
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


    public static final String AUTHENTICATION_TYPE_KEY = "authenticationType";
    public static final String AUTHENTICATION_TYPE_DEVICE_PASSCODE_OR_BIOMETRICS = "AuthenticationWithBiometricsDevicePasscode";
    public static final String AUTHENTICATION_TYPE_BIOMETRICS = "AuthenticationWithBiometrics";

    public static final String ACCESS_CONTROL_KEY = "accessControl";
    public static final String ACCESS_CONTROL_BIOMETRY_ANY = "BiometryAny";
    public static final String ACCESS_CONTROL_BIOMETRY_CURRENT_SET = "BiometryCurrentSet";

    private final Map<String, CipherStorage> cipherStorageMap = new HashMap<>();
    private final PrefsStorage prefsStorage;
    final ReactApplicationContext mReactContext;

    @Override
    public String getName() {
        return KEYCHAIN_MODULE;
    }

    public KeychainModule(ReactApplicationContext reactContext) {
        super(reactContext);
        prefsStorage = new PrefsStorage(reactContext);
        mReactContext = reactContext;

        addCipherStorageToMap(new CipherStorageFacebookConceal(reactContext));
        addCipherStorageToMap(new CipherStorageKeystoreAESCBC(reactContext));
    }

    private void addCipherStorageToMap(CipherStorage cipherStorage) {
        cipherStorageMap.put(cipherStorage.getCipherStorageName(), cipherStorage);
    }


    // That would show the android native fingerprint/Passcode activity
    // could use that for BiometryAnyOrDevicePasscode or BiometryCurrentSetOrDevicePasscode
    // private KeyguardManager mKeyguardManager;
    // @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    // private void showAuthenticationScreen() {
    //     // Create the Confirm Credentials screen. You can customize the title and description. Or
    //     // we will provide a generic one for you if you leave it null
    //     mKeyguardManager = (KeyguardManager) mReactContext.getSystemService(Context.KEYGUARD_SERVICE);
    //     Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null);
    //     if (intent != null) {
    //         mReactContext.startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS, null);
    //     }
    // }

    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;

    @ReactMethod
    public void setGenericPasswordForOptions(String service, ReadableMap options, String username, String password, final Promise promise) {
        final String defaultService = getDefaultServiceIfNull(service);
        String accessControl = null;
        if (options.hasKey(ACCESS_CONTROL_KEY)) {
            accessControl = options.getString(ACCESS_CONTROL_KEY);
        }

        EncryptionResultHandler handler = new EncryptionResultHandler() {
            @Override
            public void onEncryptionResult(EncryptionResult encryptionResult, String info, String error) {
                if (encryptionResult != null) {
                    prefsStorage.storeEncryptedEntry(defaultService, encryptionResult);
                    promise.resolve("KeychainModule saved the data");
                } else if (info != null) {
                    KeychainModule.this.sendFingerprintEvent(info);
                } else {
                    promise.reject(E_CRYPTO_FAILED, error);
                }
            }
        };

        try {
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                throw new EmptyParameterException("you passed empty or null username/password");
            }

            CipherStorage currentCipherStorage = getCipherStorageForCurrentAPILevel();
            boolean useBiometry = accessControl != null
                && (accessControl.equals(ACCESS_CONTROL_BIOMETRY_ANY)
                    || accessControl.equals(ACCESS_CONTROL_BIOMETRY_CURRENT_SET));

            currentCipherStorage.encrypt(handler, defaultService, username, password, useBiometry);
        } catch (EmptyParameterException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_EMPTY_PARAMETERS, e);
        } catch (CryptoFailedException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_CRYPTO_FAILED, e);
        }
    }

    @ReactMethod
    public void getGenericPasswordForOptions(String service, ReadableMap options, final Promise promise) {
        try {
            final String defaultService = getDefaultServiceIfNull(service);
            String accessControl = null;
            if (options != null && options.hasKey(ACCESS_CONTROL_KEY)) {
                accessControl = options.getString(ACCESS_CONTROL_KEY);
            }

            ResultSet resultSet = prefsStorage.getEncryptedEntry(defaultService);
            if (resultSet == null) {
                Log.e(KEYCHAIN_MODULE, "No entry found for service: " + defaultService);
                promise.resolve(false);
                return;
            }

            final CipherStorage currentCipherStorage = getCipherStorageForCurrentAPILevel();
            final boolean useBiometry = (accessControl == ACCESS_CONTROL_BIOMETRY_ANY || accessControl == ACCESS_CONTROL_BIOMETRY_CURRENT_SET);

            if (resultSet.cipherStorageName.equals(currentCipherStorage.getCipherStorageName())) {
                DecryptionResultHandler decryptionHandler = new DecryptionResultHandler() {
                    @Override
                    public void onDecryptionResult(DecryptionResult decryptionResult, String info, String error) {
                        if (decryptionResult != null) {
                            WritableMap credentials = Arguments.createMap();

                            credentials.putString("service", defaultService);
                            credentials.putString("username", decryptionResult.username);
                            credentials.putString("password", decryptionResult.password);

                            promise.resolve(credentials);
                        } else if (info != null) {
                            KeychainModule.this.sendFingerprintEvent(info);
                        } else {
                            promise.reject(E_CRYPTO_FAILED, error);
                        }
                    }
                };
                // The encrypted data is encrypted using the current CipherStorage, so we just decrypt and return
                currentCipherStorage.decrypt(decryptionHandler, defaultService, resultSet.usernameBytes, resultSet.passwordBytes, useBiometry);
            }
            else {
                // The encrypted data is encrypted using an older CipherStorage, so we need to decrypt the data first, then encrypt it using the current CipherStorage, then store it again and return
                final CipherStorage oldCipherStorage = getCipherStorageByName(resultSet.cipherStorageName);

                DecryptionResultHandler decryptionHandler = new DecryptionResultHandler() {
                    @Override
                    public void onDecryptionResult(DecryptionResult decryptionResult, String info, String error) {
                        if (decryptionResult != null) {
                            final WritableMap credentials = Arguments.createMap();
                            credentials.putString("service", defaultService);
                            credentials.putString("username", decryptionResult.username);
                            credentials.putString("password", decryptionResult.password);

                            EncryptionResultHandler encryptionHandler = new EncryptionResultHandler() {
                                @Override
                                public void onEncryptionResult(EncryptionResult encryptionResult, String info, String error) {
                                    if (encryptionResult != null) {
                                        try {
                                            // store the encryption result
                                            prefsStorage.storeEncryptedEntry(defaultService, encryptionResult);
                                            // clean up the old cipher storage
                                            oldCipherStorage.removeKey(defaultService);

                                            promise.resolve(credentials);
                                        } catch (KeyStoreAccessException e) {
                                            Log.e(KEYCHAIN_MODULE, e.getMessage());
                                            promise.reject(E_KEYSTORE_ACCESS_ERROR, e);
                                        }
                                    } else if (error != null) {
                                        promise.reject(E_CRYPTO_FAILED, error);
                                    }
                                }
                            };

                            try {
                                // encrypt using the current cipher storage
                                currentCipherStorage.encrypt(encryptionHandler, defaultService, decryptionResult.username, decryptionResult.password, useBiometry);
                            } catch (CryptoFailedException e) {
                                promise.reject(E_CRYPTO_FAILED, error);
                            }
                        } else if (info != null) {
                            KeychainModule.this.sendFingerprintEvent(info);
                        } else {
                            promise.reject(E_CRYPTO_FAILED, error);
                        }
                    }
                };
                // decrypt using the older cipher storage
                oldCipherStorage.decrypt(decryptionHandler, defaultService, resultSet.usernameBytes, resultSet.passwordBytes, useBiometry);
            }

            final DecryptionResult decryptionResult;
        } catch (CryptoFailedException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_CRYPTO_FAILED, e);
        }
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
    public void setInternetCredentialsForServer(@NonNull String server, String username, String password, ReadableMap options, Promise promise) {
        setGenericPasswordForOptions(server, options, username, password, promise);
    }

    @ReactMethod
    public void getInternetCredentialsForServer(@NonNull String server, ReadableMap options, Promise promise) {
        getGenericPasswordForOptions(server, options, promise);
    }

    @ReactMethod
    public void resetInternetCredentialsForServer(@NonNull String server, ReadableMap unusedOptions, Promise promise) {
        resetGenericPasswordForOptions(server, promise);
    }


    private void sendFingerprintEvent(String message) {
      mReactContext
          .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
          .emit("keychainFingerprintInfo", message);
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

    // The "Current" CipherStorage is the cipherStorage with the highest API level that is lower than or equal to the current API level
    private CipherStorage getCipherStorageForCurrentAPILevel() throws CryptoFailedException {
        int currentAPILevel = Build.VERSION.SDK_INT;
        CipherStorage currentCipherStorage = null;
        for (CipherStorage cipherStorage : cipherStorageMap.values()) {
            int cipherStorageAPILevel = cipherStorage.getMinSupportedApiLevel();
            // Is the cipherStorage supported on the current API level?
            boolean isSupported = (cipherStorageAPILevel <= currentAPILevel);
            // Is the API level better than the one we previously selected (if any)?
            if (isSupported && (currentCipherStorage == null || cipherStorageAPILevel > currentCipherStorage.getMinSupportedApiLevel())) {
                currentCipherStorage = cipherStorage;
            }
        }
        if (currentCipherStorage == null) {
            throw new CryptoFailedException("Unsupported Android SDK " + Build.VERSION.SDK_INT);
        }
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
