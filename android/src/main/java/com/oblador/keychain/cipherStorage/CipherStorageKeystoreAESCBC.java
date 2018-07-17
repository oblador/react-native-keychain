package com.oblador.keychain.cipherStorage;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

import com.facebook.react.bridge.ReactApplicationContext;

import static android.R.attr.*;


@TargetApi(Build.VERSION_CODES.M)
public class CipherStorageKeystoreAESCBC extends FingerprintManager.AuthenticationCallback implements CipherStorage {
>>>>>>> Added fingerprintManager for Android biometry support
    public static final String CIPHER_STORAGE_NAME = "KeystoreAESCBC";
    public static final String DEFAULT_SERVICE = "RN_KEYCHAIN_DEFAULT_ALIAS";
    public static final String KEYSTORE_TYPE = "AndroidKeyStore";
    public static final String ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES;
    public static final String ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC;
    public static final String ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7;
    public static final String ENCRYPTION_TRANSFORMATION =
            ENCRYPTION_ALGORITHM + "/" +
                    ENCRYPTION_BLOCK_MODE + "/" +
                    ENCRYPTION_PADDING;
    public static final int ENCRYPTION_KEY_SIZE = 256;


    private CancellationSignal mFingerprintCancellationSignal;
    private FingerprintManager mFingerprintManager;
    private KeyguardManager mKeyguardManager;
    private Context mContext;
    private EncryptionResultHandler mEncryptionResultHandler;
    private DecryptionResultHandler mDecryptionResultHandler;

    private String mUsername;
    private String mPassword;
    private byte[] mUsernameBytes;
    private byte[] mPasswordBytes;
    private Key mKey;

    public CipherStorageKeystoreAESCBC(ReactApplicationContext reactContext) {
        mContext = (Context) reactContext;
        mFingerprintManager = (FingerprintManager) reactContext.getSystemService(Context.FINGERPRINT_SERVICE);
        mKeyguardManager = (KeyguardManager) reactContext.getSystemService(Context.KEYGUARD_SERVICE);
    }

    @Override
    public void onAuthenticationError(int errMsgId,
                                      CharSequence errString) {
        if (mEncryptionResultHandler != null) {
            mEncryptionResultHandler.onEncryptionResult(null, null, errString.toString());
        }

        if (mDecryptionResultHandler != null) {
            mDecryptionResultHandler.onDecryptionResult(null, null, errString.toString());
        }
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId,
                                     CharSequence helpString) {
        if (mEncryptionResultHandler != null) {
            mEncryptionResultHandler.onEncryptionResult(null, helpString.toString(), null);
        }

        if (mDecryptionResultHandler != null) {
            mDecryptionResultHandler.onDecryptionResult(null, helpString.toString(), null);
        }
    }

    @Override
    public void onAuthenticationFailed() {
        if (mEncryptionResultHandler != null) {
            mEncryptionResultHandler.onEncryptionResult(null, null, "Authentication failed.");
            mEncryptionResultHandler = null;
        }

        if (mDecryptionResultHandler != null) {
            mDecryptionResultHandler.onDecryptionResult(null, null, "Authentication failed.");
            mDecryptionResultHandler = null;
        }
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        if (mEncryptionResultHandler != null) {
            try {
                byte[] encryptedUsername = encryptString(mUsername);
                byte[] encryptedPassword = encryptString(mPassword);
                mEncryptionResultHandler.onEncryptionResult(new EncryptionResult(encryptedUsername, encryptedPassword, this), null, null);
            } catch (Exception e) {
                mEncryptionResultHandler.onEncryptionResult(null, null, e.getMessage());
            }
            mEncryptionResultHandler = null;
        }

        if (mDecryptionResultHandler != null) {
            try {
                String decryptedUsername = decryptBytes(mUsernameBytes);
                String decryptedPassword = decryptBytes(mPasswordBytes);
                mDecryptionResultHandler.onDecryptionResult(new DecryptionResult(decryptedUsername, decryptedPassword), null, null);
            } catch (Exception e) {
                mEncryptionResultHandler.onEncryptionResult(null, null, e.getMessage());
            }
            mDecryptionResultHandler = null;
        }
    }


    @Override
    public String getCipherStorageName() {
        return CIPHER_STORAGE_NAME;
    }

    @Override
    public int getMinSupportedApiLevel() {
        return Build.VERSION_CODES.M;
    }

