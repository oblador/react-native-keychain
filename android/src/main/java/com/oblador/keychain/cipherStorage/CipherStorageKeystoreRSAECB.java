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
import com.facebook.react.bridge.ReactApplicationContext;

import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;

@TargetApi(Build.VERSION_CODES.M)
public class CipherStorageKeystoreRSAECB extends FingerprintManager.AuthenticationCallback implements CipherStorage {
    public static final String CIPHER_STORAGE_NAME = "KeystoreAESCBC";
    public static final String DEFAULT_SERVICE = "RN_KEYCHAIN_DEFAULT_ALIAS";
    public static final String KEYSTORE_TYPE = "AndroidKeyStore";
    public static final String ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_RSA;
    public static final String ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_ECB;
    public static final String ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1;
    public static final String ENCRYPTION_TRANSFORMATION =
            ENCRYPTION_ALGORITHM + "/" +
                    ENCRYPTION_BLOCK_MODE + "/" +
                    ENCRYPTION_PADDING;
    public static final int ENCRYPTION_KEY_SIZE = 1024;

    private CancellationSignal mFingerprintCancellationSignal;
    private FingerprintManager mFingerprintManager;
    private KeyguardManager mKeyguardManager;
    private Context mContext;

    class CipherDecryptionParams {
        public final DecryptionResultHandler resultHandler;
        public final Key key;
        public final byte[] username;
        public final byte[] password;

        public CipherDecryptionParams(DecryptionResultHandler handler, Key key, byte[] username, byte[] password) {
            this.resultHandler = handler;
            this.key = key;
            this.username = username;
            this.password = password;
        }
    }

    private CipherDecryptionParams mDecryptParams;

    public CipherStorageKeystoreRSAECB(ReactApplicationContext reactContext) {
        mContext = (Context) reactContext;
        mFingerprintManager = (FingerprintManager) reactContext.getSystemService(Context.FINGERPRINT_SERVICE);
        mKeyguardManager = (KeyguardManager) reactContext.getSystemService(Context.KEYGUARD_SERVICE);
    }

    @Override
    public void onAuthenticationError(int errMsgId,
                                      CharSequence errString) {
        if (mDecryptParams != null && mDecryptParams.resultHandler != null) {
            mDecryptParams.resultHandler.onDecrypt(null, null, errString.toString());
            mFingerprintCancellationSignal.cancel();
            mDecryptParams = null;
        }
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId,
                                     CharSequence helpString) {
        if (mDecryptParams != null && mDecryptParams.resultHandler != null) {
            mDecryptParams.resultHandler.onDecrypt(null, helpString.toString(), null);
        }
    }

    @Override
    public void onAuthenticationFailed() {
        if (mDecryptParams != null && mDecryptParams.resultHandler != null) {
            mDecryptParams.resultHandler.onDecrypt(null, null, "Authentication failed.");
            mFingerprintCancellationSignal.cancel();
            mDecryptParams = null;
        }
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        if (mDecryptParams != null && mDecryptParams.resultHandler != null) {
            try {
                String decryptedUsername = decryptBytes(mDecryptParams.key, mDecryptParams.username);
                String decryptedPassword = decryptBytes(mDecryptParams.key, mDecryptParams.password);
                mDecryptParams.resultHandler.onDecrypt(new DecryptionResult(decryptedUsername, decryptedPassword), null, null);
            } catch (Exception e) {
                mDecryptParams.resultHandler.onDecrypt(null, null, e.getMessage());
            }
            mDecryptParams = null;
        }
    }


