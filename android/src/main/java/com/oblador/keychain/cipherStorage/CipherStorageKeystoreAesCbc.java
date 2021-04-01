package com.oblador.keychain.cipherStorage;

import android.annotation.TargetApi;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oblador.keychain.KeychainModule.KnownCiphers;
import com.oblador.keychain.SecurityLevel;
import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.spec.KeySpec;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;

/**
 * @see <a href="https://proandroiddev.com/secure-data-in-android-initialization-vector-6ca1c659762c">Secure Data in Android</a>
 * @see <a href="https://stackoverflow.com/questions/36827352/android-aes-with-keystore-produces-different-cipher-text-with-same-plain-text">AES cipher</a>
 */
@TargetApi(Build.VERSION_CODES.M)
@SuppressWarnings({"unused", "WeakerAccess"})
public class CipherStorageKeystoreAesCbc extends CipherStorageBase {
  //region Constants
  /** AES */
  public static final String ALGORITHM_AES = KeyProperties.KEY_ALGORITHM_AES;
  /** CBC */
  public static final String BLOCK_MODE_CBC = KeyProperties.BLOCK_MODE_CBC;
  /** PKCS7 */
  public static final String PADDING_PKCS7 = KeyProperties.ENCRYPTION_PADDING_PKCS7;
  /** Transformation path. */
  public static final String ENCRYPTION_TRANSFORMATION =
    ALGORITHM_AES + "/" + BLOCK_MODE_CBC + "/" + PADDING_PKCS7;
  /** Key size. */
  public static final int ENCRYPTION_KEY_SIZE = 256;

  public static final String DEFAULT_SERVICE = "RN_KEYCHAIN_DEFAULT_ALIAS";
  //endregion

  //region Configuration
  @Override
  public String getCipherStorageName() {
    return KnownCiphers.AES;
  }

  /** API23 is a requirement. */
  @Override
  public int getMinSupportedApiLevel() {
    return Build.VERSION_CODES.M;
  }

  /** it can guarantee security levels up to SECURE_HARDWARE/SE/StrongBox */
  @Override
  public SecurityLevel securityLevel() {
    return SecurityLevel.SECURE_HARDWARE;
  }

  /** Biometry is Not Supported. */
  @Override
  public boolean isBiometrySupported() {
    return true;
  }

  /** AES. */
  @Override
  @NonNull
  protected String getEncryptionAlgorithm() {
    return ALGORITHM_AES;
  }

  /** AES/CBC/PKCS7Padding */
  @NonNull
  @Override
  protected String getEncryptionTransformation() {
    return ENCRYPTION_TRANSFORMATION;
  }

  /** {@inheritDoc}. Override for saving the compatibility with previous version of lib. */
  @Override
  public String getDefaultAliasServiceName() {
    return DEFAULT_SERVICE;
  }

  //endregion

  //region Overrides
  @Override
  @NonNull
  public EncryptionResult encrypt(@NonNull final String alias,
                                  @NonNull final String username,
                                  @NonNull final String password,
                                  @NonNull final SecurityLevel level)
    throws CryptoFailedException {
      System.out.println("ABSA_LOG : encrypt before exception : ");
    throwIfInsufficientLevel(level);
    System.out.println("ABSA_LOG : encrypt after exception : alias: " + alias);

    final String safeAlias = getDefaultAliasIfEmpty(alias, getDefaultAliasServiceName());
    final AtomicInteger retries = new AtomicInteger(1);
    System.out.println("ABSA_LOG : safeAlias: " + safeAlias);
    Key key = null;
    try {
      System.out.println("ABSA_LOG : encrypt 1 : ");
      key = extractGeneratedKey(safeAlias, level, retries);
      System.out.println("ABSA_LOG : encrypt 2 : ");
      return new EncryptionResult(
        encryptString(key, username),
        encryptString(key, password),
        this);
    } catch (GeneralSecurityException e) {
      // @SuppressWarnings("ConstantConditions") final DecryptionContext context =
      //   new DecryptionContext(safeAlias, key, password, username);

      // handler.askAccessPermissions(context);
      throw new CryptoFailedException("Could not encrypt data with alias: " + alias, e);
    } catch (Throwable fail) {
      throw new CryptoFailedException("Unknown error with alias: " + alias +
        ", error: " + fail.getMessage(), fail);
    }
  }

