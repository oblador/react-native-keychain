package com.oblador.keychain.cipherStorage;

import android.annotation.SuppressLint;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.oblador.keychain.KeychainModule;
import com.oblador.keychain.SecurityLevel;
import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

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
 * Fingerprint biometry protected storage.
 */
@RequiresApi(api = Build.VERSION_CODES.M)
@SuppressWarnings({"unused", "WeakerAccess"})
public class CipherStorageKeystoreAesCbcBiom extends CipherStorageBase {
    //region Constants
    /**
     * AES
     */
    public static final String ALGORITHM_AES = KeyProperties.KEY_ALGORITHM_AES;
    /**
     * CBC
     */
    public static final String BLOCK_MODE_CBC = KeyProperties.BLOCK_MODE_CBC;
    /**
     * PKCS7
     */
    public static final String PADDING_PKCS7 = KeyProperties.ENCRYPTION_PADDING_PKCS7;
    public static final String DEFAULT_SERVICE = "RN_KEYCHAIN_DEFAULT_ALIAS";

    /**
     * Composed transformation algorithms.
     */
    public static final String ENCRYPTION_TRANSFORMATION =
            ALGORITHM_AES + "/" + BLOCK_MODE_CBC + "/" + PADDING_PKCS7;
    /**
     * Selected encryption key size.
     */
    public static final int ENCRYPTION_KEY_SIZE = 256;
    //endregion

    //region Overrides
    @Override
    @NonNull
    public EncryptionResult encrypt(@NonNull final String alias,
                                    @NonNull final String username,
                                    @NonNull final String password,
                                    @NonNull final SecurityLevel level,
                                    @NonNull final DecryptionResultHandler handler)
            throws CryptoFailedException {

        throwIfInsufficientLevel(level);

        final String safeAlias = getDefaultAliasIfEmpty(alias, getDefaultAliasServiceName());
        final AtomicInteger retries = new AtomicInteger(1);

        try {
            final Key key = extractGeneratedKey(safeAlias, level, retries);

            final EncryptContext context =
                    new EncryptContext(alias, key, password, username);
            Cipher cipher = getCachedInstance();
            cipher.init(Cipher.ENCRYPT_MODE, key);
            handler.askAccessPermissionsEncryption(context, cipher);

            if (null == handler.getEncryptionResult()) {
                throw new CryptoFailedException("No encryption results. Something deeply wrong!");
            }
            return handler.getEncryptionResult();
        } catch (CryptoFailedException e) {
            throw e;
        } catch (GeneralSecurityException e) {
            throw new CryptoFailedException("Could not encrypt data with alias: " + alias, e);
        } catch (Throwable fail) {
            throw new CryptoFailedException("Unknown error with alias: " + alias +
                    ", error: " + fail.getMessage(), fail);
        }
    }

