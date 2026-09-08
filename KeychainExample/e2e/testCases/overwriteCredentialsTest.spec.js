import { by, device, element, expect } from 'detox';
import {
  expectCredentialsLoadedMessage,
  expectCredentialsResetMessage,
  expectCredentialsSavedMessage,
} from '../utils/statusMessageHelpers';

describe('Overwrite Credentials', () => {
  beforeEach(async () => {
    await device.launchApp({ newInstance: true });
  });

  ['genericPassword', 'internetCredentials'].forEach((type) => {
    it(
      'should overwrite the password when saving twice with the same username - ' +
        type,
      async () => {
        await expect(element(by.text('Keychain Example'))).toExist();
        await element(by.text(type)).tap();
        await element(by.text('None')).tap();

        if (device.getPlatform() === 'android') {
          await element(by.text('Software')).tap();
        }

        await element(by.id('usernameInput')).typeText('testUsernameOverwrite');
        await element(by.id('passwordInput')).typeText('testPasswordFirst');
        // Hide keyboard
        await element(by.id('passwordInput')).tapReturnKey();
        await element(by.text('Save')).tap();
        await expectCredentialsSavedMessage();

        await element(by.id('usernameInput')).typeText('testUsernameOverwrite');
        await element(by.id('passwordInput')).typeText('testPasswordSecond');
        // Hide keyboard
        await element(by.id('passwordInput')).tapReturnKey();
        await element(by.text('Save')).tap();
        await expectCredentialsSavedMessage();

        await element(by.text('Load')).tap();
        await expectCredentialsLoadedMessage(
          'testUsernameOverwrite',
          'testPasswordSecond'
        );

        await element(by.text('Reset')).tap();
        await expectCredentialsResetMessage();
      }
    );
  });
});
