package com.oblador.keychain;

import android.os.Build;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.AssertionException;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.oblador.keychain.PrefsStorage.ResultSet;
import com.oblador.keychain.cipherStorage.CipherStorage;
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionContext;
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionResult;
import com.oblador.keychain.cipherStorage.CipherStorage.DecryptionResultHandler;
import com.oblador.keychain.cipherStorage.CipherStorage.EncryptionResult;
import com.oblador.keychain.cipherStorage.CipherStorageBase;
import com.oblador.keychain.cipherStorage.CipherStorageFacebookConceal;
import com.oblador.keychain.cipherStorage.CipherStorageKeystoreAesCbc;
import com.oblador.keychain.cipherStorage.CipherStorageKeystoreRsaEcb;
import com.oblador.keychain.cipherStorage.CipherStorageKeystoreRsaEcb.NonInteractiveHandler;
import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.EmptyParameterException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.oblador.keychain.SecurityLevel.ANY;
import static com.oblador.keychain.SecurityLevel.SECURE_HARDWARE;
import static com.oblador.keychain.SecurityLevel.SECURE_SOFTWARE;

@SuppressWarnings({"unused", "WeakerAccess"})
public class KeychainModule extends ReactContextBaseJavaModule {
  //region Constants
  public static final String KEYCHAIN_MODULE = "RNKeychainManager";
  public static final String FINGERPRINT_SUPPORTED_NAME = "Fingerprint";
  public static final String EMPTY_STRING = "";
  public static final String ACCESS_CONTROL_BIOMETRY_ANY = "BiometryAny";
  public static final String ACCESS_CONTROL_BIOMETRY_CURRENT_SET = "BiometryCurrentSet";

  @interface Maps {
    String SERVICE = "service";
    String USERNAME = "username";
    String PASSWORD = "password";
  }

  @interface Errors {
    String E_EMPTY_PARAMETERS = "E_EMPTY_PARAMETERS";
    String E_CRYPTO_FAILED = "E_CRYPTO_FAILED";
    String E_KEYSTORE_ACCESS_ERROR = "E_KEYSTORE_ACCESS_ERROR";
    String E_SUPPORTED_BIOMETRY_ERROR = "E_SUPPORTED_BIOMETRY_ERROR";
    /** Raised for unexpected errors. */
    String E_UNKNOWN_ERROR = "E_UNKNOWN_ERROR";
  }

  //endregion

  //region Members
  /** Name-to-instance lookup  map. */
  private final Map<String, CipherStorage> cipherStorageMap = new HashMap<>();
  /** Shared preferences storage. */
  private final PrefsStorage prefsStorage;
  //endregion

