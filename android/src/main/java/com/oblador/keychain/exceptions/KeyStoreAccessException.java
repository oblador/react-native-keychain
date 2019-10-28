package com.oblador.keychain.exceptions;

import java.security.GeneralSecurityException;

public class KeyStoreAccessException extends GeneralSecurityException {
  public KeyStoreAccessException(final String message) {
    super(message);
  }

  public KeyStoreAccessException(final String message, final Throwable t) {
    super(message, t);
  }
}