  @Override
  public EncryptionResult encrypt(@NonNull final DecryptionResultHandler handler,
                                  @NonNull final String alias,
                                  @NonNull final String username,
                                  @NonNull final String password,
                                  @NonNull final SecurityLevel level)
    throws CryptoFailedException {
      System.out.println("ABSA_LOG : encrypt before exception : ");
    throwIfInsufficientLevel(level);
    System.out.println("ABSA_LOG : encrypt after exception : alias: " + alias);

    final String safeAlias = getDefaultAliasIfEmpty(alias, getDefaultAliasServiceName());
    final AtomicInteger retries = new AtomicInteger(1);
    System.out.println("ABSA_LOG : safeAlias: " + safeAlias);
    Key key = null;
    try {
      System.out.println("ABSA_LOG : encrypt 1 : ");
      key = extractGeneratedKey(safeAlias, level, retries);
      System.out.println("ABSA_LOG : encrypt 2 : ");
      return new EncryptionResult(
        encryptString(key, username),
        encryptString(key, password),
        this);
    } catch (GeneralSecurityException e) {
      // @SuppressWarnings("ConstantConditions") final DecryptionContext context =
      //   new DecryptionContext(safeAlias, key, password, username);

      @SuppressWarnings("ConstantConditions") final DecryptionContext context =
        new DecryptionContext(safeAlias, key, new byte[8], new byte[8]);

      handler.askAccessPermissions(context);
      // throw new CryptoFailedException("Could not encrypt data with alias: " + alias, e);
    } catch (Throwable fail) {
      throw new CryptoFailedException("Unknown error with alias: " + alias +
        ", error: " + fail.getMessage(), fail);
    }
      return null;
    }

  // @Override
  // @NonNull
  // public EncryptionResult encrypt(@NonNull final DecryptionResultHandler handler,
  //                                 @NonNull final String alias,
  //                                 @NonNull final String username,
  //                                 @NonNull final String password,
  //                                 @NonNull final SecurityLevel level)
  //   throws CryptoFailedException {
  //     System.out.println("ABSA_LOG : encrypt before exception : ");
  //   throwIfInsufficientLevel(level);
  //   System.out.println("ABSA_LOG : encrypt after exception : alias: " + alias);

  //   final String safeAlias = getDefaultAliasIfEmpty(alias, getDefaultAliasServiceName());
  //   final AtomicInteger retries = new AtomicInteger(1);
  //   System.out.println("ABSA_LOG : safeAlias: " + safeAlias);
  //   Key key = null;
  //   try {
  //     System.out.println("ABSA_LOG : encrypt 1 : ");
  //     key = extractGeneratedKey(safeAlias, level, retries);
  //     System.out.println("ABSA_LOG : encrypt 2 : ");
  //     return new EncryptionResult(
  //       encryptString(key, username),
  //       encryptString(key, password),
  //       this);
  //   } catch (GeneralSecurityException e) {
  //     @SuppressWarnings("ConstantConditions") final DecryptionContext context =
  //       new DecryptionContext(safeAlias, key, password, username);

  //     handler.askAccessPermissions(context);
  //     // throw new CryptoFailedException("Could not encrypt data with alias: " + alias, e);
  //   } catch (Throwable fail) {
  //     throw new CryptoFailedException("Unknown error with alias: " + alias +
  //       ", error: " + fail.getMessage(), fail);
  //   }
  // }

  @Override
  @NonNull
  public DecryptionResult decrypt(
                                  @NonNull final String alias,
                                  @NonNull final byte[] username,
                                  @NonNull final byte[] password,
                                  @NonNull final SecurityLevel level)
    throws CryptoFailedException {
      System.out.println("ABSA_LOG : NON handler type : ");
    throwIfInsufficientLevel(level);

    final String safeAlias = getDefaultAliasIfEmpty(alias, getDefaultAliasServiceName());
    final AtomicInteger retries = new AtomicInteger(1);

    try {
      final Key key = extractGeneratedKey(safeAlias, level, retries);

      return new DecryptionResult(
        decryptBytes(key, username),
        decryptBytes(key, password),
        getSecurityLevel(key));
    } catch (GeneralSecurityException e) {
      throw new CryptoFailedException("Could not decrypt data with alias: " + alias, e);
    } catch (Throwable fail) {
      throw new CryptoFailedException("Unknown error with alias: " + alias +
        ", error: " + fail.getMessage(), fail);
    }
  }

