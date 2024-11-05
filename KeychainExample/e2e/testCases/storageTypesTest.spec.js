import { by, device, element, expect } from 'detox';
import { matchLoadInfo } from '../utils/matchLoadInfo';
import cp from 'child_process';

describe(':android:Storage Types', () => {
  beforeEach(async () => {
    await device.launchApp({ newInstance: true });
  });
  ['genericPassword', 'internetCredentials'].forEach((type) => {
    it(
      ':android:should save with FB storage and migrate it to AES - ' + type,
      async () => {
        await expect(element(by.text('Keychain Example'))).toExist();
        await element(by.id('usernameInput')).typeText('testUsernameFB');
        await element(by.id('passwordInput')).typeText('testPasswordFB');
        // Hide keyboard
        await element(by.text('Keychain Example')).tap();

        await element(by.text(type)).tap();
        await element(by.text('None')).tap();
        await element(by.text('No upgrade')).tap();
        await element(by.text('FB')).tap();

        await expect(element(by.text('Save'))).toBeVisible();
        await element(by.text('Save')).tap();
        await expect(element(by.text(/^Credentials saved! .*$/))).toBeVisible();
        await element(by.text('Load')).tap();
        await matchLoadInfo(
          'testUsernameFB',
          'testPasswordFB',
          'FacebookConceal',
          type === 'internetCredentials' ? 'https://example.com' : undefined
        );
        await element(by.text('Automatic upgrade')).tap();
        await element(by.text('Load')).tap();
        await matchLoadInfo(
          'testUsernameFB',
          'testPasswordFB',
          'KeystoreAESCBC',
          type === 'internetCredentials' ? 'https://example.com' : undefined
        );
      }
    );

    it(':android:should save with AES_CBC storage - ' + type, async () => {
      await expect(element(by.text('Keychain Example'))).toExist();
      await element(by.id('usernameInput')).typeText('testUsernameAESCBC');
      await element(by.id('passwordInput')).typeText('testPasswordAESCBC');
      // Hide keyboard
      await element(by.text('Keychain Example')).tap();

      await element(by.text(type)).tap();
      await element(by.text('None')).tap();
      await element(by.text('AES_CBC')).tap();

      await expect(element(by.text('Save'))).toBeVisible();
      await element(by.text('Save')).tap();
      await expect(element(by.text(/^Credentials saved! .*$/))).toBeVisible();
      await element(by.text('Load')).tap();
      await matchLoadInfo(
        'testUsernameAESCBC',
        'testPasswordAESCBC',
        'KeystoreAESCBC',
        type === 'internetCredentials' ? 'https://example.com' : undefined
      );
    });

    it(':android:should save with AES_GCM storage - ' + type, async () => {
      await expect(element(by.text('Keychain Example'))).toExist();
      await element(by.id('usernameInput')).typeText('testUsernameAESGCM');
      await element(by.id('passwordInput')).typeText('testPasswordAESGCM');
      // Hide keyboard
      await element(by.text('Keychain Example')).tap();

      await element(by.text(type)).tap();
      await element(by.text('None')).tap();
      await element(by.text('AES_GCM')).tap();

      await expect(element(by.text('Save'))).toBeVisible();
      await element(by.text('Save')).tap();
      await expect(element(by.text(/^Credentials saved! .*$/))).toBeVisible();
      await element(by.text('Load')).tap();
      await matchLoadInfo(
        'testUsernameAESGCM',
        'testPasswordAESGCM',
        'KeystoreAESCBC',
        type === 'internetCredentials' ? 'https://example.com' : undefined
      );
    });

    it(':android:should save with RSA storage - ' + type, async () => {
      await expect(element(by.text('Keychain Example'))).toExist();
      await element(by.id('usernameInput')).typeText('testUsernameRSA');
      await element(by.id('passwordInput')).typeText('testPasswordRSA');
      // Hide keyboard
      await element(by.text('Keychain Example')).tap();

      await element(by.text(type)).tap();
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
      await matchLoadInfo(
        'testUsernameRSA',
        'testPasswordRSA',
        'KeystoreRSAECB',
        type === 'internetCredentials' ? 'https://example.com' : undefined
      );
    });
  });

  it(':android:should reset all credentials', async () => {
    await expect(element(by.text('Keychain Example'))).toExist();
    // Hide keyboard

    await element(by.text('Reset')).tap();
    await expect(element(by.text(/^Credentials Reset!$/))).toBeVisible();
  });
});
