package com.oblador.keychain;

import android.app.KeyguardManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
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
import com.oblador.keychain.exceptions.NotSecureException;
import com.oblador.keychain.exceptions.RequiresAuthenticationException;

import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

public class KeychainModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    public static final String E_EMPTY_PARAMETERS = "E_EMPTY_PARAMETERS";
    public static final String E_CRYPTO_FAILED = "E_CRYPTO_FAILED";
    public static final String E_KEYSTORE_ACCESS_ERROR = "E_KEYSTORE_ACCESS_ERROR";
    public static final String E_SUPPORTED_BIOMETRY_ERROR = "E_SUPPORTED_BIOMETRY_ERROR";
    public static final String E_KEYGUARD_IS_NOT_SECURE = "E_KEYGUARD_IS_NOT_SECURE";
    public static final String KEYCHAIN_MODULE = "RNKeychainManager";
    public static final String FINGERPRINT_SUPPORTED_NAME = "Fingerprint";
    public static final String EMPTY_STRING = "";
    public static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS_SET = 9999931;
    public static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS_GET = 9999932;

    private final Map<String, CipherStorage> cipherStorageMap = new HashMap<>();
    private final PrefsStorage prefsStorage;
    private KeyguardManager mKeyguardManager;

    private String currentService;
    private String currentUsername;
    private String currentPassword;
    private Promise currentPromise;

    @Override
    public String getName() {
        return KEYCHAIN_MODULE;
    }

    public KeychainModule(ReactApplicationContext reactContext) {
        super(reactContext);
        prefsStorage = new PrefsStorage(reactContext);

        mKeyguardManager = (KeyguardManager) reactContext.getSystemService(Context.KEYGUARD_SERVICE);
        reactContext.addActivityEventListener(this);

        addCipherStorageToMap(new CipherStorageFacebookConceal(reactContext));
        addCipherStorageToMap(new CipherStorageKeystoreAESCBC());
    }

    private void addCipherStorageToMap(CipherStorage cipherStorage) {
        cipherStorageMap.put(cipherStorage.getCipherStorageName(), cipherStorage);
    }

    @ReactMethod
    public void setGenericPasswordForOptions(String service, String username, String password, Promise promise) {
        try {
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                throw new EmptyParameterException("you passed empty or null username/password");
            }
            if (!isSecure()) {
              throw new NotSecureException("Secure lock screen hasn't set up");
            }
            service = getDefaultServiceIfNull(service);

            CipherStorage currentCipherStorage = getCipherStorageForCurrentAPILevel();

            EncryptionResult result = currentCipherStorage.encrypt(service, username, password);
            prefsStorage.storeEncryptedEntry(service, result);

            promise.resolve(true);
        } catch (NotSecureException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_KEYGUARD_IS_NOT_SECURE, e);
        } catch (RequiresAuthenticationException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            currentService = service;
            currentUsername = username;
            currentPassword = password;
            currentPromise = promise;
            showAuthenticationScreen(REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS_SET);
        } catch (EmptyParameterException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_EMPTY_PARAMETERS, e);
        } catch (CryptoFailedException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_CRYPTO_FAILED, e);
        }
    }

    private void showAuthenticationScreen(int code) {
      // Create the Confirm Credentials screen. You can customize the title and description. Or
      // we will provide a generic one for you if you leave it null
      Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null);
      if (intent != null) {
        getCurrentActivity().startActivityForResult(intent, code);
      }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
      if (REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS_SET == requestCode || REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS_GET == requestCode) {
        if (resultCode != RESULT_OK) {
          // The user canceled or didn’t complete the lock screen
          // operation. Go to error/cancellation flow.
          if (currentPromise != null) {
            currentPromise.reject(E_SUPPORTED_BIOMETRY_ERROR);
          }
          return;
        }
        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS_SET) {
          // Challenge completed, proceed with using cipher
          setGenericPasswordForOptions(currentService, currentUsername, currentPassword, currentPromise);
        } else if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS_GET) {
          getGenericPasswordForOptions(currentService, currentPromise);
        }
      }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onNewIntent(Intent intent) {
      Log.d(getName(), "onNewIntent");
    }

    @ReactMethod
    public void getGenericPasswordForOptions(String service, Promise promise) {
        try {
            service = getDefaultServiceIfNull(service);

            CipherStorage currentCipherStorage = getCipherStorageForCurrentAPILevel();

            final DecryptionResult decryptionResult;
            ResultSet resultSet = prefsStorage.getEncryptedEntry(service);
            if (resultSet == null) {
                Log.e(KEYCHAIN_MODULE, "No entry found for service: " + service);
                promise.resolve(false);
                return;
            }

            if (resultSet.cipherStorageName.equals(currentCipherStorage.getCipherStorageName())) {
                // The encrypted data is encrypted using the current CipherStorage, so we just decrypt and return
                decryptionResult = currentCipherStorage.decrypt(service, resultSet.usernameBytes, resultSet.passwordBytes);
            }
            else {
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
            }

            WritableMap credentials = Arguments.createMap();

            credentials.putString("service", service);
            credentials.putString("username", decryptionResult.username);
            credentials.putString("password", decryptionResult.password);

            promise.resolve(credentials);
        } catch (RequiresAuthenticationException e) {
          Log.e(KEYCHAIN_MODULE, e.getMessage());
          currentService = service;
          currentPromise = promise;
          showAuthenticationScreen(REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS_GET);
        } catch (KeyStoreAccessException e) {
            Log.e(KEYCHAIN_MODULE, e.getMessage());
            promise.reject(E_KEYSTORE_ACCESS_ERROR, e);
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
        return DeviceAvailability.isFingerprintAuthAvailable(getReactApplicationContext());
    }

    private boolean isSecure() {
      return DeviceAvailability.isSecure(getCurrentActivity());
    }

  @NonNull
    private String getDefaultServiceIfNull(String service) {
        return service == null ? EMPTY_STRING : service;
    }

}