  public KeychainModule(@NonNull final ReactApplicationContext reactContext) {
    super(reactContext);
    prefsStorage = new PrefsStorage(reactContext);

    addCipherStorageToMap(new CipherStorageFacebookConceal(reactContext));
    addCipherStorageToMap(new CipherStorageKeystoreAesCbc());

    // we have a references to newer api that will fail load of app classes in old androids OS
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      addCipherStorageToMap(new CipherStorageKeystoreRsaEcb());
    }
  }

  //region Overrides

  /** {@inheritDoc} */
  @Override
  @NonNull
  public String getName() {
    return KEYCHAIN_MODULE;
  }

  /** {@inheritDoc} */
  @NonNull
  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();

    constants.put(ANY.jsName(), ANY.name());
    constants.put(SECURE_SOFTWARE.jsName(), SECURE_SOFTWARE.name());
    constants.put(SECURE_HARDWARE.jsName(), SECURE_HARDWARE.name());

    return constants;
  }
  //endregion

  //region React Methods
  @ReactMethod
  public void getSecurityLevel(@NonNull final String accessControl,
                               @NonNull final Promise promise) {
    final boolean useBiometry = getUseBiometry(accessControl);

    promise.resolve(getSecurityLevel().name());
  }

  @ReactMethod
  public void setGenericPasswordForOptions(@Nullable final String service,
                                           @NonNull final String username,
                                           @NonNull final String password,
                                           @NonNull final String minimumSecurityLevel,
                                           @NonNull final Promise promise) {
    try {
      throwIfEmptyLoginPassword(username, password);

      final SecurityLevel level = SecurityLevel.valueOf(minimumSecurityLevel);
      final String safeService = getDefaultServiceIfNull(service);
      final CipherStorage storage = getCipherStorageForCurrentAPILevel();

      throwIfInsufficientLevel(storage, level);

      final EncryptionResult result = storage.encrypt(safeService, username, password, level);
      prefsStorage.storeEncryptedEntry(safeService, result);

      promise.resolve(true);
    } catch (EmptyParameterException e) {
      Log.e(KEYCHAIN_MODULE, e.getMessage());

      promise.reject(Errors.E_EMPTY_PARAMETERS, e);
    } catch (CryptoFailedException e) {
      Log.e(KEYCHAIN_MODULE, e.getMessage());

      promise.reject(Errors.E_CRYPTO_FAILED, e);
    } catch (Throwable fail) {
      Log.e(KEYCHAIN_MODULE, fail.getMessage(), fail);

      promise.reject(Errors.E_UNKNOWN_ERROR, fail);
    }
  }

  @ReactMethod
  public void getGenericPasswordForOptions(@Nullable final String service,
                                           @NonNull final Promise promise) {
    try {
      final String safeService = getDefaultServiceIfNull(service);
      final CipherStorage currentCipherStorage = getCipherStorageForCurrentAPILevel();
      final ResultSet resultSet = prefsStorage.getEncryptedEntry(safeService);

      if (resultSet == null) {
        Log.e(KEYCHAIN_MODULE, "No entry found for service: " + service);
        promise.resolve(false);
        return;
      }

      final DecryptionResult decryptionResult = decryptCredentials(safeService, currentCipherStorage, resultSet);

      final WritableMap credentials = Arguments.createMap();
      credentials.putString(Maps.SERVICE, safeService);
      credentials.putString(Maps.USERNAME, decryptionResult.username);
      credentials.putString(Maps.PASSWORD, decryptionResult.password);

      promise.resolve(credentials);
    } catch (KeyStoreAccessException e) {
      Log.e(KEYCHAIN_MODULE, e.getMessage());
      promise.reject(Errors.E_KEYSTORE_ACCESS_ERROR, e);
    } catch (CryptoFailedException e) {
      Log.e(KEYCHAIN_MODULE, e.getMessage());
      promise.reject(Errors.E_CRYPTO_FAILED, e);
    } catch (Throwable fail) {
      Log.e(KEYCHAIN_MODULE, fail.getMessage(), fail);

      promise.reject(Errors.E_UNKNOWN_ERROR, fail);
    }
  }

  @ReactMethod
  public void resetGenericPasswordForOptions(@Nullable String service,
                                             @NonNull final Promise promise) {
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
      promise.reject(Errors.E_KEYSTORE_ACCESS_ERROR, e);
    } catch (Throwable fail) {
      Log.e(KEYCHAIN_MODULE, fail.getMessage(), fail);

      promise.reject(Errors.E_UNKNOWN_ERROR, fail);
    }
  }

  @ReactMethod
  public void hasInternetCredentialsForServer(@NonNull String server,
                                              @NonNull final Promise promise) {
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
  public void setInternetCredentialsForServer(@NonNull String server,
                                              String username,
                                              String password,
                                              String minimumSecurityLevel,
                                              ReadableMap unusedOptions,
                                              @NonNull final Promise promise) {
    setGenericPasswordForOptions(server, username, password, minimumSecurityLevel, promise);
  }

  @ReactMethod
  public void getInternetCredentialsForServer(@NonNull String server,
                                              ReadableMap unusedOptions,
                                              @NonNull final Promise promise) {
    getGenericPasswordForOptions(server, promise);
  }

  @ReactMethod
  public void resetInternetCredentialsForServer(@NonNull String server,
                                                ReadableMap unusedOptions,
                                                @NonNull final Promise promise) {
    resetGenericPasswordForOptions(server, promise);
  }

  @ReactMethod
  public void getSupportedBiometryType(@NonNull final Promise promise) {
    try {
      boolean fingerprintAuthAvailable = isFingerprintAuthAvailable();

      if (fingerprintAuthAvailable) {
        promise.resolve(FINGERPRINT_SUPPORTED_NAME);
      } else {
        promise.resolve(null);
      }
    } catch (Exception e) {
      Log.e(KEYCHAIN_MODULE, e.getMessage(), e);

      promise.reject(Errors.E_SUPPORTED_BIOMETRY_ERROR, e);
    } catch (Throwable fail) {
      Log.e(KEYCHAIN_MODULE, fail.getMessage(), fail);

      promise.reject(Errors.E_UNKNOWN_ERROR, fail);
    }
  }
  //endregion

  //region Implementation

  /** Is provided access control string matching biometry use request? */
  public static boolean getUseBiometry(@Nullable final String accessControl) {
    return accessControl != null
      && (accessControl.equals(ACCESS_CONTROL_BIOMETRY_ANY)
      || accessControl.equals(ACCESS_CONTROL_BIOMETRY_CURRENT_SET));
  }

  private void addCipherStorageToMap(@NonNull final CipherStorage cipherStorage) {
    cipherStorageMap.put(cipherStorage.getCipherStorageName(), cipherStorage);
  }

  /**
   * Extract credentials from current storage. In case if current storage is not matching
   * results set then executed migration.
   */
  @NonNull
  private DecryptionResult decryptCredentials(@NonNull final String alias,
                                              @NonNull final CipherStorage current,
                                              @NonNull final ResultSet resultSet)
    throws CryptoFailedException, KeyStoreAccessException {
    final String storageName = resultSet.cipherStorageName;

    // The encrypted data is encrypted using the current CipherStorage, so we just decrypt and return
    if (storageName.equals(current.getCipherStorageName())) {
      final DecryptionResultHandler handler = getInteractiveHandler(current);
      current.decrypt(handler, alias, resultSet.username, resultSet.password, ANY);

      CryptoFailedException.reThrowOnError(handler.getError());

      if (null == handler.getResult()) {
        throw new CryptoFailedException("No decryption results and no error. Something deeply wrong!");
      }

      return handler.getResult();
    }

    // The encrypted data is encrypted using an older CipherStorage, so we need to decrypt the data first, then encrypt it using the current CipherStorage, then store it again and return
    final CipherStorage oldStorage = getCipherStorageByName(storageName);
    if (null == oldStorage) {
      throw new KeyStoreAccessException("Wrong cipher storage name: " + storageName);
    }

    // decrypt using the older cipher storage
    final DecryptionResult decryptionResult = oldStorage.decrypt(
      alias, resultSet.username, resultSet.password, ANY);

    try {
      // encrypt using the current cipher storage
      migrateCipherStorage(alias, current, oldStorage, decryptionResult);
    } catch (CryptoFailedException e) {
      Log.w(KEYCHAIN_MODULE, "Migrating to a less safe storage is not allowed. Keeping the old one");
    }

    return decryptionResult;
  }

  /** Get instance of handler that resolves access to the keystore on system request. */
  @NonNull
  protected DecryptionResultHandler getInteractiveHandler(@NonNull final CipherStorage current) {
    if (current.isBiometrySupported() && isFingerprintAuthAvailable()) {
      return new InteractiveBiometric(current);
    }

    return new NonInteractiveHandler();
  }

  /** Remove key from old storage and add it to the new storage. */
  /* package */ void migrateCipherStorage(@NonNull final String service,
                                          @NonNull final CipherStorage newCipherStorage,
                                          @NonNull final CipherStorage oldCipherStorage,
                                          @NonNull final DecryptionResult decryptionResult)
    throws KeyStoreAccessException, CryptoFailedException {

    // don't allow to degrade security level when transferring, the new
    // storage should be as safe as the old one.
    final EncryptionResult encryptionResult = newCipherStorage.encrypt(
      service, decryptionResult.username, decryptionResult.password,
      decryptionResult.getSecurityLevel());

    // store the encryption result
    prefsStorage.storeEncryptedEntry(service, encryptionResult);

    // clean up the old cipher storage
    oldCipherStorage.removeKey(service);
  }

  /**
   * The "Current" CipherStorage is the cipherStorage with the highest API level that is
   * lower than or equal to the current API level
   */
  @NonNull
  /* package */ CipherStorage getCipherStorageForCurrentAPILevel() throws CryptoFailedException {
    final int currentApiLevel = Build.VERSION.SDK_INT;
    final boolean isFingerprint = isFingerprintAuthAvailable();
    CipherStorage foundCipher = null;

    for (CipherStorage variant : cipherStorageMap.values()) {
      Log.d(KEYCHAIN_MODULE, "Probe cipher storage: " + variant.getClass().getSimpleName());

      // Is the cipherStorage supported on the current API level?
      final int minApiLevel = variant.getMinSupportedApiLevel();
      final int capabilityLevel = variant.getCapabilityLevel();
      final boolean isSupportedApi = (minApiLevel <= currentApiLevel);

      // API not supported
      if (!isSupportedApi) continue;

      // Is the API level better than the one we previously selected (if any)?
      if (foundCipher != null && capabilityLevel < foundCipher.getCapabilityLevel()) continue;

      // if biometric supported but not configured properly than skip
      if (variant.isBiometrySupported() && !isFingerprint) continue;

      // remember storage with the best capabilities
      foundCipher = variant;
    }

    if (foundCipher == null) {
      throw new CryptoFailedException("Unsupported Android SDK " + Build.VERSION.SDK_INT);
    }

    Log.d(KEYCHAIN_MODULE, "Selected storage: " + foundCipher.getClass().getSimpleName());

    return foundCipher;
  }

  /** Throw exception in case of empty credentials providing. */
  public static void throwIfEmptyLoginPassword(@Nullable final String username,
                                               @Nullable final String password)
    throws EmptyParameterException {
    if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
      throw new EmptyParameterException("you passed empty or null username/password");
    }
  }

  /** Throw exception if required security level does not match storage provided security level. */
  public static void throwIfInsufficientLevel(@NonNull final CipherStorage storage,
                                              @NonNull final SecurityLevel level)
    throws CryptoFailedException {
    if (storage.securityLevel().satisfiesSafetyThreshold(level)) {
      return;
    }

    throw new CryptoFailedException(
      String.format(
        "Cipher Storage is too weak. Required security level is: %s, but only %s is provided",
        level.name(),
        storage.securityLevel().name()));
  }

  @Nullable
  private CipherStorage getCipherStorageByName(@NonNull final String cipherStorageName) {
    return cipherStorageMap.get(cipherStorageName);
  }

  /** True - if fingerprint hardware available and configured, otherwise false. */
  /* package */ boolean isFingerprintAuthAvailable() {
    return DeviceAvailability.isFingerprintAuthAvailable(getReactApplicationContext());
  }

  /** Is secured hardware a part of current storage or not. */
  /* package */ boolean isSecureHardwareAvailable() {
    try {
      return getCipherStorageForCurrentAPILevel().supportsSecureHardware();
    } catch (CryptoFailedException e) {
      return false;
    }
  }

  /** Resolve storage to security level it provides. */
  @NonNull
  private SecurityLevel getSecurityLevel() {
    try {
      final CipherStorage storage = getCipherStorageForCurrentAPILevel();

      if (!storage.securityLevel().satisfiesSafetyThreshold(SECURE_SOFTWARE)) {
        return ANY;
      }

      if (isSecureHardwareAvailable()) {
        return SECURE_HARDWARE;
      }

      return SECURE_SOFTWARE;
    } catch (CryptoFailedException e) {
      Log.w(KEYCHAIN_MODULE, "Security Level Exception: " + e.getMessage(), e);

      return ANY;
    }
  }

  @NonNull
  private String getDefaultServiceIfNull(@Nullable final String service) {
    return service == null ? EMPTY_STRING : service;
  }
  //endregion

  //region Nested declarations

  /** Interactive user questioning for biometric data providing. */
  private class InteractiveBiometric extends BiometricPrompt.AuthenticationCallback implements DecryptionResultHandler {
    private DecryptionResult result;
    private Throwable error;
    private final CipherStorageBase storage;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private DecryptionContext context;

    private InteractiveBiometric(@NonNull final CipherStorage storage) {
      this.storage = (CipherStorageBase) storage;
    }

    @Override
    public void askAccessPermissions(@NonNull final DecryptionContext context) {
      this.context = context;

      if (!DeviceAvailability.isPermissionsGranted(getReactApplicationContext())) {
        final CryptoFailedException failure = new CryptoFailedException(
          "Could not start fingerprint Authentication. No permissions granted.");

        onDecrypt(null, failure);
      } else {
        startAuthentication();
      }
    }

    @Override
    public void onDecrypt(@Nullable final DecryptionResult decryptionResult, @Nullable final Throwable error) {
      this.result = decryptionResult;
      this.error = error;

      synchronized (this) {
        notifyAll();
      }
    }

    @Nullable
    @Override
    public DecryptionResult getResult() {
      return result;
    }

    @Nullable
    @Override
    public Throwable getError() {
      return error;
    }

    /** Called when an unrecoverable error has been encountered and the operation is complete. */
    @Override
    public void onAuthenticationError(final int errorCode, @NonNull final CharSequence errString) {
      final CryptoFailedException error = new CryptoFailedException("code: " + errorCode + ", msg: " + errString);

      onDecrypt(null, error);
    }

    /** Called when a biometric is recognized. */
    @Override
    public void onAuthenticationSucceeded(@NonNull final BiometricPrompt.AuthenticationResult result) {
      try {
        if (null == context) throw new NullPointerException("Decrypt context is not assigned yet.");

        final DecryptionResult decrypted = new DecryptionResult(
          storage.decryptBytes(context.key, context.username),
          storage.decryptBytes(context.key, context.password)
        );

        onDecrypt(decrypted, null);
      } catch (Throwable fail) {
        onDecrypt(null, fail);
      }
    }

    /** Called when a biometric is valid but not recognized. */
    @Override
    public void onAuthenticationFailed() {
      final CryptoFailedException error = new CryptoFailedException("Authentication failed. User Not recognized.");

      onDecrypt(null, error);
    }

    /** trigger interactive authentication. */
    public void startAuthentication() {
      final FragmentActivity activity = (FragmentActivity) getCurrentActivity();
      if (null == activity) throw new NullPointerException("Not assigned current activity");

      // code can be executed only from MAIN thread
      if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
        activity.runOnUiThread(this::startAuthentication);
        waitResult();
        return;
      }

      final BiometricPrompt prompt = new BiometricPrompt(activity, executor, this);
      final BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
        .setTitle("Authentication required")
        .setNegativeButtonText("Cancel")
        .setSubtitle("Please use biometric authentication to unlock the app")
        .build();

      prompt.authenticate(info);
    }

    /** Block current NON-main thread and wait for user authentication results. */
    @Override
    public void waitResult() {
      if (Thread.currentThread() == Looper.getMainLooper().getThread())
        throw new AssertionException("method should not be executed from MAIN thread");

      Log.i(KEYCHAIN_MODULE, "blocking thread. waiting for done UI operation.");

      try {
        synchronized (this) {
          wait();
        }
      } catch (InterruptedException ignored) {
        /* shutdown sequence */
      }

      Log.i(KEYCHAIN_MODULE, "unblocking thread.");
    }
  }
  //endregion
}
