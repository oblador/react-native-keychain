package com.oblador.keychain;

import android.app.Activity;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringDef;
import androidx.biometric.BiometricConstants;
import androidx.biometric.BiometricPrompt;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt.PromptInfo;
import androidx.fragment.app.FragmentActivity;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.AssertionException;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.oblador.keychain.utils.AppConstants;
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

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import android.security.KeyPairGeneratorSpec;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.security.auth.x500.X500Principal;

@SuppressWarnings({"unused", "WeakerAccess", "SameParameterValue"})
public class KeychainModule extends ReactContextBaseJavaModule {
  //region Constants
  public static final String KEYCHAIN_MODULE = "RNKeychainManager";
  public static final String FINGERPRINT_SUPPORTED_NAME = "Fingerprint";
  public static final String FACE_SUPPORTED_NAME = "Face";
  public static final String IRIS_SUPPORTED_NAME = "Iris";
  public static final String EMPTY_STRING = "";

  private static final String DELIMITER = "]";
  private static final String AES_GCM = "AES/GCM/NoPadding";
  private static final String RSA_ECB = "RSA/ECB/PKCS1Padding";
  private static final byte[] FIXED_IV = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1};
  private static final String KEY_ALIAS = "MySharedPreferenceKeyAlias";
  private static final String KEY_ALIAS_AES = "MyAesKeyAlias";
  private static final String AES_DEFAULT_TRANSFORMATION =
          KeyProperties.KEY_ALGORITHM_AES + "/" +
                  KeyProperties.BLOCK_MODE_CBC + "/" +
                  KeyProperties.ENCRYPTION_PADDING_PKCS7;
  private static final String ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore";
  private static final String LOG_TAG = KeychainModule.class.getSimpleName();
  private boolean invalidateEnrollment = true;

  private KeyStore mKeyStore;
  private CancellationSignal mCancellationSignal;
  private FingerprintManager mFingerprintManager;

  @RequiresApi(api = Build.VERSION_CODES.M)
  private void initFingerprintKeyStore() {
    try {
      // Check if a generated key exists under the KEY_ALIAS_AES .
      if (!mKeyStore.containsAlias(KEY_ALIAS_AES)) {
        prepareKey();
      }
    } catch (Exception e) {
      //
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
  private void initKeyStore() {
    try {
      if (!mKeyStore.containsAlias(KEY_ALIAS)) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE_PROVIDER);
          keyGenerator.init(
                  new KeyGenParameterSpec.Builder(KEY_ALIAS,
                          KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                          .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                          .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                          .setRandomizedEncryptionRequired(false)
                          .build());
          keyGenerator.generateKey();
        } else {
          Calendar notBefore = Calendar.getInstance();
          Calendar notAfter = Calendar.getInstance();
          notAfter.add(Calendar.YEAR, 10);
          KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(getReactApplicationContext())
                  .setAlias(KEY_ALIAS)
                  .setSubject(new X500Principal("CN=" + KEY_ALIAS))
                  .setSerialNumber(BigInteger.valueOf(1337))
                  .setStartDate(notBefore.getTime())
                  .setEndDate(notAfter.getTime())
                  .build();
          KeyPairGenerator kpGenerator = KeyPairGenerator.getInstance("RSA", ANDROID_KEYSTORE_PROVIDER);
          kpGenerator.initialize(spec);
          kpGenerator.generateKeyPair();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @StringDef({AccessControl.NONE
    , AccessControl.USER_PRESENCE
    , AccessControl.BIOMETRY_ANY
    , AccessControl.BIOMETRY_CURRENT_SET
    , AccessControl.DEVICE_PASSCODE
    , AccessControl.APPLICATION_PASSWORD
    , AccessControl.BIOMETRY_ANY_OR_DEVICE_PASSCODE
    , AccessControl.BIOMETRY_CURRENT_SET_OR_DEVICE_PASSCODE})
  @interface AccessControl {
    String NONE = "None";
    String USER_PRESENCE = "UserPresence";
    String BIOMETRY_ANY = "BiometryAny";
    String BIOMETRY_CURRENT_SET = "BiometryCurrentSet";
    String DEVICE_PASSCODE = "DevicePasscode";
    String APPLICATION_PASSWORD = "ApplicationPassword";
    String BIOMETRY_ANY_OR_DEVICE_PASSCODE = "BiometryAnyOrDevicePasscode";
    String BIOMETRY_CURRENT_SET_OR_DEVICE_PASSCODE = "BiometryCurrentSetOrDevicePasscode";
  }

  @interface AuthPromptOptions {
    String TITLE = "title";
    String SUBTITLE = "subtitle";
    String DESCRIPTION = "description";
    String CANCEL = "cancel";
  }

  /** Options mapping keys. */
  @interface Maps {
    String ACCESS_CONTROL = "accessControl";
    String ACCESS_GROUP = "accessGroup";
    String ACCESSIBLE = "accessible";
    String AUTH_PROMPT = "authenticationPrompt";
    String AUTH_TYPE = "authenticationType";
    String SERVICE = "service";
    String SECURITY_LEVEL = "securityLevel";
    String RULES = "rules";

    String USERNAME = "username";
    String PASSWORD = "password";
    String STORAGE = "storage";
  }

  /** Known error codes. */
  @interface Errors {
    String E_EMPTY_PARAMETERS = "E_EMPTY_PARAMETERS";
    String E_CRYPTO_FAILED = "E_CRYPTO_FAILED";
    String E_KEYSTORE_ACCESS_ERROR = "E_KEYSTORE_ACCESS_ERROR";
    String E_SUPPORTED_BIOMETRY_ERROR = "E_SUPPORTED_BIOMETRY_ERROR";
    /** Raised for unexpected errors. */
    String E_UNKNOWN_ERROR = "E_UNKNOWN_ERROR";
  }

  /** Supported ciphers. */
  @StringDef({KnownCiphers.FB, KnownCiphers.AES, KnownCiphers.RSA})
  public @interface KnownCiphers {
    /** Facebook conceal compatibility lib in use. */
    String FB = "FacebookConceal";
    /** AES encryption. */
    String AES = "KeystoreAESCBC";
    /** Biometric + RSA. */
    String RSA = "KeystoreRSAECB";
  }

  /** Secret manipulation rules. */
  @StringDef({Rules.AUTOMATIC_UPGRADE, Rules.NONE})
  @interface Rules {
    String NONE = "none";
    String AUTOMATIC_UPGRADE = "automaticUpgradeToMoreSecuredStorage";
  }
  //endregion

  //region Members
  /** Name-to-instance lookup  map. */
  private final Map<String, CipherStorage> cipherStorageMap = new HashMap<>();
  /** Shared preferences storage. */
  private final PrefsStorage prefsStorage;
  //endregion

  //region Initialization

  /** Default constructor. */
  public KeychainModule(@NonNull final ReactApplicationContext reactContext) {
    super(reactContext);

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
      Exception cause = new RuntimeException("Keystore is not supported!");
      throw new RuntimeException("Android version is too low", cause);
    }

    try {
      mKeyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER);
      mKeyStore.load(null);
    } catch (Exception e) {
      e.printStackTrace();
    }

    initKeyStore();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      try {
        mFingerprintManager = (FingerprintManager) reactContext.getSystemService(Context.FINGERPRINT_SERVICE);
        initFingerprintKeyStore();
      } catch (Exception e) {
        Log.d("RNSensitiveInfo", "Fingerprint not supported");
      }
    }
    prefsStorage = new PrefsStorage(reactContext);

    addCipherStorageToMap(new CipherStorageFacebookConceal(reactContext));
    addCipherStorageToMap(new CipherStorageKeystoreAesCbc());

    // we have a references to newer api that will fail load of app classes in old androids OS
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      addCipherStorageToMap(new CipherStorageKeystoreRsaEcb());
    }
  }

  /** Allow initialization in chain. */
  public static KeychainModule withWarming(@NonNull final ReactApplicationContext reactContext) {
    final KeychainModule instance = new KeychainModule(reactContext);

    // force initialization of the crypto api in background thread
    final Thread warmingUp = new Thread(instance::internalWarmingBestCipher, "keychain-warming-up");
    warmingUp.setDaemon(true);
    warmingUp.start();

    return instance;
  }

  /** cipher (crypto api) warming up logic. force java load classes and intializations. */
  private void internalWarmingBestCipher() {
    try {
      final long startTime = System.nanoTime();

      Log.v(KEYCHAIN_MODULE, "warming up started at " + startTime);
      final CipherStorageBase best = (CipherStorageBase) getCipherStorageForCurrentAPILevel();
      final Cipher instance = best.getCachedInstance();
      final boolean isSecure = best.supportsSecureHardware();
      final SecurityLevel requiredLevel = isSecure ? SecurityLevel.SECURE_HARDWARE : SecurityLevel.SECURE_SOFTWARE;
      best.generateKeyAndStoreUnderAlias("warmingUp", requiredLevel);
      best.getKeyStoreAndLoad();

      Log.v(KEYCHAIN_MODULE, "warming up takes: " +
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) +
        " ms");
    } catch (Throwable ex) {
      Log.e(KEYCHAIN_MODULE, "warming up failed!", ex);
    }
  }
  //endregion

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

    constants.put(SecurityLevel.ANY.jsName(), SecurityLevel.ANY.name());
    constants.put(SecurityLevel.SECURE_SOFTWARE.jsName(), SecurityLevel.SECURE_SOFTWARE.name());
    constants.put(SecurityLevel.SECURE_HARDWARE.jsName(), SecurityLevel.SECURE_HARDWARE.name());

    return constants;
  }
  //endregion

  //region React Methods
  protected void setGenericPassword(@NonNull final String alias,
                                    @NonNull final String username,
                                    @NonNull final String password,
                                    @Nullable final ReadableMap options,
                                    @NonNull final Promise promise) {
    try {
      throwIfEmptyLoginPassword(username, password);

      final SecurityLevel level = getSecurityLevelOrDefault(options);
      final CipherStorage storage = getSelectedStorage(options);

      throwIfInsufficientLevel(storage, level);

      final EncryptionResult result = storage.encrypt(alias, username, password, level);
      prefsStorage.storeEncryptedEntry(alias, result);

      final WritableMap results = Arguments.createMap();
      results.putString(Maps.SERVICE, alias);
      results.putString(Maps.STORAGE, storage.getCipherStorageName());

      promise.resolve(results);
    } catch (EmptyParameterException e) {
      Log.e(KEYCHAIN_MODULE, e.getMessage(), e);

      promise.reject(Errors.E_EMPTY_PARAMETERS, e);
    } catch (CryptoFailedException e) {
      Log.e(KEYCHAIN_MODULE, e.getMessage(), e);

      promise.reject(Errors.E_CRYPTO_FAILED, e);
    } catch (Throwable fail) {
      Log.e(KEYCHAIN_MODULE, fail.getMessage(), fail);

      promise.reject(Errors.E_UNKNOWN_ERROR, fail);
    }
  }

  @ReactMethod
  public void setGenericPasswordForOptions(@Nullable final ReadableMap options,
                                           @NonNull final String username,
                                           @NonNull final String password,
                                           @NonNull final Promise promise) {
    final String service = getServiceOrDefault(options);
    setGenericPassword(service, username, password, options, promise);
  }

  /** Get Cipher storage instance based on user provided options. */
  @NonNull
  private CipherStorage getSelectedStorage(@Nullable final ReadableMap options)
    throws CryptoFailedException {
    final String accessControl = getAccessControlOrDefault(options);
    final boolean useBiometry = getUseBiometry(accessControl);
    final String cipherName = getSpecificStorageOrDefault(options);

    CipherStorage result = null;

    if (null != cipherName) {
      result = getCipherStorageByName(cipherName);
    }

    // attempt to access none existing storage will force fallback logic.
    if (null == result) {
      result = getCipherStorageForCurrentAPILevel(useBiometry);
    }

    return result;
  }

  protected void getGenericPassword(@NonNull final String alias,
                                    @Nullable final ReadableMap options,
                                    @NonNull final Promise promise) {
    try {
      final ResultSet resultSet = prefsStorage.getEncryptedEntry(alias);

      if (resultSet == null) {
        Log.e(KEYCHAIN_MODULE, "No entry found for service: " + alias);
        promise.resolve(false);
        return;
      }

      // get the best storage
      final String accessControl = getAccessControlOrDefault(options);
      final boolean useBiometry = getUseBiometry(accessControl);
      final CipherStorage current = getCipherStorageForCurrentAPILevel(useBiometry);
      final String rules = getSecurityRulesOrDefault(options);

      final PromptInfo promptInfo = getPromptInfo(options);
      final DecryptionResult decryptionResult = decryptCredentials(alias, current, resultSet, rules, promptInfo);

      final WritableMap credentials = Arguments.createMap();
      credentials.putString(Maps.SERVICE, alias);
      credentials.putString(Maps.USERNAME, decryptionResult.username);
      credentials.putString(Maps.PASSWORD, decryptionResult.password);
      credentials.putString(Maps.STORAGE, current.getCipherStorageName());

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

  private boolean hasSetupBiometricCredential() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        ReactApplicationContext reactApplicationContext = getReactApplicationContext();
        BiometricManager biometricManager = BiometricManager.from(reactApplicationContext);
        int canAuthenticate = biometricManager.canAuthenticate();

        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS;
      } else {
        return false;
      }
    } catch (Exception e) {
      return false;
    }
  }

  private void showDialog(final HashMap strings, final BiometricPrompt.CryptoObject cryptoObject, final BiometricPrompt.AuthenticationCallback callback) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

      UiThreadUtil.runOnUiThread(
              new Runnable() {
                @Override
                public void run() {
                  try {
                    Activity activity = getCurrentActivity();
                    if (activity == null) {
                      callback.onAuthenticationError(BiometricConstants.ERROR_CANCELED,
                              strings.containsKey("cancelled") ? strings.get("cancelled").toString() : "Authentication was cancelled");
                      return;
                    }

                    FragmentActivity fragmentActivity = (FragmentActivity) getCurrentActivity();
                    Executor executor = Executors.newSingleThreadExecutor();
                    BiometricPrompt biometricPrompt = new BiometricPrompt(fragmentActivity, executor, callback);

                    BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                            .setDeviceCredentialAllowed(false)
                            .setNegativeButtonText(strings.containsKey("cancel") ? strings.get("cancel").toString() : "Cancel")
                            .setDescription(strings.containsKey("description") ? strings.get("description").toString() : null)
                            .setTitle(strings.containsKey("header") ? strings.get("header").toString() : "Unlock with your fingerprint")
                            .build();
                    biometricPrompt.authenticate(promptInfo, cryptoObject);
                  } catch (Exception e) {
                    throw e;
                  }
                }
              }
      );
    }
  }

  private void decryptWithAes(final String encrypted, final boolean showModal, final HashMap strings, final Promise pm, Cipher cipher) {

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
            && hasSetupBiometricCredential()) {

      String[] inputs = encrypted.split(DELIMITER);
      if (inputs.length < 2) {
        pm.reject("DecryptionFailed", "DecryptionFailed");
      }

      try {
        byte[] iv = Base64.decode(inputs[0], Base64.DEFAULT);
        byte[] cipherBytes = Base64.decode(inputs[1], Base64.DEFAULT);

        if (cipher == null) {
          SecretKey secretKey = (SecretKey) mKeyStore.getKey(KEY_ALIAS_AES, null);
          cipher = Cipher.getInstance(AES_DEFAULT_TRANSFORMATION);
          cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

          SecretKeyFactory factory = SecretKeyFactory.getInstance(
                  secretKey.getAlgorithm(), ANDROID_KEYSTORE_PROVIDER);
          KeyInfo info = (KeyInfo) factory.getKeySpec(secretKey, KeyInfo.class);

          if (info.isUserAuthenticationRequired() &&
                  info.getUserAuthenticationValidityDurationSeconds() <= 0) {

            if (showModal) {
              class DecryptWithAesCallback extends BiometricPrompt.AuthenticationCallback {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    decryptWithAes(encrypted, true, strings, pm, result.getCryptoObject().getCipher());
                  }
                }

                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                  pm.reject(String.valueOf(errorCode), errString.toString());
                }

                @Override
                public void onAuthenticationFailed() {
                  getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                          .emit(AppConstants.E_AUTHENTICATION_NOT_RECOGNIZED, "Authentication not recognized.");
                }
              }

              showDialog(strings, new BiometricPrompt.CryptoObject(cipher), new DecryptWithAesCallback());
            } else {
              mCancellationSignal = new CancellationSignal();
              mFingerprintManager.authenticate(new FingerprintManager.CryptoObject(cipher), mCancellationSignal,
                      0, new FingerprintManager.AuthenticationCallback() {

                        @Override
                        public void onAuthenticationFailed() {
                          super.onAuthenticationFailed();
                          getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                  .emit(AppConstants.E_AUTHENTICATION_NOT_RECOGNIZED, "Fingerprint not recognized.");
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                          super.onAuthenticationError(errorCode, errString);
                          pm.reject(String.valueOf(errorCode), errString.toString());
                        }

                        @Override
                        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                          super.onAuthenticationHelp(helpCode, helpString);
                          getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                  .emit(AppConstants.FINGERPRINT_AUTHENTICATION_HELP, helpString.toString());
                        }

                        @Override
                        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                          super.onAuthenticationSucceeded(result);
                          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            decryptWithAes(encrypted, false, strings, pm, result.getCryptoObject().getCipher());
                          }
                        }
                      }, null);
            }
          }
          return;
        }
        byte[] decryptedBytes = cipher.doFinal(cipherBytes);
        pm.resolve(new String(decryptedBytes));
      } catch (InvalidKeyException | UnrecoverableKeyException e) {
        try {
          mKeyStore.deleteEntry(KEY_ALIAS_AES);
          prepareKey();
        } catch (Exception keyResetError) {
          pm.reject(keyResetError);
        }
        pm.reject(e);
      } catch (IllegalBlockSizeException e){
        if(e.getCause() != null && e.getCause().getMessage().contains("Key user not authenticated")) {
          try {
            mKeyStore.deleteEntry(KEY_ALIAS_AES);
            prepareKey();
            pm.reject(AppConstants.KM_ERROR_KEY_USER_NOT_AUTHENTICATED, e.getCause().getMessage());
          } catch (Exception keyResetError) {
            pm.reject(keyResetError);
          }
        } else {
          pm.reject(e);
        }
      } catch (BadPaddingException e){
        Log.d("RNSensitiveInfo", "Biometric key invalid");
        pm.reject(AppConstants.E_BIOMETRICS_INVALIDATED, e.getCause().getMessage());
      } catch (SecurityException e) {
        pm.reject(e);
      } catch (Exception e) {
        pm.reject(e);
      }
    } else {
      pm.reject(AppConstants.E_BIOMETRIC_NOT_SUPPORTED, "Biometrics not supported");
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  private void prepareKey() throws Exception {

    KeyGenerator keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE_PROVIDER);

    KeyGenParameterSpec.Builder builder = null;
    builder = new KeyGenParameterSpec.Builder(
            KEY_ALIAS_AES,
            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT);

    builder.setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setKeySize(256)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            // forces user authentication with fingerprint
            .setUserAuthenticationRequired(true);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      try {
        builder.setInvalidatedByBiometricEnrollment(invalidateEnrollment);
      } catch (Exception e) {
        Log.d("RNSensitiveInfo", "Error setting setInvalidatedByBiometricEnrollment: " + e.getMessage());
      }
    }

    keyGenerator.init(builder.build());
    keyGenerator.generateKey();
  }


  @NonNull
  private String sharedPreferences(ReadableMap options) {
    String name = options.hasKey("sharedPreferencesName") ? options.getString("sharedPreferencesName") : "shared_preferences";
    if (name == null) {
      name = "shared_preferences";
    }
    return name;
  }

  private SharedPreferences prefs(String name) {
    return getReactApplicationContext().getSharedPreferences(name, Context.MODE_PRIVATE);
  }

  private void putExtra(String key, String value, SharedPreferences mSharedPreferences) {
    SharedPreferences.Editor editor = mSharedPreferences.edit();
    editor.putString(key, value).apply();
  }

  private void putExtraWithAES(final String key, final String value, final SharedPreferences mSharedPreferences, final boolean showModal, final HashMap strings, final Promise pm, Cipher cipher) {

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && hasSetupBiometricCredential()) {
      try {
        if (cipher == null) {
          SecretKey secretKey = (SecretKey) mKeyStore.getKey(KEY_ALIAS_AES, null);
          cipher = Cipher.getInstance(AES_DEFAULT_TRANSFORMATION);
          cipher.init(Cipher.ENCRYPT_MODE, secretKey);

          // Retrieve information about the SecretKey from the KeyStore.
          SecretKeyFactory factory = SecretKeyFactory.getInstance(
                  secretKey.getAlgorithm(), ANDROID_KEYSTORE_PROVIDER);
          KeyInfo info = (KeyInfo) factory.getKeySpec(secretKey, KeyInfo.class);

          if (info.isUserAuthenticationRequired() &&
                  info.getUserAuthenticationValidityDurationSeconds() <= 0) {

            if (showModal) {
              class PutExtraWithAESCallback extends BiometricPrompt.AuthenticationCallback {
                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    putExtraWithAES(key, value, mSharedPreferences, true, strings, pm, result.getCryptoObject().getCipher());
                  }
                }

                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                  pm.reject(String.valueOf(errorCode), errString.toString());
                }

                @Override
                public void onAuthenticationFailed() {
                  getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                          .emit(AppConstants.E_AUTHENTICATION_NOT_RECOGNIZED, "Authentication not recognized.");
                }
              }

              showDialog(strings, new BiometricPrompt.CryptoObject(cipher), new PutExtraWithAESCallback());
            } else {
              mCancellationSignal = new CancellationSignal();
              mFingerprintManager.authenticate(new FingerprintManager.CryptoObject(cipher), mCancellationSignal,
                      0, new FingerprintManager.AuthenticationCallback() {

                        @Override
                        public void onAuthenticationFailed() {
                          super.onAuthenticationFailed();
                          getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                  .emit(AppConstants.E_AUTHENTICATION_NOT_RECOGNIZED, "Fingerprint not recognized.");
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                          super.onAuthenticationError(errorCode, errString);
                          pm.reject(String.valueOf(errorCode), errString.toString());
                        }

                        @Override
                        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                          super.onAuthenticationHelp(helpCode, helpString);
                          getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                  .emit(AppConstants.FINGERPRINT_AUTHENTICATION_HELP, helpString.toString());
                        }

                        @Override
                        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                          super.onAuthenticationSucceeded(result);
                          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            putExtraWithAES(key, value, mSharedPreferences, false, strings, pm, result.getCryptoObject().getCipher());
                          }
                        }
                      }, null);
            }
          }
          return;
        }

        byte[] encryptedBytes = cipher.doFinal(value.getBytes());

        // Encode the initialization vector (IV) and encryptedBytes to Base64.
        String base64IV = Base64.encodeToString(cipher.getIV(), Base64.DEFAULT);
        String base64Cipher = Base64.encodeToString(encryptedBytes, Base64.DEFAULT);

        String result = base64IV + DELIMITER + base64Cipher;

        putExtra(key, result, mSharedPreferences);
        pm.resolve(value);
      } catch (InvalidKeyException | UnrecoverableKeyException e) {
        try {
          mKeyStore.deleteEntry(KEY_ALIAS_AES);
          prepareKey();
        } catch (Exception keyResetError) {
          pm.reject(keyResetError);
        }
        pm.reject(e);
      } catch (IllegalBlockSizeException e){
        if(e.getCause() != null && e.getCause().getMessage().contains("Key user not authenticated")) {
          try {
            mKeyStore.deleteEntry(KEY_ALIAS_AES);
            prepareKey();
            pm.reject(AppConstants.KM_ERROR_KEY_USER_NOT_AUTHENTICATED, e.getCause().getMessage());
          } catch (Exception keyResetError) {
            pm.reject(keyResetError);
          }
        } else {
          pm.reject(e);
        }
      } catch (SecurityException e) {
        pm.reject(e);
      } catch (Exception e) {
        pm.reject(e);
      }
    } else {
      pm.reject(AppConstants.E_BIOMETRIC_NOT_SUPPORTED, "Biometrics not supported");
    }
  }


  @ReactMethod
  public void setItem(String key, String value, ReadableMap options, Promise pm) {

    String name = sharedPreferences(options);

    if (options.hasKey("touchID") && options.getBoolean("touchID")) {
      boolean showModal = options.hasKey("showModal") && options.getBoolean("showModal");
      HashMap strings = options.hasKey("strings") ? options.getMap("strings").toHashMap() : new HashMap();
      putExtraWithAES(key, value, prefs(name), showModal, strings, pm, null);
    } else {
      try {
        putExtra(key, encrypt(value), prefs(name));
        pm.resolve(value);
      } catch (Exception e) {
        e.printStackTrace();
        pm.reject(e);
      }
    }
  }

  @ReactMethod
  public void deleteItem(String key, ReadableMap options, Promise pm) {

    String name = sharedPreferences(options);

    SharedPreferences.Editor editor = prefs(name).edit();

    editor.remove(key).apply();

    pm.resolve(null);
  }

  public String encrypt(String input) throws Exception {
    byte[] bytes = input.getBytes();
    Cipher c;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Key secretKey = ((KeyStore.SecretKeyEntry) mKeyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
      c = Cipher.getInstance(AES_GCM);
      c.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, FIXED_IV));
    } else {
      PublicKey publicKey = ((KeyStore.PrivateKeyEntry)mKeyStore.getEntry(KEY_ALIAS, null)).getCertificate().getPublicKey();
      c = Cipher.getInstance(RSA_ECB);
      c.init(Cipher.ENCRYPT_MODE, publicKey);
    }
    byte[] encodedBytes = c.doFinal(bytes);
    String encryptedBase64Encoded = Base64.encodeToString(encodedBytes, Base64.DEFAULT);
    return encryptedBase64Encoded;
  }

  @ReactMethod
  public void getItem(String key, ReadableMap options, Promise pm) {

    String name = sharedPreferences(options);

    String value = prefs(name).getString(key, null);

    if (value != null && options.hasKey("touchID") && options.getBoolean("touchID")) {
      boolean showModal = options.hasKey("showModal") && options.getBoolean("showModal");
      HashMap strings = options.hasKey("strings") ? options.getMap("strings").toHashMap() : new HashMap();
      decryptWithAes(value, showModal, strings, pm, null);
    } else if (value != null) {
      try {
        pm.resolve(decrypt(value));
      } catch (Exception e) {
        pm.reject(e);
      }
    } else {
      pm.resolve(value);
    }
  }

  public String decrypt(String encrypted) throws Exception {
    if (encrypted == null) {
      Exception cause = new RuntimeException("Invalid argument at decrypt function");
      throw new RuntimeException("encrypted argument can't be null", cause);
    }

    Cipher c;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Key secretKey = ((KeyStore.SecretKeyEntry) mKeyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
      c = Cipher.getInstance(AES_GCM);
      c.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, FIXED_IV));
    } else {
      PrivateKey privateKey = ((KeyStore.PrivateKeyEntry)mKeyStore.getEntry(KEY_ALIAS, null)).getPrivateKey();
      c = Cipher.getInstance(RSA_ECB);
      c.init(Cipher.DECRYPT_MODE, privateKey);
    }
    byte[] decodedBytes = c.doFinal(Base64.decode(encrypted, Base64.DEFAULT));
    return new String(decodedBytes);
  }

  @ReactMethod
  public void getGenericPasswordForOptions(@Nullable final ReadableMap options,
                                           @NonNull final Promise promise) {
    final String service = getServiceOrDefault(options);
    getGenericPassword(service, options, promise);
  }

  protected void resetGenericPassword(@NonNull final String alias,
                                      @NonNull final Promise promise) {
    try {
      // First we clean up the cipher storage (using the cipher storage that was used to store the entry)
      final ResultSet resultSet = prefsStorage.getEncryptedEntry(alias);

      if (resultSet != null) {
        final CipherStorage cipherStorage = getCipherStorageByName(resultSet.cipherStorageName);

        if (cipherStorage != null) {
          cipherStorage.removeKey(alias);
        }
      }
      // And then we remove the entry in the shared preferences
      prefsStorage.removeEntry(alias);

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
  public void resetGenericPasswordForOptions(@Nullable final ReadableMap options,
                                             @NonNull final Promise promise) {
    final String service = getServiceOrDefault(options);
    resetGenericPassword(service, promise);
  }

  @ReactMethod
  public void hasInternetCredentialsForServer(@NonNull final String server,
                                              @NonNull final Promise promise) {
    final String alias = getAliasOrDefault(server);

    final ResultSet resultSet = prefsStorage.getEncryptedEntry(alias);

    if (resultSet == null) {
      Log.e(KEYCHAIN_MODULE, "No entry found for service: " + alias);
      promise.resolve(false);
      return;
    }

    final WritableMap results = Arguments.createMap();
    results.putString(Maps.SERVICE, alias);
    results.putString(Maps.STORAGE, resultSet.cipherStorageName);

    promise.resolve(results);
  }

  @ReactMethod
  public void setInternetCredentialsForServer(@NonNull final String server,
                                              @NonNull final String username,
                                              @NonNull final String password,
                                              @Nullable final ReadableMap options,
                                              @NonNull final Promise promise) {
    setGenericPassword(server, username, password, options, promise);
  }

  @ReactMethod
  public void getInternetCredentialsForServer(@NonNull final String server,
                                              @Nullable final ReadableMap options,
                                              @NonNull final Promise promise) {
    getGenericPassword(server, options, promise);
  }

  @ReactMethod
  public void resetInternetCredentialsForServer(@NonNull final String server,
                                                @NonNull final Promise promise) {
    resetGenericPassword(server, promise);
  }

  @ReactMethod
  public void getSupportedBiometryType(@NonNull final Promise promise) {
    try {
      String reply = null;
      if(isFaceAuthAvailable()){
        reply = FACE_SUPPORTED_NAME;
      } else if(isIrisAuthAvailable()) {
        reply = IRIS_SUPPORTED_NAME;
      } else if(isFingerprintAuthAvailable()) {
        reply = FINGERPRINT_SUPPORTED_NAME;
      }

      promise.resolve(reply);
    } catch (Exception e) {
      Log.e(KEYCHAIN_MODULE, e.getMessage(), e);

      promise.reject(Errors.E_SUPPORTED_BIOMETRY_ERROR, e);
    } catch (Throwable fail) {
      Log.e(KEYCHAIN_MODULE, fail.getMessage(), fail);

      promise.reject(Errors.E_UNKNOWN_ERROR, fail);
    }
  }

  @ReactMethod
  public void getSecurityLevel(@Nullable final ReadableMap options,
                               @NonNull final Promise promise) {
    // DONE (olku): if forced biometry than we should return security level = HARDWARE if it supported
    final String accessControl = getAccessControlOrDefault(options);
    final boolean useBiometry = getUseBiometry(accessControl);

    promise.resolve(getSecurityLevel(useBiometry).name());
  }
  //endregion

  //region Helpers

  /** Get service value from options. */
  @NonNull
  private static String getServiceOrDefault(@Nullable final ReadableMap options) {
    String service = null;

    if (null != options && options.hasKey(Maps.SERVICE)) {
      service = options.getString(Maps.SERVICE);
    }

    return getAliasOrDefault(service);
  }

  /** Get automatic secret manipulation rules, default: Automatic Upgrade. */
  @Rules
  @NonNull
  private static String getSecurityRulesOrDefault(@Nullable final ReadableMap options) {
    return getSecurityRulesOrDefault(options, Rules.AUTOMATIC_UPGRADE);
  }

  /** Get automatic secret manipulation rules. */
  @Rules
  @NonNull
  private static String getSecurityRulesOrDefault(@Nullable final ReadableMap options,
                                                  @Rules @NonNull final String rule) {
    String rules = null;

    if (null != options && options.hasKey(Maps.RULES)) {
      rules = options.getString(Maps.ACCESS_CONTROL);
    }

    if (null == rules) return rule;

    return rules;
  }

  /** Extract user specified storage from options. */
  @KnownCiphers
  @Nullable
  private static String getSpecificStorageOrDefault(@Nullable final ReadableMap options) {
    String storageName = null;

    if (null != options && options.hasKey(Maps.STORAGE)) {
      storageName = options.getString(Maps.STORAGE);
    }

    return storageName;
  }

  /** Get access control value from options or fallback to {@link AccessControl#NONE}. */
  @AccessControl
  @NonNull
  private static String getAccessControlOrDefault(@Nullable final ReadableMap options) {
    return getAccessControlOrDefault(options, AccessControl.NONE);
  }

  /** Get access control value from options or fallback to default. */
  @AccessControl
  @NonNull
  private static String getAccessControlOrDefault(@Nullable final ReadableMap options,
                                                  @AccessControl @NonNull final String fallback) {
    String accessControl = null;

    if (null != options && options.hasKey(Maps.ACCESS_CONTROL)) {
      accessControl = options.getString(Maps.ACCESS_CONTROL);
    }

    if (null == accessControl) return fallback;

    return accessControl;
  }


  /** Get security level from options or fallback {@link SecurityLevel#ANY} value. */
  @NonNull
  private static SecurityLevel getSecurityLevelOrDefault(@Nullable final ReadableMap options) {
    return getSecurityLevelOrDefault(options, SecurityLevel.ANY.name());
  }

  /** Get security level from options or fallback to default value. */
  @NonNull
  private static SecurityLevel getSecurityLevelOrDefault(@Nullable final ReadableMap options,
                                                         @NonNull final String fallback) {
    String minimalSecurityLevel = null;

    if (null != options && options.hasKey(Maps.SECURITY_LEVEL)) {
      minimalSecurityLevel = options.getString(Maps.SECURITY_LEVEL);
    }

    if (null == minimalSecurityLevel) minimalSecurityLevel = fallback;

    return SecurityLevel.valueOf(minimalSecurityLevel);
  }
  //endregion

  //region Implementation

  /** Is provided access control string matching biometry use request? */
  public static boolean getUseBiometry(@AccessControl @Nullable final String accessControl) {
    return AccessControl.BIOMETRY_ANY.equals(accessControl)
      || AccessControl.BIOMETRY_CURRENT_SET.equals(accessControl)
      || AccessControl.BIOMETRY_ANY_OR_DEVICE_PASSCODE.equals(accessControl)
      || AccessControl.BIOMETRY_CURRENT_SET_OR_DEVICE_PASSCODE.equals(accessControl);
  }

  private void addCipherStorageToMap(@NonNull final CipherStorage cipherStorage) {
    cipherStorageMap.put(cipherStorage.getCipherStorageName(), cipherStorage);
  }

  /** Extract user specified prompt info from options. */
  @NonNull
  private static PromptInfo getPromptInfo(@Nullable final ReadableMap options) {
    final ReadableMap promptInfoOptionsMap = (options != null && options.hasKey(Maps.AUTH_PROMPT)) ? options.getMap(Maps.AUTH_PROMPT) : null;

    final PromptInfo.Builder promptInfoBuilder = new PromptInfo.Builder();
    if (null != promptInfoOptionsMap && promptInfoOptionsMap.hasKey(AuthPromptOptions.TITLE)) {
      String promptInfoTitle = promptInfoOptionsMap.getString(AuthPromptOptions.TITLE);
      promptInfoBuilder.setTitle(promptInfoTitle);
    }
    if (null != promptInfoOptionsMap && promptInfoOptionsMap.hasKey(AuthPromptOptions.SUBTITLE)) {
      String promptInfoSubtitle = promptInfoOptionsMap.getString(AuthPromptOptions.SUBTITLE);
      promptInfoBuilder.setSubtitle(promptInfoSubtitle);
    }
    if (null != promptInfoOptionsMap && promptInfoOptionsMap.hasKey(AuthPromptOptions.DESCRIPTION)) {
      String promptInfoDescription = promptInfoOptionsMap.getString(AuthPromptOptions.DESCRIPTION);
      promptInfoBuilder.setDescription(promptInfoDescription);
    }
    if (null != promptInfoOptionsMap && promptInfoOptionsMap.hasKey(AuthPromptOptions.CANCEL)) {
      String promptInfoNegativeButton = promptInfoOptionsMap.getString(AuthPromptOptions.CANCEL);
      promptInfoBuilder.setNegativeButtonText(promptInfoNegativeButton);
    }
    final PromptInfo promptInfo = promptInfoBuilder.build();

    return promptInfo;
  }

  /**
   * Extract credentials from current storage. In case if current storage is not matching
   * results set then executed migration.
   */
  @NonNull
  private DecryptionResult decryptCredentials(@NonNull final String alias,
                                              @NonNull final CipherStorage current,
                                              @NonNull final ResultSet resultSet,
                                              @Rules @NonNull final String rules,
                                              @NonNull final PromptInfo promptInfo)
    throws CryptoFailedException, KeyStoreAccessException {
    final String storageName = resultSet.cipherStorageName;

    // The encrypted data is encrypted using the current CipherStorage, so we just decrypt and return
    if (storageName.equals(current.getCipherStorageName())) {
      return decryptToResult(alias, current, resultSet, promptInfo);
    }

    // The encrypted data is encrypted using an older CipherStorage, so we need to decrypt the data first,
    // then encrypt it using the current CipherStorage, then store it again and return
    final CipherStorage oldStorage = getCipherStorageByName(storageName);
    if (null == oldStorage) {
      throw new KeyStoreAccessException("Wrong cipher storage name '" + storageName + "' or cipher not available");
    }

    // decrypt using the older cipher storage
    final DecryptionResult decryptionResult = decryptToResult(alias, oldStorage, resultSet, promptInfo);

    if (Rules.AUTOMATIC_UPGRADE.equals(rules)) {
      try {
        // encrypt using the current cipher storage
        migrateCipherStorage(alias, current, oldStorage, decryptionResult);
      } catch (CryptoFailedException e) {
        Log.w(KEYCHAIN_MODULE, "Migrating to a less safe storage is not allowed. Keeping the old one");
      }
    }

    return decryptionResult;
  }

  /** Try to decrypt with provided storage. */
  @NonNull
  private DecryptionResult decryptToResult(@NonNull final String alias,
                                           @NonNull final CipherStorage storage,
                                           @NonNull final ResultSet resultSet,
                                           @NonNull final PromptInfo promptInfo)
    throws CryptoFailedException {
    final DecryptionResultHandler handler = getInteractiveHandler(storage, promptInfo);
    storage.decrypt(handler, alias, resultSet.username, resultSet.password, SecurityLevel.ANY);

    CryptoFailedException.reThrowOnError(handler.getError());

    if (null == handler.getResult()) {
      throw new CryptoFailedException("No decryption results and no error. Something deeply wrong!");
    }

    return handler.getResult();
  }

  /** Get instance of handler that resolves access to the keystore on system request. */
  @NonNull
  protected DecryptionResultHandler getInteractiveHandler(@NonNull final CipherStorage current, @NonNull final PromptInfo promptInfo) {
    if (current.isBiometrySupported() /*&& isFingerprintAuthAvailable()*/) {
      return new InteractiveBiometric(current, promptInfo);
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
    return getCipherStorageForCurrentAPILevel(true);
  }

  /**
   * The "Current" CipherStorage is the cipherStorage with the highest API level that is
   * lower than or equal to the current API level. Parameter allow to reduce level.
   */
  @NonNull
  /* package */ CipherStorage getCipherStorageForCurrentAPILevel(final boolean useBiometry)
    throws CryptoFailedException {
    final int currentApiLevel = Build.VERSION.SDK_INT;
    final boolean isBiometry = (isFingerprintAuthAvailable() || isFaceAuthAvailable() || isIrisAuthAvailable()) && useBiometry;
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
      if (variant.isBiometrySupported() && !isBiometry) continue;

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

  /** Extract cipher by it unique name. {@link CipherStorage#getCipherStorageName()}. */
  @Nullable
  /* package */ CipherStorage getCipherStorageByName(@KnownCiphers @NonNull final String knownName) {
    return cipherStorageMap.get(knownName);
  }

  /** True - if fingerprint hardware available and configured, otherwise false. */
  /* package */ boolean isFingerprintAuthAvailable() {
    return DeviceAvailability.isBiometricAuthAvailable(getReactApplicationContext()) && DeviceAvailability.isFingerprintAuthAvailable(getReactApplicationContext());
  }

  /** True - if face recognition hardware available and configured, otherwise false. */
  /* package */ boolean isFaceAuthAvailable() {
    return DeviceAvailability.isBiometricAuthAvailable(getReactApplicationContext()) && DeviceAvailability.isFaceAuthAvailable(getReactApplicationContext());
  }

  /** True - if iris recognition hardware available and configured, otherwise false. */
  /* package */ boolean isIrisAuthAvailable() {
    return DeviceAvailability.isBiometricAuthAvailable(getReactApplicationContext()) && DeviceAvailability.isIrisAuthAvailable(getReactApplicationContext());
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
  private SecurityLevel getSecurityLevel(final boolean useBiometry) {
    try {
      final CipherStorage storage = getCipherStorageForCurrentAPILevel(useBiometry);

      if (!storage.securityLevel().satisfiesSafetyThreshold(SecurityLevel.SECURE_SOFTWARE)) {
        return SecurityLevel.ANY;
      }

      if (storage.supportsSecureHardware()) {
        return SecurityLevel.SECURE_HARDWARE;
      }

      return SecurityLevel.SECURE_SOFTWARE;
    } catch (CryptoFailedException e) {
      Log.w(KEYCHAIN_MODULE, "Security Level Exception: " + e.getMessage(), e);

      return SecurityLevel.ANY;
    }
  }

  @NonNull
  private static String getAliasOrDefault(@Nullable final String service) {
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
    private PromptInfo promptInfo;

    private InteractiveBiometric(@NonNull final CipherStorage storage, @NonNull final PromptInfo promptInfo) {
      this.storage = (CipherStorageBase) storage;
      this.promptInfo = promptInfo;
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

      prompt.authenticate(this.promptInfo);
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
