---
id: faq
title: Frequently Asked Questions
---

**Q: What is the default storage type on Android?**

**A:** When `storage` is omitted, `setGenericPassword` and `setInternetCredentials` select a cipher based on the Android version, requested `accessControl`, and available authentication methods. There is no single fixed `STORAGE_TYPE` default. With neither `storage` nor `accessControl` specified, the library selects storage without user authentication. The `accessible` option is for Apple platforms and does not select an Android cipher.

The `storage` field returned by a successful set or get identifies the cipher used. Set `storage` explicitly if your application requires a specific cipher; see the example below.

---

**Q: Do I need `SECURITY_RULES.AUTOMATIC_UPGRADE` when upgrading to v10?**

**A:** No. Version 10 removed Facebook Conceal (`STORAGE_TYPE.FB`), `SECURITY_RULES`, and the `rules` option. Reads use the cipher recorded with each entry and do not automatically re-encrypt it using the default for new writes.

In v8.2.0, writes without `storage` or biometric access control selected AES-CBC on Android 6.0 (API 23) and later. The AES-CBC reader remains available in v10+, so those entries do not need a Conceal migration simply because `storage` was omitted.

Entries actually stored with Facebook Conceal cannot be read or migrated by v10+. Before upgrading, migrate them on a compatible device using a release that still supports Conceal, such as v9.2.3. In that release, `getGenericPassword` and `getInternetCredentials` with `rules: SECURITY_RULES.AUTOMATIC_UPGRADE` can migrate Conceal entries when a compatible Keystore cipher is available. Verify every affected entry was migrated, and provide a sign-in or recovery path for users who skip the intermediate app release.

The automatic migration from Android SharedPreferences to DataStore moves the stored encrypted entries; it does not convert Conceal ciphertext into a supported cipher.

---

**Q: What happens if the user disables or drops biometric usage?**

**A:** The user will lose the ability to retrieve the secret from storage. If biometric access is re-enabled, access to the secret will be restored.

---

**Q: Is it possible to implement automatic downgrading?**

**A:** From a security perspective, automatic downgrading is considered a "loss of trust." Developers should implement their own logic to allow downgrades and handle the "security loss" accordingly.

> **Recommendation:** Avoid implementing automatic downgrades.

---

**Q: How do I select a specific storage type on Android?**

**A:** Pass a `STORAGE_TYPE` enum value in the third argument. For example, to use AES-GCM without user authentication:

```tsx
import * as Keychain from 'react-native-keychain';

await Keychain.setGenericPassword('session', 'example-token', {
  service: 'session',
  storage: Keychain.STORAGE_TYPE.AES_GCM_NO_AUTH,
});
```

> **Note:** If you attempt to force `RSA` storage when biometrics are not available, the call will be rejected with an error related to the device's biometric configuration.
