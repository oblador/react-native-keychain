package com.oblador.keychain;

import android.content.Context;

import com.oblador.keychain.cipherStorage.CipherStorage;
import com.oblador.keychain.cipherStorage.CipherStorageFacebookConceal;
import com.oblador.keychain.cipherStorage.CipherStorageKeystoreAESCBC;

import java.util.LinkedHashMap;

/**
 * Keychains is a factory class used to create Keychain objects used for storing of secrets.
 *
 * @author Miroslav Genov <miroslav.genov@clouway.com>
 */
public final class Keychains {

  /**
   * Creates a new {@link Keychain} instance that is using shared preferences as storage.
   *
   * @param context the android context
   * @return the newly created Keychain instance
   */
  public static Keychain create(Context context) {
    final CipherStorage facebookConcealCipherStorage = new CipherStorageFacebookConceal(context);
    final CipherStorage keystoreCipherStorage = new CipherStorageKeystoreAESCBC();

    return new SharedRefKeychain(context, new LinkedHashMap<String, CipherStorage>() {{
      put(facebookConcealCipherStorage.getCipherStorageName(), facebookConcealCipherStorage);
      put(keystoreCipherStorage.getCipherStorageName(), keystoreCipherStorage);
    }});
  }

}
