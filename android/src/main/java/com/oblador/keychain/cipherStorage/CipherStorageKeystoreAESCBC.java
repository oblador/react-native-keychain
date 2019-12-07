package com.oblador.keychain.cipherStorage;

import android.annotation.TargetApi;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import androidx.annotation.NonNull;
import android.util.Log;

import com.oblador.keychain.SecurityLevel;
import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import android.security.keystore.StrongBoxUnavailableException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;

@TargetApi(Build.VERSION_CODES.M)
public class CipherStorageKeystoreAESCBC implements CipherStorage {
    public static final String TAG = "KeystoreAESCBC";
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
    private boolean retry = true;


    @Override
    public String getCipherStorageName() {
        return CIPHER_STORAGE_NAME;
    }

    @Override
    public int getMinSupportedApiLevel() {
        return Build.VERSION_CODES.M;
    }

    @Override
    public SecurityLevel securityLevel() {
        // it can guarantee security levels up to SECURE_HARDWARE/SE/StrongBox
        return SecurityLevel.SECURE_HARDWARE;
    }

    @Override
    public boolean supportsSecureHardware() {
        final String testKeyAlias = "AndroidKeyStore#supportsSecureHardware";

        try {
            SecretKey key = tryGenerateRegularSecurityKey(testKeyAlias);
            return validateKeySecurityLevel(SecurityLevel.SECURE_HARDWARE, key);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException e) {
            return false;
        } finally {
            try {
                removeKey(testKeyAlias);
            } catch (KeyStoreAccessException e) {
                Log.e(TAG, "Unable to remove temp key from keychain", e);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public EncryptionResult encrypt(@NonNull String service, @NonNull String username, @NonNull String password, SecurityLevel level, boolean useStrongBox) throws CryptoFailedException {
        service = getDefaultServiceIfEmpty(service);

        try {
            KeyStore keyStore = getKeyStoreAndLoad();

            if (!keyStore.containsAlias(service)) {
                generateKeyAndStoreUnderAlias(service, level, useStrongBox);
            }

            Key key = null;
            try {
                key = keyStore.getKey(service, null);
            } catch (UnrecoverableKeyException ex) {
                ex.printStackTrace();
                // Fix for android.security.KeyStoreException: Invalid key blob
                // more info: https://stackoverflow.com/questions/36488219/android-security-keystoreexception-invalid-key-blob/36846085#36846085
                if (retry) {
                    retry = false;
                    keyStore.deleteEntry(service);
                    return encrypt(service, username, password, level, useStrongBox);
                } else {
                    throw ex;
                }
            }

            byte[] encryptedUsername = encryptString(key, service, username);
            byte[] encryptedPassword = encryptString(key, service, password);

            retry = true;
            return new EncryptionResult(encryptedUsername, encryptedPassword, this);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException | UnrecoverableKeyException e) {
            throw new CryptoFailedException("Could not encrypt data for service " + service, e);
        } catch (KeyStoreException | KeyStoreAccessException e) {
            throw new CryptoFailedException("Could not access Keystore for service " + service, e);
        } catch (Exception e) {
            throw new CryptoFailedException("Unknown error: " + e.getMessage(), e);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean validateKeySecurityLevel(SecurityLevel level, SecretKey generatedKey) {
        return getSecurityLevel(generatedKey).satisfiesSafetyThreshold(level);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private SecurityLevel getSecurityLevel(SecretKey key) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(key.getAlgorithm(), KEYSTORE_TYPE);
            KeyInfo keyInfo;
            keyInfo = (KeyInfo) factory.getKeySpec(key, KeyInfo.class);
            return keyInfo.isInsideSecureHardware() ? SecurityLevel.SECURE_HARDWARE : SecurityLevel.SECURE_SOFTWARE;
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
            return SecurityLevel.ANY;
        }
    }

    private void generateKeyAndStoreUnderAlias(@NonNull String service, SecurityLevel requiredLevel, boolean useStrongBox) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, CryptoFailedException {
        // Firstly, try to generate the key as safe as possible (strongbox).
        // see https://developer.android.com/training/articles/keystore#HardwareSecurityModule
        SecretKey secretKey = tryGenerateStrongBoxSecurityKey(service, useStrongBox);
        if (secretKey == null) {
            // If that is not possible, we generate the key in a regular way
            // (it still might be generated in hardware, but not in StrongBox)
            secretKey = tryGenerateRegularSecurityKey(service);
        }

        if (!validateKeySecurityLevel(requiredLevel, secretKey)) {
            try {
                removeKey(service);
            } catch (KeyStoreAccessException e) {
                Log.e(TAG, "Unable to remove key from keychain", e);
            }

            throw new CryptoFailedException("Cannot generate keys with required security guarantees");
        }
    }

    @Override
    public DecryptionResult decrypt(@NonNull String service, @NonNull byte[] username, @NonNull byte[] password) throws CryptoFailedException {
        service = getDefaultServiceIfEmpty(service);

        try {
            KeyStore keyStore = getKeyStoreAndLoad();

            Key key = keyStore.getKey(service, null);
            if (key == null) {
              throw new CryptoFailedException("The provided service/key could not be found in the Keystore");
            }

            String decryptedUsername = decryptBytes(key, username);
            String decryptedPassword = decryptBytes(key, password);

            return new DecryptionResult(decryptedUsername, decryptedPassword, getSecurityLevel((SecretKey) key));
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
            // write initialization vector to the beginning of the stream
            byte[] iv = cipher.getIV();
            outputStream.write(iv, 0, iv.length);
            // encrypt the value using a CipherOutputStream
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
            cipherOutputStream.write(value.getBytes("UTF-8"));
            cipherOutputStream.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new CryptoFailedException("Could not encrypt value for service " + service + ", message: " + e.getMessage(), e);
        }
    }

    private String decryptBytes(Key key, byte[] bytes) throws CryptoFailedException {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            // read the initialization vector from the beginning of the stream
            IvParameterSpec ivParams = readIvFromStream(inputStream);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParams);
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
        } catch (Exception e) {
            throw new CryptoFailedException("Could not decrypt bytes: " + e.getMessage(), e);
        }
    }

    private IvParameterSpec readIvFromStream(ByteArrayInputStream inputStream) {
        byte[] iv = new byte[16];
        inputStream.read(iv, 0, iv.length);
        return new IvParameterSpec(iv);
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

    @TargetApi(Build.VERSION_CODES.P)
    private SecretKey tryGenerateStrongBoxSecurityKey(String service, boolean useStrongBox) throws NoSuchAlgorithmException,
      InvalidAlgorithmParameterException, NoSuchProviderException {
        // If user set useStrongBox as false skip StrongBox usage
        if (!useStrongBox) {
            return null;
        }
        // StrongBox is only supported on Android P and higher
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null;
        }
        try {
            return generateKey(getKeyGenSpecBuilder(service).setIsStrongBoxBacked(true).build());
        } catch (Exception e) {
          if (e instanceof StrongBoxUnavailableException) {
            Log.i(TAG, "StrongBox is unavailable on this device");
          } else {
            Log.e(TAG, "An error occurred when trying to generate a StrongBoxSecurityKey: " + e.getMessage());
          }
          return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private SecretKey tryGenerateRegularSecurityKey(String service) throws NoSuchAlgorithmException,
      InvalidAlgorithmParameterException, NoSuchProviderException {
        return generateKey(getKeyGenSpecBuilder(service).build());
    }

    // returns true if the key was generated successfully
    @TargetApi(Build.VERSION_CODES.M)
    private SecretKey generateKey(KeyGenParameterSpec spec) throws NoSuchProviderException,
      NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyGenerator generator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, KEYSTORE_TYPE);
        generator.init(spec);
        return generator.generateKey();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private KeyGenParameterSpec.Builder getKeyGenSpecBuilder(String service) {
        return new KeyGenParameterSpec.Builder(
                service,
                KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT)
            .setBlockModes(ENCRYPTION_BLOCK_MODE)
            .setEncryptionPaddings(ENCRYPTION_PADDING)
            .setRandomizedEncryptionRequired(true)
            //.setUserAuthenticationRequired(true) // Will throw InvalidAlgorithmParameterException if there is no fingerprint enrolled on the device
            .setKeySize(ENCRYPTION_KEY_SIZE);
    }
}
