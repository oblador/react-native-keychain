package com.oblador.keychain.cipherStorage;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oblador.keychain.SecurityLevel;
import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.ProviderException;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;

import static com.oblador.keychain.SecurityLevel.SECURE_HARDWARE;

@SuppressWarnings({"unused", "WeakerAccess", "CharsetObjectCanBeUsed"})
abstract public class CipherStorageBase implements CipherStorage {
  //region Constants
  /** Logging tag. */
  protected static final String LOG_TAG = CipherStorageBase.class.getSimpleName();
  /** Default key storage type/name. */
  public static final String KEYSTORE_TYPE = "AndroidKeyStore";
  /** Key used for testing storage capabilities. */
  public static final String TEST_KEY_ALIAS = KEYSTORE_TYPE + "#supportsSecureHardware";
  /** Default service name. */
  public static final String DEFAULT_ALIAS = "RN_KEYCHAIN_DEFAULT_ALIAS";
  /** Size of hash calculation buffer. Default: 4Kb. */
  private static final int BUFFER_SIZE = 4 * 1024;
  /** Default size of read/write operation buffer. Default: 16Kb. */
  private static final int BUFFER_READ_WRITE_SIZE = 4 * BUFFER_SIZE;
  /** Default charset encoding. */
  public static final Charset UTF8 = Charset.forName("UTF-8");
  //endregion

  //region Members
  /** Guard object for {@link #isSupportsSecureHardware} field. */
  protected final Object _sync = new Object();
  /** Try to resolve it only once and cache result for all future calls. */
  protected transient AtomicBoolean isSupportsSecureHardware;
  //endregion

  //region Overrides

  /** Hardware supports keystore operations. */
  @Override
  public SecurityLevel securityLevel() {
    return SecurityLevel.SECURE_HARDWARE;
  }

  /**
   * The higher value means better capabilities. Range: [19..1129].
   * Formula: `1000 * isBiometrySupported() + 100 * isSecureHardware() + minSupportedApiLevel()`
   */
  @Override
  public final int getCapabilityLevel() {
    // max: 1000 + 100 + 29 == 1129
    // min: 0000 + 000 + 19 == 0019

    return
      (1000 * (isBiometrySupported() ? 1 : 0)) + // 0..1000
        (100 * (supportsSecureHardware() ? 1 : 0)) + // 0..100
        (getMinSupportedApiLevel()); // 19..29
  }

  /** Try device capabilities by creating temporary key in keystore. */
  @Override
  public boolean supportsSecureHardware() {
    if (null != isSupportsSecureHardware) return isSupportsSecureHardware.get();

    synchronized (_sync) {
      // double check pattern in use
      if (null != isSupportsSecureHardware) return isSupportsSecureHardware.get();

      isSupportsSecureHardware = new AtomicBoolean(false);

      try (SelfDestroyKey sdk = new SelfDestroyKey(TEST_KEY_ALIAS)) {
        isSupportsSecureHardware.set(validateKeySecurityLevel(SECURE_HARDWARE, sdk.key));
      } catch (Throwable ignored) {
      }
    }

    return isSupportsSecureHardware.get();
  }

  /** Remove key with provided name from security storage. */
  @Override
  public void removeKey(@NonNull final String alias) throws KeyStoreAccessException {
    final String safeService = getDefaultAliasIfEmpty(alias);
    final KeyStore ks = getKeyStoreAndLoad();

    try {
      if (ks.containsAlias(safeService)) {
        ks.deleteEntry(safeService);
      }
    } catch (GeneralSecurityException ignored) {
      /* only one exception can be raised by code: 'KeyStore is not loaded' */
    }
  }

  //endregion

  //region Abstract methods

  /** Get encryption algorithm specification builder instance. */
  @NonNull
  protected abstract KeyGenParameterSpec.Builder getKeyGenSpecBuilder(@NonNull final String alias)
    throws GeneralSecurityException;

  /** Get information about provided key. */
  @NonNull
  protected abstract KeyInfo getKeyInfo(@NonNull final Key key) throws GeneralSecurityException;

