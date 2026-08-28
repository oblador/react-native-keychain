---
id: faq
title: Frequently Asked Questions
---

**Q: How does the library handle encryption when storing secrets, and can it upgrade the encryption?**

**A:** The library automatically applies the highest possible encryption when storing secrets. However, once a secret is stored, it does not attempt to upgrade the encryption.

---

**Q: What happens if the user disables or drops biometric usage?**

**A:** Access depends on the selected access control and the platform. Removing enrolled biometrics or changing the device passcode can permanently invalidate a key. Re-enabling biometrics does not guarantee recovery. Handle retrieval failures and provide a separate sign-in or recovery flow before storing replacement credentials.

---

**Q: Is it possible to implement automatic downgrading?**

**A:** From a security perspective, automatic downgrading is considered a "loss of trust." Developers should implement their own logic to allow downgrades and handle the "security loss" accordingly.

> **Recommendation:** Avoid implementing automatic downgrades.

---

**Q: How do I force a specific level of encryption when saving a secret?**

**A:** To force a specific encryption level, call:

```tsx
import * as Keychain from 'react-native-keychain';

await Keychain.setGenericPassword('username', 'password', {
  storage: Keychain.STORAGE_TYPE.AES_GCM_NO_AUTH,
});
```

> **Note:** If you attempt to force `RSA` storage when biometrics are not available, the call will be rejected with an error related to the device's biometric configuration.