    @NonNull
    @Override
    public DecryptionResult decrypt(@NonNull String alias,
                                    @NonNull byte[] username,
                                    @NonNull byte[] password,
                                    @NonNull final SecurityLevel level, byte[] vector)
            throws CryptoFailedException {

        final NonInteractiveHandler handler = new NonInteractiveHandler();
        decrypt(handler, alias, username, password, level, vector);

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
                        @NonNull final SecurityLevel level,
                        byte[] vector)
            throws CryptoFailedException {

        throwIfInsufficientLevel(level);

        final String safeAlias = getDefaultAliasIfEmpty(alias, getDefaultAliasServiceName());
        final AtomicInteger retries = new AtomicInteger(1);

        try {
            // key is always NOT NULL otherwise GeneralSecurityException raised
            Key key = extractGeneratedKey(safeAlias, level, retries);

            // expected that KEY instance is extracted and we caught exception on decryptBytes operation
            @SuppressWarnings("ConstantConditions") final DecryptionContext context =
                    new DecryptionContext(safeAlias, key, password, username);

            final Cipher cipher = getCachedInstance();
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(vector));

            handler.askAccessPermissions(context, cipher);
        } catch (final Throwable fail) {
            // any other exception treated as a failure
            handler.onDecrypt(null, fail);
        }
    }

    //region Configuration
    @Override
    public String getCipherStorageName() {
        return KeychainModule.KnownCiphers.AES_BIOM;
    }

    /**
     * API23 is a requirement.
     */
    @Override
    public int getMinSupportedApiLevel() {
        return Build.VERSION_CODES.M;
    }

    /**
     * it can guarantee security levels up to SECURE_HARDWARE/SE/StrongBox
     */
    @Override
    public SecurityLevel securityLevel() {
        return SecurityLevel.SECURE_HARDWARE;
    }

    /**
     * Biometry is Not Supported.
     */
    @Override
    public boolean isBiometrySupported() {
        return true;
    }

    /**
     * AES.
     */
    @Override
    @NonNull
    protected String getEncryptionAlgorithm() {
        return ALGORITHM_AES;
    }

    /**
     * AES/CBC/PKCS7Padding
     */
    @NonNull
    @Override
    protected String getEncryptionTransformation() {
        return ENCRYPTION_TRANSFORMATION;
    }

    /**
     * {@inheritDoc}. Override for saving the compatibility with previous version of lib.
     */
    @Override
    public String getDefaultAliasServiceName() {
        return DEFAULT_SERVICE;
    }

    //endregion

    /**
     * Get encryption algorithm specification builder instance.
     */
    @NonNull
    @Override
    protected KeyGenParameterSpec.Builder getKeyGenSpecBuilder(@NonNull final String alias)
            throws GeneralSecurityException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw new KeyStoreAccessException("Unsupported API" + Build.VERSION.SDK_INT + " version detected.");
        }

        final int purposes = KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT;

        return new KeyGenParameterSpec.Builder(alias, purposes)
                .setBlockModes(BLOCK_MODE_CBC)
                .setEncryptionPaddings(PADDING_PKCS7)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(ENCRYPTION_KEY_SIZE);
    }

    /**
     * Get information about provided key.
     */
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

    /**
     * Try to generate key from provided specification.
     */
    @NonNull
    @Override
    protected Key generateKey(@NonNull final KeyGenParameterSpec spec) throws GeneralSecurityException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            throw new KeyStoreAccessException("Unsupported API" + Build.VERSION.SDK_INT + " version detected.");
        }

        final KeyGenerator generator = KeyGenerator.getInstance(getEncryptionAlgorithm(), KEYSTORE_TYPE);

        // initialize key generator
        generator.init(spec);

        return generator.generateKey();
    }

    //endregion

    //region Nested classes

    /**
     * Non interactive handler for decrypting the credentials.
     */
    public static class NonInteractiveHandler implements DecryptionResultHandler {
        private DecryptionResult result;
        private Throwable error;
        private EncryptionResult encryptionResult;

        @Override
        public void askAccessPermissions(@NonNull final DecryptionContext context, Cipher cipher) {
            final CryptoFailedException failure = new CryptoFailedException(
                    "Non interactive decryption mode.");

            onDecrypt(null, failure);
        }

        @Override
        public void onDecrypt(@Nullable final DecryptionResult decryptionResult,
                              @Nullable final Throwable error) {
            this.result = decryptionResult;
            this.error = error;
        }

        @Override
        public void onEncrypt(@Nullable EncryptionResult encryptionResult) {
            this.encryptionResult = encryptionResult;
        }

        @Override
        public void askAccessPermissionsEncryption(@NonNull EncryptContext context, Cipher cipher) {
            final CryptoFailedException failure = new CryptoFailedException(
                    "Non interactive encryption mode.");

            onDecrypt(null, failure);
        }

        @Nullable
        @Override
        public DecryptionResult getResult() {
            return result;
        }

        @Nullable
        @Override
        public EncryptionResult getEncryptionResult() {
            return encryptionResult;
        }

        @Nullable
        @Override
        public Throwable getError() {
            return error;
        }

        @Override
        public void waitResult() {
            /* do nothing, expected synchronized call in one thread */
        }
    }
    //endregion
}
