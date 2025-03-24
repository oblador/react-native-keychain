import cp from 'child_process';
import { device } from 'detox';

// Wait for 5 seconds to ensure auth validity period has expired
export const waitForAuthValidity = async () => {
  await new Promise((resolve) => setTimeout(resolve, 5500)); // buffer needed for auth validity period
};

export const enterBiometrics = async () => {
  // Biometric prompt is not available in the IOS simulator
  // https://github.com/oblador/react-native-keychain/issues/340
  if (device.getPlatform() === 'android') {
    await new Promise((resolve) => setTimeout(resolve, 1000));
    cp.spawnSync('adb', ['-e', 'emu', 'finger', 'touch', '1']);
  }
};

export const enterPasscode = async () => {
  if (device.getPlatform() === 'android') {
    await new Promise((resolve) => setTimeout(resolve, 1000));
    cp.spawnSync('adb', ['shell', 'input', 'text', '1111']);
    await new Promise((resolve) => setTimeout(resolve, 1000));
    cp.spawnSync('adb', ['shell', 'input', 'keyevent', '66']);
  }
};