  /** Redirect call to {@link #decrypt(String, byte[], byte[], SecurityLevel)} method. */
  @Override
  public void decrypt(@NonNull final DecryptionResultHandler handler,
                      @NonNull final String service,
                      @NonNull final byte[] username,
                      @NonNull final byte[] password,
                      @NonNull final SecurityLevel level) throws CryptoFailedException {
                        System.out.println("ABSA_LOG : handler type : " + handler.toString());
    // try {
    //   final DecryptionResult results = decrypt(service, username, password, level);

    //   handler.onDecrypt(results, null);
    // } catch (Throwable fail) {
    //   handler.onDecrypt(null, fail);
    // }
    throwIfInsufficientLevel(level);

    final String safeAlias = getDefaultAliasIfEmpty(service, getDefaultAliasServiceName());
    final AtomicInteger retries = new AtomicInteger(1);
    boolean shouldAskPermissions = false;

    Key key = null;

    try {
      // key is always NOT NULL otherwise GeneralSecurityException raised
      key = extractGeneratedKey(safeAlias, level, retries);

      // final DecryptionResult results = new DecryptionResult(
      //   decryptBytes(key, username),
      //   decryptBytes(key, password)
      // );

      // handler.onDecrypt(results, null);

      // Log.d(LOG_TAG, "Unlock of keystore is needed. Error: " + ex.getMessage(), ex);

      // expected that KEY instance is extracted and we caught exception on decryptBytes operation
      @SuppressWarnings("ConstantConditions") final DecryptionContext context =
        new DecryptionContext(safeAlias, key, password, username);

      handler.askAccessPermissions(context);
    } catch (final Exception ex) {
      Log.d(LOG_TAG, "Unlock of keystore is needed. Error: " + ex.getMessage(), ex);

      // expected that KEY instance is extracted and we caught exception on decryptBytes operation
      @SuppressWarnings("ConstantConditions") final DecryptionContext context =
        new DecryptionContext(safeAlias, key, password, username);

      handler.askAccessPermissions(context);
    } catch (final Throwable fail) {
      // any other exception treated as a failure
      handler.onDecrypt(null, fail);
    }
  }
  //endregion

  //region Implementation

