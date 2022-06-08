package com.oblador.keychain.decryptionHandler;

import android.security.keystore.UserNotAuthenticatedException;

import com.oblador.keychain.cipherStorage.CipherStorage;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface DecryptionResultJob {
    CipherStorage.DecryptionResult get() throws UserNotAuthenticatedException, IOException, GeneralSecurityException;
}
