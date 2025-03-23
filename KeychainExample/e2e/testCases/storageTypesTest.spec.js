import { by, device, element, expect, waitFor } from 'detox';
import { matchLoadInfo } from '../utils/matchLoadInfo';
import { enterBiometrics, waitForAuthValidity } from '../utils/authHelpers';

describe(':android:Storage Types', () => {
  beforeEach(async () => {
    await device.launchApp({ newInstance: true });
  });
  ['genericPassword', 'internetCredentials'].forEach((type) => {
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
      await waitFor(element(by.text(/^Credentials saved! .*$/)))
        .toExist()
        .withTimeout(3000);
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
      await enterBiometrics();
      await expect(element(by.text(/^Credentials saved! .*$/)))
        .toExist()
        .withTimeout(3000);
      await waitForAuthValidity();
      await element(by.text('Load')).tap();
      await enterBiometrics();
      await matchLoadInfo(
        'testUsernameAESGCM',
        'testPasswordAESGCM',
        'KeystoreAESGCM',
        type === 'internetCredentials' ? 'https://example.com' : undefined
      );
    });

    it(
      ':android:should save with AES_GCM_NO_AUTH storage - ' + type,
      async () => {
        await expect(element(by.text('Keychain Example'))).toExist();
        await element(by.id('usernameInput')).typeText(
          'testUsernameAESGCMNoAuth'
        );
        await element(by.id('passwordInput')).typeText(
          'testPasswordAESGCMNoAuth'
        );
        // Hide keyboard
        await element(by.text('Keychain Example')).tap();

        await element(by.text(type)).tap();
        await element(by.text('None')).tap();
        await element(by.text('AES_GCM_NO_AUTH')).tap();

        await expect(element(by.text('Save'))).toBeVisible();
        await element(by.text('Save')).tap();
        await waitFor(element(by.text(/^Credentials saved! .*$/)))
          .toExist()
          .withTimeout(3000);
        await element(by.text('Load')).tap();
        await matchLoadInfo(
          'testUsernameAESGCMNoAuth',
          'testPasswordAESGCMNoAuth',
          'KeystoreAESGCM_NoAuth',
          type === 'internetCredentials' ? 'https://example.com' : undefined
        );
      }
    );

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
      await expect(element(by.text(/^Credentials saved! .*$/)))
        .toExist()
        .withTimeout(3000);
      await element(by.text('Load')).tap();
      await enterBiometrics();
      await expect(element(by.text(/^Credentials loaded! .*$/)))
        .toExist()
        .withTimeout(3000);
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
    await expect(element(by.text(/^Credentials Reset!$/))).toExist();
  });
});
