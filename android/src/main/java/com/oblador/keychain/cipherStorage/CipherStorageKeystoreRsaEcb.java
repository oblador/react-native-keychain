package com.oblador.keychain.cipherStorage;

import android.annotation.SuppressLint;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.oblador.keychain.KeychainModule;
import com.oblador.keychain.SecurityLevel;
import com.oblador.keychain.decryptionHandler.DecryptionResultHandler;
import com.oblador.keychain.decryptionHandler.DecryptionResultHandlerNonInteractive;
import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.NoSuchPaddingException;

/** Fingerprint biometry protected storage. */
@RequiresApi(api = Build.VERSION_CODES.M)
@SuppressWarnings({"unused", "WeakerAccess"})
public class CipherStorageKeystoreRsaEcb extends CipherStorageBase {
  //region Constants
  /** Selected algorithm. */
  public static final String ALGORITHM_RSA = KeyProperties.KEY_ALGORITHM_RSA;
  /** Selected block mode. */
  public static final String BLOCK_MODE_ECB = KeyProperties.BLOCK_MODE_ECB;
  /** Selected padding transformation. */
  public static final String PADDING_PKCS1 = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1;
  /** Composed transformation algorithms. */
  public static final String TRANSFORMATION_RSA_ECB_PKCS1 =
    ALGORITHM_RSA + "/" + BLOCK_MODE_ECB + "/" + PADDING_PKCS1;
  /** Selected encryption key size. */
  public static final int ENCRYPTION_KEY_SIZE = 3072;
  public static final int ENCRYPTION_KEY_SIZE_WHEN_TESTING = 512;

  //endregion

