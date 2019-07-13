package com.oblador.keychain.cipherStorage;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.StrongBoxUnavailableException;
import android.support.annotation.NonNull;
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
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;

@TargetApi(Build.VERSION_CODES.M)
public abstract class CipherStorageKeystoreBase implements CipherStorage {
    public static final String TAG = "Keystore";
    public static final String DEFAULT_SERVICE = "RN_KEYCHAIN_DEFAULT_ALIAS";
    public static final String KEYSTORE_TYPE = "AndroidKeyStore";

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
    protected boolean validateKeySecurityLevel(SecurityLevel level, SecretKey generatedKey) {
        return getSecurityLevel(generatedKey).satisfiesSafetyThreshold(level);
    }

    @TargetApi(Build.VERSION_CODES.M)
    protected SecurityLevel getSecurityLevel(SecretKey key) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(key.getAlgorithm(), KEYSTORE_TYPE);
            KeyInfo keyInfo;
            keyInfo = (KeyInfo) factory.getKeySpec(key, KeyInfo.class);
            return keyInfo.isInsideSecureHardware() ? SecurityLevel.SECURE_HARDWARE : SecurityLevel.SECURE_SOFTWARE;
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
            return SecurityLevel.ANY;
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

    protected KeyStore getKeyStoreAndLoad() throws KeyStoreException, KeyStoreAccessException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            keyStore.load(null);
            return keyStore;
        } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
            throw new KeyStoreAccessException("Could not access Keystore", e);
        }
    }

    @NonNull
    protected String getDefaultServiceIfEmpty(@NonNull String service) {
        return service.isEmpty() ? DEFAULT_SERVICE : service;
    }

    protected void generateKeyAndStoreUnderAlias(@NonNull String service, SecurityLevel requiredLevel) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, CryptoFailedException {
        // Firstly, try to generate the key as safe as possible (strongbox).
        // see https://developer.android.com/training/articles/keystore#HardwareSecurityModule
        SecretKey secretKey = tryGenerateStrongBoxSecurityKey(service);
        if (secretKey == null) {
            // If that is not possible, we generate the key in a regular way
            // (it still might be generated in hardware, but not in StrongBox)
            secretKey = tryGenerateRegularSecurityKey(service);
        }

        if(!validateKeySecurityLevel(requiredLevel, secretKey)) {
            throw new CryptoFailedException("Cannot generate keys with required security guarantees");
        }
    }
 
    @TargetApi(Build.VERSION_CODES.P)
    protected SecretKey tryGenerateStrongBoxSecurityKey(String service) throws NoSuchAlgorithmException,
      InvalidAlgorithmParameterException, NoSuchProviderException {
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
    protected SecretKey tryGenerateRegularSecurityKey(String service) throws NoSuchAlgorithmException,
      InvalidAlgorithmParameterException, NoSuchProviderException {
        return generateKey(getKeyGenSpecBuilder(service).build());
    }

    // returns true if the key was generated successfully
    @TargetApi(Build.VERSION_CODES.M)
    protected abstract SecretKey generateKey(KeyGenParameterSpec spec) throws NoSuchProviderException,
      NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    @TargetApi(Build.VERSION_CODES.M)
    protected abstract KeyGenParameterSpec.Builder getKeyGenSpecBuilder(String service);
}