    private boolean startFingerprintAuthentication() {
        if (mKeyguardManager.isKeyguardSecure() &&
                mContext.checkSelfPermission(Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED) {
            // If we have a previous cancellationSignal, cancel it.
            if (mFingerprintCancellationSignal != null) {
                mFingerprintCancellationSignal.cancel();
            }
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

    @Override
    public String getCipherStorageName() {
        return CIPHER_STORAGE_NAME;
    }

    @Override
    public boolean getCipherBiometrySupported() {
        return true;
    }

    @Override
    public int getMinSupportedApiLevel() {
        return Build.VERSION_CODES.M;
    }

    @Override
    public EncryptionResult encrypt(@NonNull String service, @NonNull String username, @NonNull String password) throws CryptoFailedException {
        service = getDefaultServiceIfEmpty(service);

        try {
            KeyStore keyStore = getKeyStoreAndLoad();

            if (!keyStore.containsAlias(service)) {
                generateKeyAndStoreUnderAlias(service);
            }

            KeyFactory keyFactory = KeyFactory.getInstance(ENCRYPTION_ALGORITHM);
            PublicKey publicKey = keyStore.getCertificate(service).getPublicKey();
            KeySpec spec = new X509EncodedKeySpec(publicKey.getEncoded());
            Key key = keyFactory.generatePublic(spec);

            byte[] encryptedUsername = encryptString(key, service, username);
            byte[] encryptedPassword = encryptString(key, service, password);

            return new EncryptionResult(encryptedUsername, encryptedPassword, this);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException e) {
            throw new CryptoFailedException("Could not encrypt data for service " + service, e);
        } catch (KeyStoreException | KeyStoreAccessException e) {
            throw new CryptoFailedException("Could not access Keystore for service " + service, e);
        } catch (Exception e) {
            throw new CryptoFailedException("Unknown error: " + e.getMessage(), e);
        }
    }

    private void generateKeyAndStoreUnderAlias(@NonNull String service) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        AlgorithmParameterSpec spec = new KeyGenParameterSpec.Builder(
                service,
                KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT)
                .setBlockModes(ENCRYPTION_BLOCK_MODE)
                .setEncryptionPaddings(ENCRYPTION_PADDING)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(30)
                .setKeySize(ENCRYPTION_KEY_SIZE)
                .build();

        KeyPairGenerator generator;
        generator = KeyPairGenerator.getInstance(ENCRYPTION_ALGORITHM, KEYSTORE_TYPE);
        generator.initialize(spec);
        generator.generateKeyPair();
    }

    @Override
    public void decrypt(@NonNull DecryptionResultHandler decryptionResultHandler, @NonNull String service, @NonNull byte[] username, @NonNull byte[] password) throws CryptoFailedException {
        service = getDefaultServiceIfEmpty(service);

        try {
            KeyStore keyStore = getKeyStoreAndLoad();

            Key key = keyStore.getKey(service, null);

            String decryptedUsername = null;
            String decryptedPassword = null;
            try {
                decryptedUsername = decryptBytes(key, username);
                decryptedPassword = decryptBytes(key, password);
            } catch (UserNotAuthenticatedException e) {
                mDecryptParams = new CipherDecryptionParams(decryptionResultHandler, key, username, password);
                if (!this.startFingerprintAuthentication()) {
                    throw new CryptoFailedException("Could not start fingerprint Authentication", e);
                }
                return;
            }

            decryptionResultHandler.onDecrypt(new DecryptionResult(decryptedUsername, decryptedPassword), null, null);
        } catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
            throw new CryptoFailedException("Could not get key from Keystore", e);
        } catch (KeyStoreAccessException e) {
            throw new CryptoFailedException("Could not access Keystore", e);
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

    private byte[] encryptString(Key key, String service, String value) throws CryptoFailedException {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // encrypt the value using a CipherOutputStream
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
            cipherOutputStream.write(value.getBytes("UTF-8"));
            cipherOutputStream.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new CryptoFailedException("Could not encrypt value for service " + service, e);
        }
    }

    private String decryptBytes(Key key, byte[] bytes) throws CryptoFailedException, UserNotAuthenticatedException {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            // read the initialization vector from the beginning of the stream
            cipher.init(Cipher.DECRYPT_MODE, key);
            // decrypt the bytes using a CipherInputStream
            CipherInputStream cipherInputStream = new CipherInputStream(
                    inputStream, cipher);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while (true) {
                int n = cipherInputStream.read(buffer, 0, buffer.length);
                if (n <= 0) {
                    break;
                }
                output.write(buffer, 0, n);
            }
            return new String(output.toByteArray(), Charset.forName("UTF-8"));
        } catch (UserNotAuthenticatedException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
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
