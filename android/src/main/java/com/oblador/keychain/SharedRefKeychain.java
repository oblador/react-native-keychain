package com.oblador.keychain;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.oblador.keychain.cipherStorage.CipherStorage;
import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.EmptyParameterException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

import java.util.Map;

/**
 * SharedRefKeychain an {@link Keychain} implementation which stores the the encrypted values as shared
 * preferences maintained by the helper class {@link PrefsStorage}.
 *
 * @author Miroslav Genov <miroslav.genov@clouway.com>
 */
class SharedRefKeychain implements Keychain {
    private static final String EMPTY_STRING = "";

    private final PrefsStorage prefsStorage;
    private final Map<String, CipherStorage> nameToCipher;


    SharedRefKeychain(Context context, Map<String, CipherStorage> nameToCipher) {
        this.nameToCipher = nameToCipher;
        this.prefsStorage = new PrefsStorage(context);
    }

    @Override
    public SecurityLevel getSecurityLevel() {
        try {
            CipherStorage storage = getCipherStorageForCurrentAPILevel();
            if (!storage.securityLevel().satisfiesSafetyThreshold(SecurityLevel.SECURE_SOFTWARE)) {
                return SecurityLevel.ANY;
            }

            if (isSecureHardwareAvailable()) {
                return SecurityLevel.SECURE_HARDWARE;
            } else {
                return SecurityLevel.SECURE_SOFTWARE;
            }
        } catch (CryptoFailedException e) {
            return SecurityLevel.ANY;
        }
    }

    @Override
    @Nullable
    public ServiceCredentials getGenericPasswordForOptions(String service) throws CryptoFailedException, KeyStoreAccessException {
        String targetService = getDefaultServiceIfNull(service);

        CipherStorage currentCipherStorage = getCipherStorageForCurrentAPILevel();

        PrefsStorage.ResultSet resultSet = prefsStorage.getEncryptedEntry(targetService);
        if (resultSet == null) {
            return null;
        }

        final CipherStorage.DecryptionResult decryptionResult = decryptCredentials(targetService, currentCipherStorage, resultSet);

        return new ServiceCredentials(targetService, decryptionResult.username, decryptionResult.password);
    }

    @Override
    public boolean hasInternetCredentialsForServer(@NonNull String server) {
        final String defaultService = getDefaultServiceIfNull(server);
        PrefsStorage.ResultSet resultSet = prefsStorage.getEncryptedEntry(defaultService);
        return resultSet != null;
    }

    @Override
    public void setGenericPasswordForOptions(String service, String username, String password, String minimumSecurityLevel) throws EmptyParameterException, CryptoFailedException {
        SecurityLevel level = SecurityLevel.valueOf(minimumSecurityLevel);
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new EmptyParameterException("you passed empty or null username/password");
        }
        service = getDefaultServiceIfNull(service);

        CipherStorage currentCipherStorage = getCipherStorageForCurrentAPILevel();
        validateCipherStorageSecurityLevel(currentCipherStorage, level);

        CipherStorage.EncryptionResult result = currentCipherStorage.encrypt(service, username, password, level);
        prefsStorage.storeEncryptedEntry(service, result);
    }

    @Override
    public void resetGenericPasswordForOptions(String service) throws KeyStoreAccessException {
        service = getDefaultServiceIfNull(service);

        // First we clean up the cipher storage (using the cipher storage that was used to store the entry)
        PrefsStorage.ResultSet resultSet = prefsStorage.getEncryptedEntry(service);
        if (resultSet != null) {
            CipherStorage cipherStorage = getCipherStorageByName(resultSet.cipherStorageName);
            if (cipherStorage != null) {
                cipherStorage.removeKey(service);
            }
        }
        // And then we remove the entry in the shared preferences
        prefsStorage.removeEntry(service);
    }


    private CipherStorage.DecryptionResult decryptCredentials(String service, CipherStorage currentCipherStorage, PrefsStorage.ResultSet resultSet) throws CryptoFailedException, KeyStoreAccessException {
        if (resultSet.cipherStorageName.equals(currentCipherStorage.getCipherStorageName())) {
            // The encrypted data is encrypted using the current CipherStorage, so we just decrypt and return
            return currentCipherStorage.decrypt(service, resultSet.usernameBytes, resultSet.passwordBytes);
        }

        // The encrypted data is encrypted using an older CipherStorage, so we need to decrypt the data first, then encrypt it using the current CipherStorage, then store it again and return
        CipherStorage oldCipherStorage = getCipherStorageByName(resultSet.cipherStorageName);
        // decrypt using the older cipher storage

        CipherStorage.DecryptionResult decryptionResult = oldCipherStorage.decrypt(service, resultSet.usernameBytes, resultSet.passwordBytes);
        // encrypt using the current cipher storage


        migrateCipherStorage(service, currentCipherStorage, oldCipherStorage, decryptionResult);


        return decryptionResult;
    }

    private void migrateCipherStorage(String service, CipherStorage newCipherStorage, CipherStorage oldCipherStorage, CipherStorage.DecryptionResult decryptionResult) throws KeyStoreAccessException, CryptoFailedException {
        // don't allow to degrade security level when transferring, the new storage should be as safe as the old one.
        CipherStorage.EncryptionResult encryptionResult = newCipherStorage.encrypt(service, decryptionResult.username, decryptionResult.password, decryptionResult.getSecurityLevel());
        // store the encryption result
        prefsStorage.storeEncryptedEntry(service, encryptionResult);
        // clean up the old cipher storage
        oldCipherStorage.removeKey(service);
    }


    private void validateCipherStorageSecurityLevel(CipherStorage cipherStorage, SecurityLevel requiredLevel) throws CryptoFailedException {
        if (cipherStorage.securityLevel().satisfiesSafetyThreshold(requiredLevel)) {
            return;
        }

        throw new CryptoFailedException(
                String.format(
                        "Cipher Storage is too weak. Required security level is: %s, but only %s is provided",
                        requiredLevel.name(),
                        cipherStorage.securityLevel().name()));
    }

    private boolean isSecureHardwareAvailable() {
        try {
            return getCipherStorageForCurrentAPILevel().supportsSecureHardware();
        } catch (CryptoFailedException e) {
            return false;
        }
    }

    // The "Current" CipherStorage is the cipherStorage with the highest API level that is lower than or equal to the current API level
    private CipherStorage getCipherStorageForCurrentAPILevel() throws CryptoFailedException {
        int currentAPILevel = Build.VERSION.SDK_INT;
        CipherStorage currentCipherStorage = null;
        for (CipherStorage cipherStorage : nameToCipher.values()) {
            int cipherStorageAPILevel = cipherStorage.getMinSupportedApiLevel();
            // Is the cipherStorage supported on the current API level?
            boolean isSupported = (cipherStorageAPILevel <= currentAPILevel);
            if (!isSupported) {
                continue;
            }
            // Is the API level better than the one we previously selected (if any)?
            if (currentCipherStorage == null || cipherStorageAPILevel > currentCipherStorage.getMinSupportedApiLevel()) {
                currentCipherStorage = cipherStorage;
            }
        }
        if (currentCipherStorage == null) {
            throw new CryptoFailedException("Unsupported Android SDK " + Build.VERSION.SDK_INT);
        }
        return currentCipherStorage;
    }

    private CipherStorage getCipherStorageByName(String cipherStorageName) {
        return nameToCipher.get(cipherStorageName);
    }

    @NonNull
    private String getDefaultServiceIfNull(String service) {
        return service == null ? EMPTY_STRING : service;
    }
}
