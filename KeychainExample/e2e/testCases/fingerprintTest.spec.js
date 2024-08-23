import { by, device, element, expect } from 'detox';
import cp from 'child_process';

describe('FingerprintTest', () => {
  beforeAll(async () => {
    await device.launchApp();
  });

  it('should save and retrieve username and password', async () => {
    await expect(element(by.text('Keychain Example'))).toExist();
    await element(by.id('usernameInput')).typeText('testUsername');
    await element(by.id('passwordInput')).typeText('testPassword');
    // Hide keyboard
    await element(by.text('Keychain Example')).tap();

    await element(by.text('Fingerprint')).tap();
    await element(by.text('Software')).tap();
    await expect(element(by.text('Save'))).toBeVisible();
    await element(by.text('Save')).tap();
    await expect(element(by.text(/^Credentials saved! .*$/))).toBeVisible();
    setTimeout(() => {
      cp.spawnSync('adb', ['-e', 'emu', 'finger', 'touch', '1']);
    }, 1000);
    await element(by.text('Load')).tap();
    await expect(element(by.text('Credentials loaded!'))).toBeVisible();
    await expect(element(by.id('usernameInput'))).toHaveText('testUsername');
    await expect(element(by.id('passwordInput'))).toHaveText('testPassword');
  });

  it('should retrieve username and password after app launch', async () => {
    await device.launchApp({ newInstance: true });
    await expect(element(by.text('Keychain Example'))).toExist();
    setTimeout(() => {
      cp.spawnSync('adb', ['-e', 'emu', 'finger', 'touch', '1']);
    }, 1000);
    await element(by.text('Load')).tap();
    await expect(element(by.text('Credentials loaded!'))).toBeVisible();
    await expect(element(by.id('usernameInput'))).toHaveText('testUsername');
    await expect(element(by.id('passwordInput'))).toHaveText('testPassword');
  });
});
