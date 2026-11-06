import { selectOption, scrollToControl } from '../utils/workbenchHelpers';
import { by, element, expect, device } from 'detox';
import { enterBiometrics, waitForAuthValidity } from '../utils/authHelpers';

import {
  expectCredentialsLoadedMessage,
  expectCredentialsSavedMessage,
  expectCredentialsResetMessage,
} from '../utils/statusMessageHelpers';

describe(':android:Storage Types', () => {
  beforeEach(async () => {
    await device.launchApp({ newInstance: true });
  });
  ['genericPassword', 'internetCredentials'].forEach((type) => {
    it(':android:should save with AES_CBC storage - ' + type, async () => {
      await expect(element(by.id('appTitle'))).toExist();
      await scrollToControl('usernameInput');
      await element(by.id('usernameInput')).typeText('testUsernameAESCBC');
      await element(by.id('appTitle')).tap();
      await scrollToControl('passwordInput');
      await element(by.id('passwordInput')).typeText('testPasswordAESCBC');
      // Hide keyboard
      await element(by.id('appTitle')).tap();

      await selectOption(`type-${type}`);
      await selectOption('access-None');
      await selectOption('storage-AES_CBC');

      await expect(element(by.id('saveButton'))).toBeVisible();
      await element(by.id('saveButton')).tap();
      await expectCredentialsSavedMessage();
      await element(by.id('loadButton')).tap();
      await expectCredentialsLoadedMessage(
        'testUsernameAESCBC',
        'testPasswordAESCBC',
        'KeystoreAESCBC',
        type === 'internetCredentials' ? 'https://example.com' : undefined
      );
    });

    it(':android:should save with AES_GCM storage - ' + type, async () => {
      await expect(element(by.id('appTitle'))).toExist();
      await scrollToControl('usernameInput');
      await element(by.id('usernameInput')).typeText('testUsernameAESGCM');
      await element(by.id('appTitle')).tap();
      await scrollToControl('passwordInput');
      await element(by.id('passwordInput')).typeText('testPasswordAESGCM');
      // Hide keyboard
      await element(by.id('appTitle')).tap();

      await selectOption(`type-${type}`);
      await selectOption('access-None');
      await selectOption('storage-AES_GCM');

      await expect(element(by.id('saveButton'))).toBeVisible();
      await element(by.id('saveButton')).tap();
      await enterBiometrics();
      await expectCredentialsSavedMessage();
      await waitForAuthValidity();
      await element(by.id('loadButton')).tap();
      await enterBiometrics();
      await expectCredentialsLoadedMessage(
        'testUsernameAESGCM',
        'testPasswordAESGCM',
        'KeystoreAESGCM',
        type === 'internetCredentials' ? 'https://example.com' : undefined
      );
    });

    it(
      ':android:should save with AES_GCM_NO_AUTH storage - ' + type,
      async () => {
        await expect(element(by.id('appTitle'))).toExist();
        await scrollToControl('usernameInput');
        await element(by.id('usernameInput')).typeText(
          'testUsernameAESGCMNoAuth'
        );
        await element(by.id('appTitle')).tap();
        await scrollToControl('passwordInput');
        await element(by.id('passwordInput')).typeText(
          'testPasswordAESGCMNoAuth'
        );
        // Hide keyboard
        await element(by.id('appTitle')).tap();

        await selectOption(`type-${type}`);
        await selectOption('access-None');
        await selectOption('storage-AES_GCM_NO_AUTH');

        await expect(element(by.id('saveButton'))).toBeVisible();
        await element(by.id('saveButton')).tap();
        await expectCredentialsSavedMessage();
        await element(by.id('loadButton')).tap();
        await expectCredentialsLoadedMessage(
          'testUsernameAESGCMNoAuth',
          'testPasswordAESGCMNoAuth',
          'KeystoreAESGCM_NoAuth',
          type === 'internetCredentials' ? 'https://example.com' : undefined
        );
      }
    );

    it(':android:should save with RSA storage - ' + type, async () => {
      await expect(element(by.id('appTitle'))).toExist();
      await scrollToControl('usernameInput');
      await element(by.id('usernameInput')).typeText('testUsernameRSA');
      await element(by.id('appTitle')).tap();
      await scrollToControl('passwordInput');
      await element(by.id('passwordInput')).typeText('testPasswordRSA');
      // Hide keyboard
      await element(by.id('appTitle')).tap();

      await selectOption(`type-${type}`);
      await selectOption('access-None');
      await selectOption('storage-RSA');

      await expect(element(by.id('saveButton'))).toBeVisible();
      await element(by.id('saveButton')).tap();
      await expectCredentialsSavedMessage();
      await element(by.id('loadButton')).tap();
      await enterBiometrics();
      await expectCredentialsLoadedMessage(
        'testUsernameRSA',
        'testPasswordRSA',
        'KeystoreRSAECB',
        type === 'internetCredentials' ? 'https://example.com' : undefined
      );
    });
  });

  it(':android:should reset all credentials', async () => {
    await expect(element(by.id('appTitle'))).toExist();
    // Hide keyboard

    await element(by.id('resetButton')).tap();
    await expectCredentialsResetMessage();
  });
});
