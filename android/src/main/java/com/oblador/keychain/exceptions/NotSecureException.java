package com.oblador.keychain.exceptions;

public class NotSecureException extends CryptoFailedException {
  public NotSecureException(String message) {
    super(message);
  }

  public NotSecureException(String message, Throwable t) {
    super(message, t);
  }
}
