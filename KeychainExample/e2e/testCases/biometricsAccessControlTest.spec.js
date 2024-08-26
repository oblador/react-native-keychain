import { by, device, element, expect } from 'detox';
import path from 'path';
import cp from 'child_process';

const enrollBiometric = async () => {
  if (device.getPlatform() === 'android') {
    const script = path.resolve(
      __dirname,
      '../utils/enrollFingerprintAndroid.sh'
    );
    const result = cp.spawnSync('sh', [script], {
      stdio: 'inherit',
    });

    // Check for errors
    if (result.error) {
      console.error('Error executing script:', result.error);
    }
  } else {
    await device.setBiometricEnrollment(true);
  }
};

describe('Biometrics Access Control', () => {
  beforeAll(async () => {
    await enrollBiometric();
  });

  beforeEach(async () => {
    await device.launchApp({ newInstance: true });
  });

  it('should save and retrieve username and password', async () => {
    await expect(element(by.text('Keychain Example'))).toExist();
    await element(by.id('usernameInput')).typeText('testUsername');
    await element(by.id('passwordInput')).typeText('testPassword');
    // Hide keyboard
    await element(by.text('Keychain Example')).tap();

    if (device.getPlatform() === 'android') {
      await element(by.text('Fingerprint')).tap();
      await element(by.text('Software')).tap();
    } else {
      await element(by.text('FaceID')).tap();
    }

    await expect(element(by.text('Save'))).toBeVisible();
    await element(by.text('Save')).tap();
    await expect(element(by.text(/^Credentials saved! .*$/))).toBeVisible();
    // Biometric prompt is not available in the IOS simulator
    // https://github.com/oblador/react-native-keychain/issues/340
    if (device.getPlatform() === 'android') {
      setTimeout(() => {
        cp.spawnSync('adb', ['-e', 'emu', 'finger', 'touch', '1']);
      }, 1000);
    }
    await element(by.text('Load')).tap();
    await expect(element(by.text('Credentials loaded!'))).toBeVisible();
    await expect(element(by.id('usernameInput'))).toHaveText('testUsername');
    await expect(element(by.id('passwordInput'))).toHaveText('testPassword');
  });

  it('should retrieve username and password after app launch', async () => {
    await expect(element(by.text('Keychain Example'))).toExist();
    // Biometric prompt is not available in the IOS simulator
    // https://github.com/oblador/react-native-keychain/issues/340
    if (device.getPlatform() === 'android') {
      setTimeout(() => {
        cp.spawnSync('adb', ['-e', 'emu', 'finger', 'touch', '1']);
      }, 1000);
    }
    await element(by.text('Load')).tap();
    await expect(element(by.text('Credentials loaded!'))).toBeVisible();
    await expect(element(by.id('usernameInput'))).toHaveText('testUsername');
    await expect(element(by.id('passwordInput'))).toHaveText('testPassword');
  });

  it(':android:should save and retrieve username and password for hardware security level', async () => {
    await expect(element(by.text('Keychain Example'))).toExist();
    await element(by.id('usernameInput')).typeText('testUsernameHardware');
    await element(by.id('passwordInput')).typeText('testPasswordHardware');
    // Hide keyboard
    await element(by.text('Keychain Example')).tap();
    await element(by.text('Fingerprint')).tap();
    await element(by.text('Hardware')).tap();

    await expect(element(by.text('Save'))).toBeVisible();
    await element(by.text('Save')).tap();
    await expect(element(by.text(/^Credentials saved! .*$/))).toBeVisible();

    setTimeout(() => {
      cp.spawnSync('adb', ['-e', 'emu', 'finger', 'touch', '1']);
    }, 1000);

    await element(by.text('Load')).tap();
    await expect(element(by.text('Credentials loaded!'))).toBeVisible();
    await expect(element(by.id('usernameInput'))).toHaveText(
      'testUsernameHardware'
    );
    await expect(element(by.id('passwordInput'))).toHaveText(
      'testPasswordHardware'
    );
  });
});
