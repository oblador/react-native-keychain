import { execFile } from 'child_process';
import { promisify } from 'util';
import { device } from 'detox';

const execute = promisify(execFile);
const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));
const adb = (...args: string[]) =>
  execute('adb', ['-s', device.id, ...args], { timeout: 5000 });

// The key's actual validity window is five seconds. This is not a UI readiness delay.
export const waitForAuthValidity = () => delay(5500);

async function androidPrompt(passcode = false) {
  const deadline = Date.now() + 15000;
  while (Date.now() < deadline) {
    const { stdout } = await adb('shell', 'dumpsys', 'window');
    const focus = stdout
      .split('\n')
      .filter((line) => /mCurrentFocus|mFocusedApp/.test(line))
      .join('\n');
    if (!passcode && /BiometricPrompt/.test(focus)) {
      // The window can take focus before Android starts the fingerprint sensor.
      // AOSP AuthSession starts it after onDialogAnimatedIn(): session state 3
      // (AUTH_STARTED_UI_SHOWING), fingerprint modality 2, sensor state 3
      // (AUTHENTICATING). Sending a fingerprint earlier can silently lose it.
      const biometrics = await adb('shell', 'dumpsys', 'biometric');
      if (
        /CurrentSession:\s*State:\s*3\b/.test(biometrics.stdout) &&
        /modality 2, state: 3\b/.test(biometrics.stdout)
      )
        return;
    }
    if (
      passcode &&
      /BiometricPrompt|ConfirmDeviceCredential|ConfirmLockPassword/.test(focus)
    ) {
      // Android 35+ embeds PIN entry in SystemUI's BiometricPrompt. Wait for the
      // focused editor too, not just the activity starting its entrance animation.
      const input = await adb('shell', 'dumpsys', 'input_method');
      if (
        /mServedView=.*(?:lockPassword|password_entry|pinEntry)/.test(
          input.stdout
        ) &&
        /mServedConnecting=false/.test(input.stdout)
      )
        return;
    }
    await delay(100);
  }
  throw new Error(
    `Timed out waiting for the Android ${passcode ? 'PIN' : 'biometric'} prompt`
  );
}

export async function enterBiometrics() {
  if (device.getPlatform() === 'android') {
    await androidPrompt();
    await adb('emu', 'finger', 'touch', '1');
  } else
    throw new Error(
      'Keychain authentication requires a physical iPhone; use the XCTest UI suite.'
    );
}

export async function cancelBiometrics() {
  await androidPrompt();
  await adb('shell', 'input', 'keyevent', '4');
}

export async function dismissAuthenticationIfShown() {
  if (device.getPlatform() !== 'android') return;
  const { stdout } = await adb('shell', 'dumpsys', 'window');
  if (
    /mCurrentFocus=.*(?:BiometricPrompt|ConfirmDeviceCredential|ConfirmLockPassword)/.test(
      stdout
    )
  ) {
    await adb('shell', 'input', 'keyevent', '4');
  }
}

export async function enterPasscode() {
  await androidPrompt(true);
  await adb('shell', 'input', 'text', '1111');
  await adb('shell', 'input', 'keyevent', '66');
}
