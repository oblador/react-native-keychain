# Authentication

Require the user to authenticate before accessing a saved secret.

Set the protection policy when saving the credential. A custom prompt explains why the app is asking for authentication; it does not add protection by itself.

## Check available authentication

```ts
import * as Keychain from 'react-native-keychain';

const biometry = await Keychain.getSupportedBiometryType();
const hasPasscode = await Keychain.isPasscodeAuthAvailable();
```

`getSupportedBiometryType()` returns a biometry type or `null`. Capability checks help you present choices; they do not guarantee that a later credential operation will succeed. The device's enrollment, passcode, and lockout state can change.

## Protect a credential

This example ties access to the current biometric enrollment:

```ts
import * as Keychain from 'react-native-keychain';

const service = 'com.example.protected-session';

await Keychain.setGenericPassword('session', 'example-token', {
  service,
  accessControl: Keychain.ACCESS_CONTROL.BIOMETRY_CURRENT_SET,
  authenticationPrompt: {
    title: 'Save your sign-in',
    cancel: 'Cancel',
  },
});

try {
  const credentials = await Keychain.getGenericPassword({
    service,
    authenticationPrompt: {
      title: 'Unlock your sign-in',
      cancel: 'Cancel',
    },
  });

  if (credentials) {
    // Use credentials.password without logging or displaying it.
  } else {
    // No matching credential exists. Offer sign-in.
  }
} catch {
  // Authentication or storage failed. Let the user retry or sign in again.
}
```

Saving may also require authentication on Android. Treat both save and read as operations that can fail or be cancelled.

## Choose a policy

| Policy                                    | Intent                                                       |
| ----------------------------------------- | ------------------------------------------------------------ |
| `BIOMETRY_ANY`                            | Allow biometric authentication.                              |
| `BIOMETRY_CURRENT_SET`                    | Bind access to the current biometric enrollment.             |
| `DEVICE_PASSCODE`                         | Require the device passcode.                                 |
| `BIOMETRY_ANY_OR_DEVICE_PASSCODE`         | Allow biometrics or a device passcode.                       |
| `BIOMETRY_CURRENT_SET_OR_DEVICE_PASSCODE` | Allow the current biometric enrollment or a device passcode. |

Support and behavior depend on the platform, OS version, and device. See [`ACCESS_CONTROL`](./api/enumerations/ACCESS_CONTROL.md) for the complete set of policies. Do not silently retry with weaker protection when a requested policy is unavailable.

On Android, reads without an explicit `accessControl` reuse the policy saved with the entry. A read can narrow that policy, but cannot broaden it. Older entries without a recorded policy have platform-specific defaults; see [`GetOptions`](./api/type-aliases/GetOptions.md).

## Handle changes and failures

A cancelled prompt, failed authentication, lockout, or invalidated key is different from a missing item. Use the exported [`ERROR_CODE`](./api/enumerations/ERROR_CODE.md) values when your app needs to distinguish supported errors; native mappings differ by platform.

Changing biometric enrollment or removing a passcode can make protected credentials inaccessible, depending on the policy. Provide a sign-in or recovery path. Re-enrolling biometrics does not guarantee that an invalidated key becomes usable again.

For Face ID, remember the [iOS purpose string](./installation.md#ios-setup). Use a device to verify the authentication flows your app relies on.
