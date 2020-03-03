package com.oblador.keychain;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.test.core.app.ApplicationProvider;

import com.facebook.react.bridge.JavaOnlyMap;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.oblador.keychain.KeychainModule.AccessControl;
import com.oblador.keychain.KeychainModule.Errors;
import com.oblador.keychain.KeychainModule.KnownCiphers;
import com.oblador.keychain.KeychainModule.Maps;
import com.oblador.keychain.cipherStorage.CipherStorage;
import com.oblador.keychain.cipherStorage.CipherStorageBase;
import com.oblador.keychain.cipherStorage.CipherStorageFacebookConceal;
import com.oblador.keychain.cipherStorage.CipherStorageKeystoreAesCbc;
import com.oblador.keychain.cipherStorage.CipherStorageKeystoreRsaEcb;
import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.VerificationCollector;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.security.KeyStore;
import java.security.Security;

import javax.crypto.Cipher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class KeychainModuleTests {
  public static final byte[] BYTES_USERNAME = "username".getBytes();
  public static final byte[] BYTES_PASSWORD = "password".getBytes();
  /**
   * Cancel test after 5 seconds.
   */
  @ClassRule
  public static Timeout timeout = Timeout.seconds(10);
  /**
   * Get test method name.
   */
  @Rule
  public TestName methodName = new TestName();
  /**
   * Mock all the dependencies.
   */
  @Rule
  public MockitoRule mockDependencies = MockitoJUnit.rule().silent();
  @Rule
  public VerificationCollector collector = MockitoJUnit.collector();
  /**
   * Security fake provider.
   */
  private FakeProvider provider = new FakeProvider();

  @Before
  public void setUp() throws Exception {
    provider.configuration.clear();

    Security.insertProviderAt(provider, 0);
  }

  @After
  public void tearDown() throws Exception {
    Security.removeProvider(FakeProvider.NAME);
  }

  @NonNull
  private ReactApplicationContext getRNContext() {
    return new ReactApplicationContext(ApplicationProvider.getApplicationContext());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
  public void testFingerprintNoHardware_api21() throws Exception {
    // GIVEN: API21 android version
    ReactApplicationContext context = getRNContext();
    KeychainModule module = new KeychainModule(context);

    // WHEN: verify availability
    final int result = BiometricManager.from(context).canAuthenticate();
    final boolean isFingerprintAvailable = module.isFingerprintAuthAvailable();

    // THEN: in api lower 23 - biometric is not available at all
    assertThat(isFingerprintAvailable, is(false));
    assertThat(result, is(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE));

    // fingerprint hardware not available, minimal API for fingerprint is api23, Android 6.0
    // https://developer.android.com/about/versions/marshmallow/android-6.0
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testFingerprintAvailableButNotConfigured_api23() throws Exception {
    // GIVEN:
    //   fingerprint api available but not configured properly
    //   API23 android version
    ReactApplicationContext context = getRNContext();
    KeychainModule module = new KeychainModule(context);

    // set that hardware is available
    FingerprintManager fm = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
    shadowOf(fm).setIsHardwareDetected(true);

    // WHEN: check availability
    final int result = BiometricManager.from(context).canAuthenticate();
    final boolean isFingerprintWorking = module.isFingerprintAuthAvailable();

    // THEN: another status from biometric api, fingerprint is still unavailable
    assertThat(result, is(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED));
    assertThat(isFingerprintWorking, is(false));
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testFingerprintConfigured_api23() throws Exception {
    // GIVEN:
    //   API23 android version
    //   Fingerprints are configured
    //   fingerprint feature is ignored by android os
    ReactApplicationContext context = getRNContext();

    // set that hardware is available
    FingerprintManager fm = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
    shadowOf(fm).setIsHardwareDetected(true);
    shadowOf(fm).setDefaultFingerprints(5); // 5 fingerprints are available

    // WHEN: check availability
    final int result = BiometricManager.from(context).canAuthenticate();
    final KeychainModule module = new KeychainModule(context);
    final boolean isFingerprintWorking = module.isFingerprintAuthAvailable();

    // THEN: biometric works
    assertThat(result, is(BiometricManager.BIOMETRIC_SUCCESS));
    assertThat(isFingerprintWorking, is(true));
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.P)
  public void testFingerprintConfigured_api28() throws Exception {
    // GIVEN:
    //   API28 android version
    //   for api24+ system feature should be enabled
    //   fingerprints are configured
    ReactApplicationContext context = getRNContext();
    shadowOf(context.getPackageManager()).setSystemFeature(PackageManager.FEATURE_FINGERPRINT, true);

    // set that hardware is available
    FingerprintManager fm = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
    shadowOf(fm).setIsHardwareDetected(true);
    shadowOf(fm).setDefaultFingerprints(5); // 5 fingerprints are available

    // WHEN: verify availability
    final int result = BiometricManager.from(context).canAuthenticate();
    final KeychainModule module = new KeychainModule(context);
    final boolean isFingerprintWorking = module.isFingerprintAuthAvailable();

    // THEN: biometrics works
    assertThat(result, is(BiometricManager.BIOMETRIC_SUCCESS));
    assertThat(isFingerprintWorking, is(true));
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.KITKAT)
  public void testExtractFacebookConceal_NoHardware_api19() throws Exception {
    // GIVEN:
    //  API19, minimal Android version
    final ReactApplicationContext context = getRNContext();

    // WHEN: ask keychain for secured storage
    final KeychainModule module = new KeychainModule(context);
    final CipherStorage storage = module.getCipherStorageForCurrentAPILevel();

    // THEN: expected Facebook cipher storage, its the only one that supports API19
    assertThat(storage, notNullValue());
    assertThat(storage, instanceOf(CipherStorageFacebookConceal.class));
    assertThat(storage.isBiometrySupported(), is(false));
    assertThat(storage.securityLevel(), is(SecurityLevel.ANY));
    assertThat(storage.getMinSupportedApiLevel(), is(Build.VERSION_CODES.JELLY_BEAN));
    assertThat(storage.supportsSecureHardware(), is(false));
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testExtractAesCbc_NoFingerprintConfigured_api23() throws Exception {
    // GIVEN:
    //  API23 android version
    final ReactApplicationContext context = getRNContext();

    // WHEN: get the best secured storage
    final KeychainModule module = new KeychainModule(context);
    final CipherStorage storage = module.getCipherStorageForCurrentAPILevel();

    // THEN:
    //   expected AES cipher storage due no fingerprint available
    //   AES win and returned instead of facebook cipher
    assertThat(storage, notNullValue());
    assertThat(storage, instanceOf(CipherStorageKeystoreAesCbc.class));
    assertThat(storage.isBiometrySupported(), is(false));
    assertThat(storage.securityLevel(), is(SecurityLevel.SECURE_HARDWARE));
    assertThat(storage.getMinSupportedApiLevel(), is(Build.VERSION_CODES.M));
    assertThat(storage.supportsSecureHardware(), is(true));
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testExtractRsaEcb_EnabledFingerprint_api23() throws Exception {
    // GIVEN:
    //   API23 android version
    //   fingerprints configured
    final ReactApplicationContext context = getRNContext();

    // set that hardware is available and fingerprints configured
    final FingerprintManager fm = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
    shadowOf(fm).setIsHardwareDetected(true);
    shadowOf(fm).setDefaultFingerprints(5); // 5 fingerprints are available

    // WHEN: fingerprint availability influence on storage selection
    final KeychainModule module = new KeychainModule(context);
    final boolean isFingerprintWorking = module.isFingerprintAuthAvailable();
    final CipherStorage storage = module.getCipherStorageForCurrentAPILevel();

    // THEN: expected RsaEcb with working fingerprint
    assertThat(isFingerprintWorking, is(true));
    assertThat(storage, notNullValue());
    assertThat(storage, instanceOf(CipherStorageKeystoreRsaEcb.class));
    assertThat(storage.isBiometrySupported(), is(true));
    assertThat(storage.securityLevel(), is(SecurityLevel.SECURE_HARDWARE));
    assertThat(storage.getMinSupportedApiLevel(), is(Build.VERSION_CODES.M));
    assertThat(storage.supportsSecureHardware(), is(true));
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.P)
  public void testExtractRsaEcb_EnabledFingerprint_api28() throws Exception {
    // GIVEN:
    //   API28 android version
    //   fingerprint feature enabled
    //   fingerprints configured
    final ReactApplicationContext context = getRNContext();
    shadowOf(context.getPackageManager()).setSystemFeature(PackageManager.FEATURE_FINGERPRINT, true);

    // set that hardware is available and fingerprints configured
    final FingerprintManager fm = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
    shadowOf(fm).setIsHardwareDetected(true);
    shadowOf(fm).setDefaultFingerprints(5); // 5 fingerprints are available

    // WHEN: get secured storage
    final int result = BiometricManager.from(context).canAuthenticate();
    final KeychainModule module = new KeychainModule(context);
    final boolean isFingerprintWorking = module.isFingerprintAuthAvailable();
    final CipherStorage storage = module.getCipherStorageForCurrentAPILevel();

    // THEN: expected RsaEcb with working fingerprint
    assertThat(isFingerprintWorking, is(true));
    assertThat(result, is(BiometricManager.BIOMETRIC_SUCCESS));
    assertThat(storage, notNullValue());
    assertThat(storage, instanceOf(CipherStorageKeystoreRsaEcb.class));
    assertThat(storage.isBiometrySupported(), is(true));
    assertThat(storage.securityLevel(), is(SecurityLevel.SECURE_HARDWARE));
    assertThat(storage.getMinSupportedApiLevel(), is(Build.VERSION_CODES.M));
    assertThat(storage.supportsSecureHardware(), is(true));
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testMigrateStorageFromOlder_api23() throws Exception {
    // GIVEN:
    final ReactApplicationContext context = getRNContext();
    final CipherStorage aes = Mockito.mock(CipherStorage.class);
    final CipherStorage rsa = Mockito.mock(CipherStorage.class);
    when(rsa.getCipherStorageName()).thenReturn("dummy");

    final CipherStorage.DecryptionResult decrypted = new CipherStorage.DecryptionResult("user", "password");
    final CipherStorage.EncryptionResult encrypted = new CipherStorage.EncryptionResult("user".getBytes(), "password".getBytes(), rsa);
    final KeychainModule module = new KeychainModule(context);
    final SharedPreferences prefs = context.getSharedPreferences(PrefsStorage.KEYCHAIN_DATA, Context.MODE_PRIVATE);

    when(
      rsa.encrypt(eq("dummy"), eq("user"), eq("password"), any())
    ).thenReturn(encrypted);

    // WHEN:
    module.migrateCipherStorage("dummy", rsa, aes, decrypted);
    final String username = prefs.getString(PrefsStorage.getKeyForUsername("dummy"), "");
    final String password = prefs.getString(PrefsStorage.getKeyForPassword("dummy"), "");
    final String cipherName = prefs.getString(PrefsStorage.getKeyForCipherStorage("dummy"), "");

    // THEN:
    //   delete of key from old storage
    //   re-store of encrypted data in shared preferences
    verify(rsa).encrypt("dummy", "user", "password", SecurityLevel.ANY);
    verify(aes).removeKey("dummy");

    // Base64.DEFAULT force '\n' char in the end of string
    assertThat(username, is("dXNlcg==\n"));
    assertThat(password, is("cGFzc3dvcmQ=\n"));
    assertThat(cipherName, is("dummy"));
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.P)
  public void testGetSecurityLevel_Unspecified_api28() throws Exception {
    // GIVE:
    final ReactApplicationContext context = getRNContext();
    final KeychainModule module = new KeychainModule(context);
    final Promise mockPromise = mock(Promise.class);

    // WHEN:
    module.getSecurityLevel(null, mockPromise);

    // THEN:
    verify(mockPromise).resolve(SecurityLevel.SECURE_HARDWARE.name());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.M)
  public void testGetSecurityLevel_Unspecified_api23() throws Exception {
    // GIVE:
    final ReactApplicationContext context = getRNContext();
    final KeychainModule module = new KeychainModule(context);
    final Promise mockPromise = mock(Promise.class);

    // WHEN:
    module.getSecurityLevel(null, mockPromise);

    // THEN:
    verify(mockPromise).resolve(SecurityLevel.SECURE_HARDWARE.name());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
  public void testGetSecurityLevel_Unspecified_api21() throws Exception {
    // GIVE:
    final ReactApplicationContext context = getRNContext();
    final KeychainModule module = new KeychainModule(context);
    final Promise mockPromise = mock(Promise.class);

    // WHEN:
    module.getSecurityLevel(null, mockPromise);

    // THEN:
    verify(mockPromise).resolve(SecurityLevel.ANY.name());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.KITKAT)
  public void testGetSecurityLevel_Unspecified_api19() throws Exception {
    // GIVE:
    final ReactApplicationContext context = getRNContext();
    final KeychainModule module = new KeychainModule(context);
    final Promise mockPromise = mock(Promise.class);

    // WHEN:
    module.getSecurityLevel(null, mockPromise);

    // THEN:
    verify(mockPromise).resolve(SecurityLevel.ANY.name());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.P)
  public void testGetSecurityLevel_NoBiometry_api28() throws Exception {
    // GIVE:
    final ReactApplicationContext context = getRNContext();
    final KeychainModule module = new KeychainModule(context);
    final Promise mockPromise = mock(Promise.class);

    // WHEN:
    final JavaOnlyMap options = new JavaOnlyMap();
    options.putString(Maps.ACCESS_CONTROL, AccessControl.DEVICE_PASSCODE);

    module.getSecurityLevel(options, mockPromise);

    // THEN:
    verify(mockPromise).resolve(SecurityLevel.SECURE_HARDWARE.name());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.P)
  public void testGetSecurityLevel_NoBiometry_NoSecuredHardware_api28() throws Exception {
    // GIVE:
    final ReactApplicationContext context = getRNContext();
    final KeychainModule module = new KeychainModule(context);
    final Promise mockPromise = mock(Promise.class);

    // set key info - software method
    provider.configuration.put("isInsideSecureHardware", false);

    // WHEN:
    final JavaOnlyMap options = new JavaOnlyMap();
    options.putString(Maps.ACCESS_CONTROL, AccessControl.DEVICE_PASSCODE);

    module.getSecurityLevel(options, mockPromise);

    // THEN:
    // expected AesCbc usage
    assertThat(provider.mocks.get("KeyGenerator"), notNullValue());
    assertThat(provider.mocks.get("KeyGenerator").get("AES"), notNullValue());
    assertThat(provider.mocks.get("KeyPairGenerator"), notNullValue());
    assertThat(provider.mocks.get("KeyPairGenerator").get("RSA"), notNullValue());
    verify(mockPromise).resolve(SecurityLevel.SECURE_SOFTWARE.name());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.P)
  public void testDowngradeBiometricToAes_api28() throws Exception {
    // GIVEN:
    final ReactApplicationContext context = getRNContext();
    final KeychainModule module = new KeychainModule(context);
    final PrefsStorage prefs = new PrefsStorage(context);
    final Cipher mockCipher = Mockito.mock(Cipher.class);
    final KeyStore mockKeyStore = Mockito.mock(KeyStore.class);
    final CipherStorage storage = module.getCipherStorageByName(KnownCiphers.RSA);
    final CipherStorage.EncryptionResult result = new CipherStorage.EncryptionResult(BYTES_USERNAME, BYTES_PASSWORD, storage);
    final Promise mockPromise = mock(Promise.class);
    final JavaOnlyMap options = new JavaOnlyMap();
    options.putString(Maps.SERVICE, "dummy");

    // store record done with RSA/Biometric cipher
    prefs.storeEncryptedEntry("dummy", result);

    assertThat(storage, instanceOf(CipherStorage.class));
    ((CipherStorageBase)storage).setCipher(mockCipher).setKeyStore(mockKeyStore);
    when(mockKeyStore.getKey(eq("dummy"), isNull())).thenReturn(null); // return empty Key!

    // WHEN:
    module.getGenericPasswordForOptions(options, mockPromise);

    // THEN:
    ArgumentCaptor<Exception> exception = ArgumentCaptor.forClass(Exception.class);
    verify(mockPromise).reject(eq(Errors.E_CRYPTO_FAILED), exception.capture());
    assertThat(exception.getValue(), instanceOf(CryptoFailedException.class));
    assertThat(exception.getValue().getCause(), instanceOf(KeyStoreAccessException.class));
    assertThat(exception.getValue().getMessage(), is("Wrapped error: Empty key extracted!"));
  }
}
