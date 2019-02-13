package com.oblador.keychain;

public enum SecurityLevel {
    ANY,
    SECURE_SOFTWARE,
    SECURE_HARDWARE; // Trusted Execution Environment or Secure Environment guarantees

    public String jsName() {
        return String.format("SECURITY_LEVEL_%s", this.name());
    }

    public boolean satisfiesSafetyThreshold(SecurityLevel threshold) {
        return this.compareTo(threshold) >= 0;
    }
}