  //region Overrides
  @Override
  @NonNull
  public EncryptionResult encrypt(@NonNull final String alias,
                                  @NonNull final String username,
                                  @NonNull final String password,
                                  @NonNull final SecurityLevel level)
    throws CryptoFailedException {

    throwIfInsufficientLevel(level);

    final String safeAlias = getDefaultAliasIfEmpty(alias, getDefaultAliasServiceName());

    try {
      return innerEncryptedCredentials(safeAlias, password, username, level);

      // KeyStoreException | KeyStoreAccessException  | NoSuchAlgorithmException | InvalidKeySpecException |
      //    IOException | NoSuchPaddingException | InvalidKeyException e
    } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException e) {
      throw new CryptoFailedException("Could not encrypt data for service " + alias, e);
    } catch (KeyStoreException | KeyStoreAccessException e) {
      throw new CryptoFailedException("Could not access Keystore for service " + alias, e);
    } catch (IOException io) {
      throw new CryptoFailedException("I/O error: " + io.getMessage(), io);
    } catch (final Throwable ex) {
      throw new CryptoFailedException("Unknown error: " + ex.getMessage(), ex);
    }
  }

  @NonNull
  @Override
  public DecryptionResult decrypt(@NonNull String alias,
                                  @NonNull byte[] username,
                                  @NonNull byte[] password,
                                  @NonNull final SecurityLevel level)
    throws CryptoFailedException {

    final DecryptionResultHandlerNonInteractive handler = new DecryptionResultHandlerNonInteractive();
    decrypt(handler, alias, username, password, level);

    CryptoFailedException.reThrowOnError(handler.getError());

    if (null == handler.getResult()) {
      throw new CryptoFailedException("No decryption results and no error. Something deeply wrong!");
    }

    return handler.getResult();
  }

  @Override
  @SuppressLint("NewApi")
  public void decrypt(@NonNull DecryptionResultHandler handler,
                      @NonNull String alias,
                      @NonNull byte[] username,
                      @NonNull byte[] password,
                      @NonNull final SecurityLevel level)
    throws CryptoFailedException {

    throwIfInsufficientLevel(level);

    final String safeAlias = getDefaultAliasIfEmpty(alias, getDefaultAliasServiceName());
    final AtomicInteger retries = new AtomicInteger(1);
    boolean shouldAskPermissions = false;

    Key key = null;

    try {
      // key is always NOT NULL otherwise GeneralSecurityException raised
      key = extractGeneratedKey(safeAlias, level, retries);

      final DecryptionResult results = new DecryptionResult(
        decryptBytes(key, username),
        decryptBytes(key, password)
      );

      handler.onDecrypt(results, null);
    } catch (final UserNotAuthenticatedException ex) {
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

  //region Configuration

  /** RSAECB. */
  @Override
  public String getCipherStorageName() {
    return KeychainModule.KnownCiphers.RSA;
  }

  /** API23 is a requirement. */
  @Override
  public int getMinSupportedApiLevel() {
    return Build.VERSION_CODES.M;
  }

  /** Biometry is supported. */
  @Override
  public boolean isBiometrySupported() {
    return true;
  }

  /** RSA. */
  @NonNull
  @Override
  protected String getEncryptionAlgorithm() {
    return ALGORITHM_RSA;
  }

  /** RSA/ECB/PKCS1Padding */
  @NonNull
  @Override
  protected String getEncryptionTransformation() {
    return TRANSFORMATION_RSA_ECB_PKCS1;
  }
  //endregion

  //region Implementation

  /** Clean code without try/catch's that encrypt username and password with a key specified by alias. */
  @NonNull
  private EncryptionResult innerEncryptedCredentials(@NonNull final String alias,
                                                     @NonNull final String password,
                                                     @NonNull final String username,
                                                     @NonNull final SecurityLevel level)
    throws GeneralSecurityException, IOException {

    final KeyStore store = getKeyStoreAndLoad();

    // on first access create a key for storage
    if (!store.containsAlias(alias)) {
      generateKeyAndStoreUnderAlias(alias, level);
    }

    final KeyFactory kf = KeyFactory.getInstance(ALGORITHM_RSA);
    final Certificate certificate = store.getCertificate(alias);
    final PublicKey publicKey = certificate.getPublicKey();
    final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKey.getEncoded());
    final PublicKey key = kf.generatePublic(keySpec);

    return new EncryptionResult(
      encryptString(key, username),
      encryptString(key, password),
      this);
  }

  /** Get builder for encryption and decryption operations with required user Authentication. */
  @NonNull
  @Override
  @SuppressLint("NewApi")
  protected KeyGenParameterSpec.Builder getKeyGenSpecBuilder(@NonNull final String alias) throws GeneralSecurityException{
    return getKeyGenSpecBuilder(alias, false);
  }

  /** Get builder for encryption and decryption operations with required user Authentication. */
  @NonNull
  @Override
  @SuppressLint("NewApi")
  protected KeyGenParameterSpec.Builder getKeyGenSpecBuilder(@NonNull final String alias, @NonNull final boolean isForTesting)
    throws GeneralSecurityException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      throw new KeyStoreAccessException("Unsupported API" + Build.VERSION.SDK_INT + " version detected.");
    }

    final int purposes = KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT;

    final int keySize = isForTesting ? ENCRYPTION_KEY_SIZE_WHEN_TESTING : ENCRYPTION_KEY_SIZE;

    return new KeyGenParameterSpec.Builder(alias, purposes)
      .setBlockModes(BLOCK_MODE_ECB)
      .setEncryptionPaddings(PADDING_PKCS1)
      .setRandomizedEncryptionRequired(true)
      .setUserAuthenticationRequired(true)
      .setUserAuthenticationValidityDurationSeconds(5)
      .setKeySize(keySize);
  }

  /** Get information about provided key. */
  @NonNull
  @Override
  protected KeyInfo getKeyInfo(@NonNull final Key key) throws GeneralSecurityException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      throw new KeyStoreAccessException("Unsupported API" + Build.VERSION.SDK_INT + " version detected.");
    }

    final KeyFactory factory = KeyFactory.getInstance(key.getAlgorithm(), KEYSTORE_TYPE);

    return factory.getKeySpec(key, KeyInfo.class);
  }

  /** Try to generate key from provided specification. */
  @NonNull
  @Override
  protected Key generateKey(@NonNull final KeyGenParameterSpec spec) throws GeneralSecurityException {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      throw new KeyStoreAccessException("Unsupported API" + Build.VERSION.SDK_INT + " version detected.");
    }

    final KeyPairGenerator generator = KeyPairGenerator.getInstance(getEncryptionAlgorithm(), KEYSTORE_TYPE);
    generator.initialize(spec);

    return generator.generateKeyPair().getPrivate();
  }

  //endregion
}
