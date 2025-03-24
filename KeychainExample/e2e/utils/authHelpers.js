import cp from 'child_process';
import { by, device, element, expect, waitFor } from 'detox';
const statusTestID = 'statusMessage';

// Wait for 5 seconds to ensure auth validity period has expired
export const waitForAuthValidity = async () => {
  await new Promise((resolve) => setTimeout(resolve, 5500)); // Added 500ms buffer
};

export const enterBiometrics = async () => {
  // Biometric prompt is not available in the IOS simulator
  // https://github.com/oblador/react-native-keychain/issues/340
  if (device.getPlatform() === 'android') {
    await new Promise((resolve) => setTimeout(resolve, 1000));
    cp.spawnSync('adb', ['-e', 'emu', 'finger', 'touch', '1']);
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
};

export const enterPasscode = async () => {
  if (device.getPlatform() === 'android') {
    await new Promise((resolve) => setTimeout(resolve, 1500));
    cp.spawnSync('adb', ['shell', 'input', 'text', '1111']);
    await new Promise((resolve) => setTimeout(resolve, 2000));
    cp.spawnSync('adb', ['shell', 'input', 'keyevent', '66']);
    await new Promise((resolve) => setTimeout(resolve, 1500));
  }
};

export async function expectCredentialsSavedMessage() {
  await waitFor(element(by.id(statusTestID))).toBeVisible();
  const text = await element(by.id(statusTestID));

  expect(text).toHaveText(/^Credentials saved! .*$/);
}
