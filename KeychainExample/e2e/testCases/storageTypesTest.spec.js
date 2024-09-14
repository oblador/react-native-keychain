import { by, device, element, expect } from 'detox';
import { enrollBiometric } from '../utils/enrollBiometrics';
import { matchLoadInfo } from '../utils/matchLoadInfo';
import cp from 'child_process';

describe(':android:None Access Control', () => {
  beforeAll(async () => {
    await enrollBiometric();
  });

  beforeEach(async () => {
    await device.launchApp({ newInstance: true });
  });

  it(':android:should save with FB storage and migrate it to AES', async () => {
    await expect(element(by.text('Keychain Example'))).toExist();
    await element(by.id('usernameInput')).typeText('testUsernameFB');
    await element(by.id('passwordInput')).typeText('testPasswordFB');
    // Hide keyboard
    await element(by.text('Keychain Example')).tap();
    await element(by.text('None')).tap();
    await element(by.text('No upgrade')).tap();
    await element(by.text('FB')).tap();

    await expect(element(by.text('Save'))).toBeVisible();
    await element(by.text('Save')).tap();
    await expect(element(by.text(/^Credentials saved! .*$/))).toBeVisible();
    await element(by.text('Load')).tap();
    await matchLoadInfo('testUsernameFB', 'testPasswordFB', 'FacebookConceal');
    await element(by.text('Automatic upgrade')).tap();
    await element(by.text('Load')).tap();
    await matchLoadInfo('testUsernameFB', 'testPasswordFB', 'KeystoreAESCBC');
  });

  it(':android:should save with AES storage', async () => {
    await expect(element(by.text('Keychain Example'))).toExist();
    await element(by.id('usernameInput')).typeText('testUsernameAES');
    await element(by.id('passwordInput')).typeText('testPasswordAES');
    // Hide keyboard
    await element(by.text('Keychain Example')).tap();
    await element(by.text('None')).tap();
    await element(by.text('AES')).tap();

    await expect(element(by.text('Save'))).toBeVisible();
    await element(by.text('Save')).tap();
    await expect(element(by.text(/^Credentials saved! .*$/))).toBeVisible();
    await element(by.text('Load')).tap();
    await matchLoadInfo('testUsernameAES', 'testPasswordAES', 'KeystoreAESCBC');
  });

  it(':android:should save with RSA storage', async () => {
    await expect(element(by.text('Keychain Example'))).toExist();
    await element(by.id('usernameInput')).typeText('testUsernameRSA');
    await element(by.id('passwordInput')).typeText('testPasswordRSA');
    // Hide keyboard
    await element(by.text('Keychain Example')).tap();
    await element(by.text('None')).tap();
    await element(by.text('RSA')).tap();

    await expect(element(by.text('Save'))).toBeVisible();
    await element(by.text('Save')).tap();
    await expect(element(by.text(/^Credentials saved! .*$/))).toBeVisible();
    setTimeout(() => {
      cp.spawnSync('adb', ['-e', 'emu', 'finger', 'touch', '1']);
    }, 1000);
    await element(by.text('Load')).tap();
    await expect(element(by.text(/^Credentials loaded! .*$/))).toBeVisible();
    await matchLoadInfo('testUsernameRSA', 'testPasswordRSA', 'KeystoreRSAECB');
  });
});
