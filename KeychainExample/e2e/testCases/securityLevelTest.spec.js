import { by, device, element, expect, waitFor } from 'detox';
import { matchLoadInfo } from '../utils/matchLoadInfo';

describe(':android:Security Level', () => {
  beforeEach(async () => {
    await device.launchApp({ newInstance: true });
  });
  ['genericPassword', 'internetCredentials'].forEach((type) => {
    it(':android:should save with Any security level - ' + type, async () => {
      await expect(element(by.text('Keychain Example'))).toExist();
      await element(by.id('usernameInput')).typeText('testUsernameAny');
      await element(by.id('passwordInput')).typeText('testPasswordAny');
      // Hide keyboard
      await element(by.text('Keychain Example')).tap();

      await element(by.text(type)).tap();
      await element(by.text('None')).tap();
      await element(by.text('Any')).tap();

      await expect(element(by.text('Save'))).toBeVisible();
      await element(by.text('Save')).tap();
      await waitFor(element(by.text(/^Credentials saved! .*$/)))
        .toExist()
        .withTimeout(3000);
      await element(by.text('Load')).tap();
      await matchLoadInfo(
        'testUsernameAny',
        'testPasswordAny',
        undefined,
        type === 'internetCredentials' ? 'https://example.com' : undefined
      );
    });

    it(
      ':android:should save with Software security level - ' + type,
      async () => {
        await expect(element(by.text('Keychain Example'))).toExist();
        await element(by.id('usernameInput')).typeText('testUsernameSoftware');
        await element(by.id('passwordInput')).typeText('testPasswordSoftware');
        // Hide keyboard
        await element(by.text('Keychain Example')).tap();

        await element(by.text(type)).tap();
        await element(by.text('None')).tap();
        await element(by.text('Software')).tap();

        await expect(element(by.text('Save'))).toBeVisible();
        await element(by.text('Save')).tap();
        await waitFor(element(by.text(/^Credentials saved! .*$/)))
          .toExist()
          .withTimeout(3000);
        await element(by.text('Load')).tap();
        await matchLoadInfo(
          'testUsernameSoftware',
          'testPasswordSoftware',
          undefined,
          type === 'internetCredentials' ? 'https://example.com' : undefined
        );
      }
    );

    it(
      ':android:should save with Hardware security level - ' + type,
      async () => {
        await expect(element(by.text('Keychain Example'))).toExist();
        await element(by.id('usernameInput')).typeText('testUsernameHardware');
        await element(by.id('passwordInput')).typeText('testPasswordHardware');
        // Hide keyboard
        await element(by.text('Keychain Example')).tap();

        await element(by.text(type)).tap();
        await element(by.text('None')).tap();
        await element(by.text('Hardware')).tap();

        await expect(element(by.text('Save'))).toBeVisible();
        await element(by.text('Save')).tap();
        await waitFor(element(by.text(/^Credentials saved! .*$/)))
          .toExist()
          .withTimeout(3000);
        await element(by.text('Load')).tap();
        await matchLoadInfo(
          'testUsernameHardware',
          'testPasswordHardware',
          undefined,
          type === 'internetCredentials' ? 'https://example.com' : undefined
        );
      }
    );
  });

  it(':android:should reset all credentials', async () => {
    await expect(element(by.text('Keychain Example'))).toExist();
    // Hide keyboard

    await element(by.text('Reset')).tap();
    await expect(element(by.text(/^Credentials Reset!$/))).toBeVisible();
  });
});