  /** Try to generate key from provided specification. */
  @NonNull
  protected abstract Key generateKey(@NonNull final KeyGenParameterSpec spec)
    throws GeneralSecurityException;

  /** Get name of the required encryption algorithm. */
  @NonNull
  protected abstract String getEncryptionAlgorithm();

  /** Get transformation algorithm for encrypt/decrypt operations. */
  @NonNull
  protected abstract String getEncryptionTransformation();
  //endregion

  //region Implementation

  /** Check requirements to the security level. */
  protected void throwIfInsufficientLevel(@NonNull final SecurityLevel level)
    throws CryptoFailedException {

    if (!securityLevel().satisfiesSafetyThreshold(level)) {
      throw new CryptoFailedException(String.format(
        "Insufficient security level (wants %s; got %s)",
        level, securityLevel()));
    }
  }

  /** Extract existing key or generate a new one. In case of problems raise exception. */
  @NonNull
  protected Key extractGeneratedKey(@NonNull final String safeAlias,
                                    @NonNull final SecurityLevel level,
                                    @NonNull final AtomicInteger retries)
    throws GeneralSecurityException {
    Key key;

    do {
      final KeyStore keyStore = getKeyStoreAndLoad();

      // if key is not available yet, try to generate the strongest possible
      if (!keyStore.containsAlias(safeAlias)) {
        generateKeyAndStoreUnderAlias(safeAlias, level);
      }

      // throw exception if cannot extract key in several retries
      key = extractKey(keyStore, safeAlias, retries);
    } while (null == key);

    return key;
  }

  /** Try to extract key by alias from keystore, in case of 'known android bug' reduce retry counter. */
  @Nullable
  protected Key extractKey(@NonNull final KeyStore keyStore,
                           @NonNull final String safeAlias,
                           @NonNull final AtomicInteger retry)
    throws GeneralSecurityException {
    final Key key;

    // Fix for android.security.KeyStoreException: Invalid key blob
    // more info: https://stackoverflow.com/questions/36488219/android-security-keystoreexception-invalid-key-blob/36846085#36846085
    try {
      key = keyStore.getKey(safeAlias, null);
    } catch (final UnrecoverableKeyException ex) {
      // try one more time
      if (retry.getAndDecrement() > 0) {
        keyStore.deleteEntry(safeAlias);

        return null;
      }

      throw ex;
    }

    // null if the given alias does not exist or does not identify a key-related entry.
    if (null == key) {
      throw new KeyStoreAccessException("Empty key extracted!");
    }

    return key;
  }

  /** Verify that provided key satisfy minimal needed level. */
  protected boolean validateKeySecurityLevel(@NonNull final SecurityLevel level,
                                             @NonNull final Key key)
    throws GeneralSecurityException {

    return getSecurityLevel(key)
      .satisfiesSafetyThreshold(level);
  }

  /** Get the supported level of security for provided Key instance. */
  @NonNull
  protected SecurityLevel getSecurityLevel(@NonNull final Key key) throws GeneralSecurityException {
    final KeyInfo keyInfo = getKeyInfo(key);

    // lower API23 we don't have any hardware support
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (keyInfo.isInsideSecureHardware()) {
        return SECURE_HARDWARE;
      }
    }

