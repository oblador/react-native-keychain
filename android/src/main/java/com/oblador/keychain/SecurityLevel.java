package com.oblador.keychain;

import androidx.annotation.NonNull;

/** */
public enum SecurityLevel {
  /** */
  ANY,
  /** */
  SECURE_SOFTWARE,
  /** Trusted Execution Environment or Secure Environment guarantees */
  SECURE_HARDWARE;

  /** Get JavaScript friendly name. */
  @NonNull
  public String jsName() {
    return String.format("SECURITY_LEVEL_%s", this.name());
  }

  public boolean satisfiesSafetyThreshold(@NonNull final SecurityLevel threshold) {
    return this.compareTo(threshold) >= 0;
  }
}

