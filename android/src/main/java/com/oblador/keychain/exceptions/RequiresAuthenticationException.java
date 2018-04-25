package com.oblador.keychain.exceptions;

public class RequiresAuthenticationException extends CryptoFailedException {
  public RequiresAuthenticationException(String message) {
    super(message);
  }

  public RequiresAuthenticationException(String message, Throwable t) {
    super(message, t);
  }
}
