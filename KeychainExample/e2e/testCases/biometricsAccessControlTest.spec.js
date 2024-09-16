import { by, device, element, expect } from 'detox';
import { enrollBiometric } from '../utils/enrollBiometrics';
import { matchLoadInfo } from '../utils/matchLoadInfo';
import cp from 'child_process';

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
    await matchLoadInfo('testUsername', 'testPassword');
  });

  it('should retrieve username and password after app launch', async () => {
    await expect(element(by.text('Keychain Example'))).toExist();
    await expect(element(by.text('hasGenericPassword: true'))).toBeVisible();
    // Biometric prompt is not available in the IOS simulator
    // https://github.com/oblador/react-native-keychain/issues/340
    if (device.getPlatform() === 'android') {
      setTimeout(() => {
        cp.spawnSync('adb', ['-e', 'emu', 'finger', 'touch', '1']);
      }, 1000);
    }
    await element(by.text('Load')).tap();
    await matchLoadInfo('testUsername', 'testPassword');
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
    await matchLoadInfo('testUsernameHardware', 'testPasswordHardware');
  });
});
