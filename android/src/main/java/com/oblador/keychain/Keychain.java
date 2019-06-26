package com.oblador.keychain;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.oblador.keychain.exceptions.CryptoFailedException;
import com.oblador.keychain.exceptions.EmptyParameterException;
import com.oblador.keychain.exceptions.KeyStoreAccessException;

/**
 * Keychain is an Keychain module which abstracts an encrypted the keychain related lookups and updates.
 * <p/>
 * <p>
 * The flow is the following:
 * <pre>
 * setGenericPassword(service1, "user1", "pass1") -> get service1 keys -> encrypt user/pass -> store user/pass.
 * </pre>
 *
 * @author Miroslav Genov <miroslav.genov@clouway.com>
 */
public interface Keychain {
  /**
   * Gets the SecurityLevel of the Keychain, e.g software, hardware and etc.
   * @return the security level
   */
  SecurityLevel getSecurityLevel();

  /**
   * Gets the {@link ServiceCredentials} associated with the provided service
   *
   * @param service the service to which credentials are associated
   * @return the service credentials associated with the provided service
   * @throws CryptoFailedException in case of crypto failure
   * @throws KeyStoreAccessException in case of error during accessing the keystore
   */
  @Nullable
  ServiceCredentials getGenericPasswordForOptions(String service) throws CryptoFailedException, KeyStoreAccessException;

  /**
   * Checks whether the provided credentials exists.
   * @param server the service id
   * @return true if credentials exists and false in other case
   */
  boolean hasInternetCredentialsForServer(@NonNull String server);

  /**
   * Sets new credentials of the provided service
   * @param service the name of the service
   * @param username the username value
   * @param password the password value
   * @param minimumSecurityLevel the minimum security level
   * @throws EmptyParameterException is password is empty
   * @throws CryptoFailedException in case of crypto failure
   */
  void setGenericPasswordForOptions(String service, String username, String password, String minimumSecurityLevel) throws EmptyParameterException, CryptoFailedException;

  /**
   * Resets credentials of a given service.
   *
   * @param service the service of which credentials to be reset
   * @throws KeyStoreAccessException in case of keystore access error
   */
  void resetGenericPasswordForOptions(String service) throws KeyStoreAccessException;
}