  /** Get encryption algorithm specification builder instance. */
  @NonNull
  @Override
  protected KeyGenParameterSpec.Builder getKeyGenSpecBuilder(@NonNull final String alias)
    throws GeneralSecurityException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      throw new KeyStoreAccessException("Unsupported API" + Build.VERSION.SDK_INT + " version detected.");
    }

    // createDecryptKey();

    final int purposes = KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT;

    System.out.println("ABSA_LOG : getKeyGenSpecBuilder called with alias : " + alias);

    return new KeyGenParameterSpec.Builder(alias, purposes)
      .setBlockModes(BLOCK_MODE_CBC)
      .setEncryptionPaddings(PADDING_PKCS7) //.setUserAuthenticationRequired(true)
      .setUserAuthenticationRequired(true)
      .setUserAuthenticationValidityDurationSeconds(5)
      .setRandomizedEncryptionRequired(true)
      .setKeySize(ENCRYPTION_KEY_SIZE);
  }

  private void createDecryptKey() throws GeneralSecurityException {

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      throw new KeyStoreAccessException("Unsupported API" + Build.VERSION.SDK_INT + " version detected.");
    }

    final int purposes = KeyProperties.PURPOSE_DECRYPT;// | KeyProperties.PURPOSE_ENCRYPT;
    KeyGenerator generator = KeyGenerator.getInstance(getEncryptionAlgorithm(), KEYSTORE_TYPE);
    
    System.out.println("ABSA_LOG : getKeyGenSpecBuilder called is : ");

    KeyGenParameterSpec a = new KeyGenParameterSpec.Builder("RN_KEYCHAIN_DEFAULT_ALIAS_DECR", purposes)
      .setBlockModes(BLOCK_MODE_CBC)
      .setEncryptionPaddings(PADDING_PKCS7) //.setUserAuthenticationRequired(true)
      .setUserAuthenticationRequired(true)
      .setRandomizedEncryptionRequired(true)
      .setKeySize(ENCRYPTION_KEY_SIZE)
      .build();

      generator.init(a);
      generator.generateKey();
  }

  /** Get information about provided key. */
  @NonNull
  @Override
  protected KeyInfo getKeyInfo(@NonNull final Key key) throws GeneralSecurityException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      throw new KeyStoreAccessException("Unsupported API" + Build.VERSION.SDK_INT + " version detected.");
    }

    final SecretKeyFactory factory = SecretKeyFactory.getInstance(key.getAlgorithm(), KEYSTORE_TYPE);
    final KeySpec keySpec = factory.getKeySpec((SecretKey) key, KeyInfo.class);

    return (KeyInfo) keySpec;
  }

  /** Try to generate key from provided specification. */
  @NonNull
  @Override
  protected Key generateKey(@NonNull final KeyGenParameterSpec spec) throws GeneralSecurityException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      throw new KeyStoreAccessException("Unsupported API" + Build.VERSION.SDK_INT + " version detected.");
    }

    KeyGenerator generator = KeyGenerator.getInstance(getEncryptionAlgorithm(), KEYSTORE_TYPE);

    

    // final int purposes = KeyProperties.PURPOSE_DECRYPT;// | KeyProperties.PURPOSE_ENCRYPT;

    // System.out.println("ABSA_LOG : getKeyGenSpecBuilder called is : ");

    // KeyGenParameterSpec a = new KeyGenParameterSpec.Builder("RN_KEYCHAIN_DEFAULT_ALIAS", purposes)
    //   .setBlockModes(BLOCK_MODE_CBC)
    //   .setEncryptionPaddings(PADDING_PKCS7) //.setUserAuthenticationRequired(true)
    //   .setUserAuthenticationRequired(true)
    //   .setRandomizedEncryptionRequired(true)
    //   .setKeySize(ENCRYPTION_KEY_SIZE)
    //   .build();

    //   generator.init(a);
    //   generator.generateKey();

    // generator = KeyGenerator.getInstance(getEncryptionAlgorithm(), KEYSTORE_TYPE);

    // initialize key generator
    generator.init(spec);

    return generator.generateKey();
  }

  /** Decrypt provided bytes to a string. */
  @NonNull
  @Override
  protected String decryptBytes(@NonNull final Key key, @NonNull final byte[] bytes,
                                @Nullable final DecryptBytesHandler handler)
    throws GeneralSecurityException, IOException {
    final Cipher cipher = getCachedInstance();

    try {
      // read the initialization vector from bytes array
      final IvParameterSpec iv = IV.readIv(bytes);
      cipher.init(Cipher.DECRYPT_MODE, key, iv);

      // decrypt the bytes using cipher.doFinal(). Using a CipherInputStream for decryption has historically led to issues
      // on the Pixel family of devices.
      // see https://github.com/oblador/react-native-keychain/issues/383
      byte[] decryptedBytes = cipher.doFinal(bytes, IV.IV_LENGTH, bytes.length - IV.IV_LENGTH);
      return new String(decryptedBytes, UTF8);
    } catch (Throwable fail) {
      Log.w(LOG_TAG, fail.getMessage(), fail);

      throw fail;
    }
  }
  //endregion

  //region Initialization Vector encrypt/decrypt support
  @NonNull
  @Override
  public byte[] encryptString(@NonNull final Key key, @NonNull final String value)
    throws GeneralSecurityException, IOException {

    return encryptString(key, value, IV.encrypt);
  }

  @NonNull
  @Override
  public String decryptBytes(@NonNull final Key key, @NonNull final byte[] bytes)
    throws GeneralSecurityException, IOException {
    return decryptBytes(key, bytes, IV.decrypt);
  }
  //endregion
}