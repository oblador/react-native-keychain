import { selectOption, scrollToControl } from '../utils/workbenchHelpers';
import { by, element, expect, device } from 'detox';
import {
  expectCredentialsLoadedMessage,
  expectCredentialsSavedMessage,
  expectCredentialsResetMessage,
} from '../utils/statusMessageHelpers';

describe(':android:Security Level', () => {
  beforeEach(async () => {
    await device.launchApp({ newInstance: true });
  });
  ['genericPassword', 'internetCredentials'].forEach((type) => {
    it(':android:should save with Any security level - ' + type, async () => {
      await expect(element(by.id('appTitle'))).toExist();
      await scrollToControl('usernameInput');
      await element(by.id('usernameInput')).typeText('testUsernameAny');
      await element(by.id('appTitle')).tap();
      await scrollToControl('passwordInput');
      await element(by.id('passwordInput')).typeText('testPasswordAny');
      // Hide keyboard
      await element(by.id('appTitle')).tap();

      await selectOption(`type-${type}`);
      await selectOption('access-None');
      await selectOption('security-Any');

      await expect(element(by.id('saveButton'))).toBeVisible();
      await element(by.id('saveButton')).tap();
      await expectCredentialsSavedMessage();
      await element(by.id('loadButton')).tap();
      await expectCredentialsLoadedMessage(
        'testUsernameAny',
        'testPasswordAny',
        undefined,
        type === 'internetCredentials' ? 'https://example.com' : undefined
      );
    });

    it(
      ':android:should save with Software security level - ' + type,
      async () => {
        await expect(element(by.id('appTitle'))).toExist();
        await scrollToControl('usernameInput');
        await element(by.id('usernameInput')).typeText('testUsernameSoftware');
        await element(by.id('appTitle')).tap();
        await scrollToControl('passwordInput');
        await element(by.id('passwordInput')).typeText('testPasswordSoftware');
        // Hide keyboard
        await element(by.id('appTitle')).tap();

        await selectOption(`type-${type}`);
        await selectOption('access-None');
        await selectOption('security-Software');

        await expect(element(by.id('saveButton'))).toBeVisible();
        await element(by.id('saveButton')).tap();
        await expectCredentialsSavedMessage();
        await element(by.id('loadButton')).tap();
        await expectCredentialsLoadedMessage(
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
        await expect(element(by.id('appTitle'))).toExist();
        await scrollToControl('usernameInput');
        await element(by.id('usernameInput')).typeText('testUsernameHardware');
        await element(by.id('appTitle')).tap();
        await scrollToControl('passwordInput');
        await element(by.id('passwordInput')).typeText('testPasswordHardware');
        // Hide keyboard
        await element(by.id('appTitle')).tap();

        await selectOption(`type-${type}`);
        await selectOption('access-None');
        await selectOption('security-Hardware');

        await expect(element(by.id('saveButton'))).toBeVisible();
        await element(by.id('saveButton')).tap();
        await expectCredentialsSavedMessage();
        await element(by.id('loadButton')).tap();
        await expectCredentialsLoadedMessage(
          'testUsernameHardware',
          'testPasswordHardware',
          undefined,
          type === 'internetCredentials' ? 'https://example.com' : undefined
        );
      }
    );
  });

  it(':android:should reset all credentials', async () => {
    await expect(element(by.id('appTitle'))).toExist();
    // Hide keyboard

    await element(by.id('resetButton')).tap();
    await expectCredentialsResetMessage();
  });
});
