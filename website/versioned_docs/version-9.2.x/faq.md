---
id: faq
title: Frequently Asked Questions
---

**Q: How does the library handle encryption when storing secrets, and can it upgrade the encryption?**

**A:** The library automatically applies the highest possible encryption when storing secrets. However, once a secret is stored, it does not attempt to upgrade the encryption unless **Facebook Conceal** was used and the `SECURITY_RULES` option is set to `AUTOMATIC_UPGRADE`.

---

**Q: What happens if the user disables or drops biometric usage?**

**A:** The user will lose the ability to retrieve the secret from storage. If biometric access is re-enabled, access to the secret will be restored.

---

**Q: Is it possible to implement automatic downgrading?**

**A:** From a security perspective, automatic downgrading is considered a "loss of trust." Developers should implement their own logic to allow downgrades and handle the "security loss" accordingly. 

> **Recommendation:** Avoid implementing automatic downgrades.

---

**Q: How do I enable automatic upgrades for Facebook Conceal?**

**A:** Use the following call:

```tsx
getGenericPassword({ ...otherProps, rules: "AUTOMATIC_UPGRADE" });
```

Ensure the `rules` property is set to the string value `AUTOMATIC_UPGRADE`.

---

**Q: How do I force a specific level of encryption when saving a secret?**

**A:** To force a specific encryption level, call:

```tsx
setGenericPassword({ ...otherProps, storage: "AES_GCM_NO_AUTH" });
```

> **Note:** If you attempt to force `RSA` storage when biometrics are not available, the call will be rejected with an error related to the device's biometric configuration.