    return SecurityLevel.SECURE_SOFTWARE;
  }

  /** Load key store. */
  @NonNull
  protected KeyStore getKeyStoreAndLoad() throws KeyStoreAccessException {
    try {
      final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);

      // initialize instance
      keyStore.load(null);

      return keyStore;
    } catch (final Throwable fail) {
      throw new KeyStoreAccessException("Could not access Keystore", fail);
    }
  }

  /** Default encryption with cipher without initialization vector. */
  @NonNull
  public byte[] encryptString(@NonNull final Key key, @NonNull final String value)
    throws IOException, GeneralSecurityException {

    return encryptString(key, value, Defaults.encrypt);
  }

  /** Default decryption with cipher without initialization vector. */
  @NonNull
  public String decryptBytes(@NonNull final Key key, @NonNull final byte[] bytes)
    throws IOException, GeneralSecurityException {

    return decryptBytes(key, bytes, Defaults.decrypt);
  }

  /** Encrypt provided string value. */
  @NonNull
  protected byte[] encryptString(@NonNull final Key key, @NonNull final String value,
                                 @Nullable final EncryptStringHandler handler)
    throws IOException, GeneralSecurityException {

    final Cipher cipher = Cipher.getInstance(getEncryptionTransformation());

    // encrypt the value using a CipherOutputStream
    try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {

      // write initialization vector to the beginning of the stream
      if (null != handler) {
        handler.initialize(cipher, key, output);
        output.flush();
      }

      try (final CipherOutputStream encrypt = new CipherOutputStream(output, cipher)) {
        encrypt.write(value.getBytes(UTF8));
      }

      return output.toByteArray();
    } catch (Throwable fail) {
      Log.e(LOG_TAG, fail.getMessage(), fail);

      throw fail;
    }
  }

  /** Decrypt provided bytes to a string. */
  @NonNull
  protected String decryptBytes(@NonNull final Key key, @NonNull final byte[] bytes,
                                @Nullable final DecryptBytesHandler handler)
    throws GeneralSecurityException, IOException {
    final Cipher cipher = Cipher.getInstance(getEncryptionTransformation());

    // decrypt the bytes using a CipherInputStream
    try (ByteArrayInputStream in = new ByteArrayInputStream(bytes);
         ByteArrayOutputStream output = new ByteArrayOutputStream()) {

      // read the initialization vector from the beginning of the stream
      if (null != handler) {
        handler.initialize(cipher, key, in);
      }

      try (CipherInputStream decrypt = new CipherInputStream(in, cipher)) {
        copy(decrypt, output);
      }

      return new String(output.toByteArray(), UTF8);
    } catch (Throwable fail) {
      Log.w(LOG_TAG, fail.getMessage(), fail);

      throw fail;
    }
  }

  /** Get the most secured keystore */
  protected void generateKeyAndStoreUnderAlias(@NonNull final String alias,
                                               @NonNull final SecurityLevel requiredLevel)
    throws GeneralSecurityException {

    // Firstly, try to generate the key as safe as possible (strongbox).
    // see https://developer.android.com/training/articles/keystore#HardwareSecurityModule

    Key secretKey;

    try {
      secretKey = tryGenerateStrongBoxSecurityKey(alias);
    } catch (GeneralSecurityException | ProviderException ex) {
      Log.w(LOG_TAG, "StrongBox security storage is not available.", ex);

      // If that is not possible, we generate the key in a regular way
      // (it still might be generated in hardware, but not in StrongBox)
      try {
        secretKey = tryGenerateRegularSecurityKey(alias);
      } catch (GeneralSecurityException fail) {
        Log.e(LOG_TAG, "Regular security storage is not available.", fail);
        throw fail;
      }
    }

    if (!validateKeySecurityLevel(requiredLevel, secretKey)) {
      throw new CryptoFailedException("Cannot generate keys with required security guarantees");
    }
  }

  /** Try to get secured keystore instance. */
  @NonNull
  protected Key tryGenerateRegularSecurityKey(@NonNull final String alias)
    throws GeneralSecurityException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      throw new KeyStoreAccessException("Regular security keystore is not supported " +
        "for old API" + Build.VERSION.SDK_INT + ".");
    }

    final KeyGenParameterSpec specification = getKeyGenSpecBuilder(alias)
      .build();

    return generateKey(specification);
  }

  /** Try to get strong secured keystore instance. (StrongBox security chip) */
  @NonNull
  protected Key tryGenerateStrongBoxSecurityKey(@NonNull final String alias)
    throws GeneralSecurityException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
      throw new KeyStoreAccessException("Strong box security keystore is not supported " +
        "for old API" + Build.VERSION.SDK_INT + ".");
    }

    final KeyGenParameterSpec specification = getKeyGenSpecBuilder(alias)
      .setIsStrongBoxBacked(true)
      .build();

    return generateKey(specification);
  }

  //endregion

  //region Static methods

  /** Convert provided service name to safe not-null/not-empty value. */
  @NonNull
  public static String getDefaultAliasIfEmpty(@Nullable final String service) {
    //noinspection ConstantConditions
    return TextUtils.isEmpty(service) ? DEFAULT_ALIAS : service;
  }

  /**
   * Copy input stream to output.
   *
   * @param in  instance of input stream.
   * @param out instance of output stream.
   * @throws IOException read/write operation failure.
   */
  public static void copy(@NonNull final InputStream in, @NonNull final OutputStream out) throws IOException {
    // Transfer bytes from in to out
    final byte[] buf = new byte[BUFFER_READ_WRITE_SIZE];
    int len;

    while ((len = in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }
  }
  //endregion

  //region Nested declarations

  /** Generic cipher initialization. */
  public static final class Defaults {
    public static final EncryptStringHandler encrypt = (cipher, key, output) -> {
      cipher.init(Cipher.ENCRYPT_MODE, key);
    };

    public static final DecryptBytesHandler decrypt = (cipher, key, input) -> {
      cipher.init(Cipher.DECRYPT_MODE, key);
    };
  }

  /** Initialization vector support. */
  public static final class IV {
    /** Encryption/Decryption initialization vector length. */
    public static final int IV_LENGTH = 16;

    /** Save Initialization vector to output stream. */
    public static final EncryptStringHandler encrypt = (cipher, key, output) -> {
      cipher.init(Cipher.ENCRYPT_MODE, key);

      final byte[] iv = cipher.getIV();
      output.write(iv, 0, iv.length);
    };
    /** Read initialization vector from input stream and configure cipher by it. */
    public static final DecryptBytesHandler decrypt = (cipher, key, input) -> {
      final IvParameterSpec iv = readIv(input);
      cipher.init(Cipher.DECRYPT_MODE, key, iv);
    };

    /** Extract initialization vector from provided bytes array. */
    @NonNull
    public static IvParameterSpec readIv(@NonNull final byte[] bytes) throws IOException {
      final byte[] iv = new byte[IV_LENGTH];

      if (IV_LENGTH <= bytes.length)
        throw new IOException("Insufficient length of input data for IV extracting.");

      System.arraycopy(bytes, 0, iv, 0, IV_LENGTH);

      return new IvParameterSpec(iv);
    }

    /** Extract initialization vector from provided input stream. */
    @NonNull
    public static IvParameterSpec readIv(@NonNull final InputStream inputStream) throws IOException {
      final byte[] iv = new byte[IV_LENGTH];
      final int result = inputStream.read(iv, 0, IV_LENGTH);

      if (result != IV_LENGTH)
        throw new IOException("Input stream has insufficient data.");

      return new IvParameterSpec(iv);
    }
  }

  /** Handler for storing cipher configuration in output stream. */
  public interface EncryptStringHandler {
    void initialize(@NonNull final Cipher cipher, @NonNull final Key key, @NonNull final OutputStream output)
      throws GeneralSecurityException, IOException;
  }

  /** Handler for configuring cipher by initialization data from input stream. */
  public interface DecryptBytesHandler {
    void initialize(@NonNull final Cipher cipher, @NonNull final Key key, @NonNull final InputStream input)
      throws GeneralSecurityException, IOException;
  }

  /** Auto remove keystore key. */
  public class SelfDestroyKey implements AutoCloseable {
    public final String name;
    public final Key key;

    public SelfDestroyKey(@NonNull final String name) throws GeneralSecurityException {
      this(name, tryGenerateRegularSecurityKey(name));
    }

    public SelfDestroyKey(@NonNull final String name, @NonNull final Key key) {
      this.name = name;
      this.key = key;
    }

    @Override
    public void close() {
      try {
        removeKey(name);
      } catch (KeyStoreAccessException ex) {
        Log.w(LOG_TAG, "AutoClose remove key failed. Error: " + ex.getMessage(), ex);
      }
    }
  }
  //endregion
}