    @Override
    public void encrypt(@NonNull EncryptionResultHandler encryptionResultHandler, @NonNull String service, @NonNull String username, @NonNull String password, @NonNull boolean useBiometry) throws CryptoFailedException {
        service = getDefaultServiceIfEmpty(service);
        mUsername = username;
        mPassword = password;
        mEncryptionResultHandler = encryptionResultHandler;
        try {
            KeyStore keyStore = getKeyStoreAndLoad();

            if (!keyStore.containsAlias(service)) {
                generateKeyAndStoreUnderAlias(service, useBiometry);Added fingerprintManager for Android biometry support
            }

            mKey = keyStore.getKey(service, null);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
            try {
                // We test if a cipher could be unlocked straight away.
                cipher.init(Cipher.ENCRYPT_MODE, mKey);
            } catch (UserNotAuthenticatedException e) {
                if (useBiometry && !this.startFingerprintAuthentication(cipher)) {
                    throw new CryptoFailedException("Could not start fingerprint Authentication", e);
                }
                return;
            }

            byte[] encryptedUsername = encryptString(mUsername);
            byte[] encryptedPassword = encryptString(mPassword);
            mEncryptionResultHandler.onEncryptionResult(new EncryptionResult(encryptedUsername, encryptedPassword, this), null, null);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException | UnrecoverableKeyException e) {
            throw new CryptoFailedException("Could not encrypt data for service " + service, e);
        } catch (KeyStoreException | KeyStoreAccessException e) {
            throw new CryptoFailedException("Could not access Keystore for service " + service, e);
        } catch (InvalidKeyException e) {
            throw new CryptoFailedException("Invalid key exception: " + e.getMessage(), e);
        } catch (NoSuchPaddingException e) {
            throw new CryptoFailedException("No such padding error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new CryptoFailedException("Unknown error: " + e.getMessage(), e);
        }
    }

    private boolean startFingerprintAuthentication(Cipher cipher) {
        if (mKeyguardManager.isKeyguardSecure() &&
            ActivityCompat.checkSelfPermission(mContext, Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED) {
            mFingerprintCancellationSignal = new CancellationSignal();
            mFingerprintManager.authenticate(null,
                    mFingerprintCancellationSignal,
                    0,     // flags
                    this,  // authentication callback
                    null); // handler

            return true;
        }

        return false;
    }

    private void generateKeyAndStoreUnderAlias(@NonNull String service, @NonNull boolean useBiometry) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        KeyGenParameterSpec.Builder specBuilder = new KeyGenParameterSpec.Builder(
                service,
                KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT)
                .setBlockModes(ENCRYPTION_BLOCK_MODE)
                .setEncryptionPaddings(ENCRYPTION_PADDING)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(ENCRYPTION_KEY_SIZE);
        if (useBiometry) {
            specBuilder.setUserAuthenticationRequired(true) // Will throw InvalidAlgorithmParameterException if there is no fingerprint enrolled on the device
                    .setUserAuthenticationValidityDurationSeconds(30);
        }

        AlgorithmParameterSpec spec = specBuilder.build();

        KeyGenerator generator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, KEYSTORE_TYPE);
        generator.init(spec);

        generator.generateKey();
    }

    @Override
    public void decrypt(@NonNull DecryptionResultHandler decryptionResultHandler, @NonNull String service, @NonNull byte[] username, @NonNull byte[] password, @NonNull boolean useBiometry) throws CryptoFailedException {
        service = getDefaultServiceIfEmpty(service);

        try {
            KeyStore keyStore = getKeyStoreAndLoad();
            mKey = keyStore.getKey(service, null);
            Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
            try {
                cipher.init(Cipher.DECRYPT_MODE, mKey);
            } catch (UserNotAuthenticatedException e) {
                if (useBiometry && !this.startFingerprintAuthentication(cipher)) {
                    throw new CryptoFailedException("Could not start fingerprint Authentication", e);
                }
                return;
            }

            String decryptedUsername = decryptBytes(username);
            String decryptedPassword = decryptBytes(password);
            mDecryptionResultHandler.onDecryptionResult(new DecryptionResult(decryptedUsername, decryptedPassword), null, null);
        } catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
            throw new CryptoFailedException("Could not get key from Keystore", e);
        } catch (KeyStoreAccessException e) {
            throw new CryptoFailedException("Could not access Keystore", e);
        } catch (InvalidKeyException e) {
            throw new CryptoFailedException("Invalid key exception: " + e.getMessage(), e);
        } catch (NoSuchPaddingException e) {
            throw new CryptoFailedException("No such padding error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new CryptoFailedException("Unknown error: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeKey(@NonNull String service) throws KeyStoreAccessException {
        service = getDefaultServiceIfEmpty(service);

        try {
            KeyStore keyStore = getKeyStoreAndLoad();

            if (keyStore.containsAlias(service)) {
                keyStore.deleteEntry(service);
            }
        } catch (KeyStoreException e) {
            throw new KeyStoreAccessException("Failed to access Keystore", e);
        } catch (Exception e) {
            throw new KeyStoreAccessException("Unknown error " + e.getMessage(), e);
        }
    }

    private byte[] encryptString(String value) throws CryptoFailedException {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, mKey);
            byte[] encryptedBytes = cipher.doFinal(value.getBytes("UTF-8"));
            return encryptedBytes;
        } catch (Exception e) {
            throw new CryptoFailedException("Could not encrypt value for service", e);
        }
    }

    private String decryptBytes(byte[] bytes) throws CryptoFailedException {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, mKey);
            byte[] decryptedBytes = cipher.doFinal(bytes);
            return new String(decryptedBytes, Charset.forName("UTF-8"));
        } catch (Exception e) {
            throw new CryptoFailedException("Could not decrypt bytes", e);
        }
    }

    private KeyStore getKeyStoreAndLoad() throws KeyStoreException, KeyStoreAccessException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            keyStore.load(null);
            return keyStore;
        } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
            throw new KeyStoreAccessException("Could not access Keystore", e);
        }
    }

    @NonNull
    private String getDefaultServiceIfEmpty(@NonNull String service) {
        return service.isEmpty() ? DEFAULT_SERVICE : service;
    }
}
