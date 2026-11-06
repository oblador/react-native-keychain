import { selectOption, scrollToControl } from '../utils/workbenchHelpers';
import { by, device, element, expect } from 'detox';
import {
  waitForAuthValidity,
  enterBiometrics,
  enterPasscode,
} from '../utils/authHelpers';
import {
  expectCredentialsLoadedMessage,
  expectCredentialsSavedMessage,
  expectCredentialsResetMessage,
} from '../utils/statusMessageHelpers';

describe('Access Control', () => {
  beforeEach(async () => {
    await device.launchApp({ newInstance: true });
  });
  ['genericPassword', 'internetCredentials'].forEach((type) => {
    it(
      ':android:should save and retrieve username and password with passcode - ' +
        type,
      async () => {
        await expect(element(by.id('appTitle'))).toExist();
        await selectOption(`type-${type}`);
        await scrollToControl('usernameInput');
        await element(by.id('usernameInput')).replaceText(
          'testUsernamePasscode'
        );
        await element(by.id('appTitle')).tap();
        await scrollToControl('passwordInput');
        await element(by.id('passwordInput')).replaceText(
          'testPasswordPasscode'
        );
        // Hide keyboard
        await element(by.id('appTitle')).tap();
        await selectOption('access-Passcode');

        await expect(element(by.id('saveButton'))).toBeVisible();

        await element(by.id('saveButton')).tap();
        await enterPasscode();
        // Hide keyboard if open
        await element(by.id('appTitle')).tap();
        await expectCredentialsSavedMessage();

        await waitForAuthValidity();
        await element(by.id('loadButton')).tap();
        await enterPasscode();
        // Hide keyboard if open
        await element(by.id('appTitle')).tap();
        await expectCredentialsLoadedMessage(
          'testUsernamePasscode',
          'testPasswordPasscode',
          'KeystoreAESGCM'
        );
      }
    );
    it(
      ':android:should retrieve passcode credentials after restart without accessControl - ' +
        type,
      async () => {
        await expect(element(by.id('appTitle'))).toExist();
        await selectOption(`type-${type}`);
        await waitForAuthValidity();
        await element(by.id('loadButton')).tap();
        await enterPasscode();
        await expectCredentialsLoadedMessage(
          'testUsernamePasscode',
          'testPasswordPasscode',
          'KeystoreAESGCM'
        );
      }
    );

    it(
      ' should save and retrieve username and password with biometrics - ' +
        type,
      async () => {
        await expect(element(by.id('appTitle'))).toExist();
        await selectOption(`type-${type}`);
        await scrollToControl('usernameInput');
        await element(by.id('usernameInput')).replaceText(
          'testUsernameBiometrics'
        );
        await element(by.id('appTitle')).tap();
        await scrollToControl('passwordInput');
        await element(by.id('passwordInput')).replaceText(
          'testPasswordBiometrics'
        );
        // Hide keyboard
        await element(by.id('appTitle')).tap();

        if (device.getPlatform() === 'android') {
          await selectOption('access-Fingerprint');
          await selectOption('security-Software');
        } else {
          await selectOption('access-FaceID');
        }

        await expect(element(by.id('saveButton'))).toBeVisible();
        await element(by.id('saveButton')).tap();
        await enterBiometrics();

        await expectCredentialsSavedMessage();

        await waitForAuthValidity();
        await element(by.id('loadButton')).tap();
        await enterBiometrics();

        await expectCredentialsLoadedMessage(
          'testUsernameBiometrics',
          'testPasswordBiometrics'
        );
      }
    );

    it(
      'should retrieve username and password after app launch with biometrics - ' +
        type,
      async () => {
        await expect(element(by.id('appTitle'))).toExist();
        await selectOption(`type-${type}`);
        await expect(
          element(
            by.id(
              type === 'genericPassword'
                ? 'genericPresence'
                : 'internetPresence'
            )
          )
        ).toHaveText('Stored');
        await element(by.id('loadButton')).tap();
        await enterBiometrics();
        await expectCredentialsLoadedMessage(
          'testUsernameBiometrics',
          'testPasswordBiometrics'
        );
      }
    );

    it(
      'should save and retrieve username and password without biometrics - ' +
        type,
      async () => {
        await expect(element(by.id('appTitle'))).toExist();
        await selectOption(`type-${type}`);
        await scrollToControl('usernameInput');
        await element(by.id('usernameInput')).replaceText('testUsernameAny');
        await element(by.id('appTitle')).tap();
        await scrollToControl('passwordInput');
        await element(by.id('passwordInput')).replaceText('testPasswordAny');
        // Hide keyboard
        await element(by.id('appTitle')).tap();
        await selectOption('access-None');

        if (device.getPlatform() === 'android') {
          await selectOption('security-Software');
        }

        await expect(element(by.id('saveButton'))).toBeVisible();
        await element(by.id('saveButton')).tap();
        await expectCredentialsSavedMessage();
        await element(by.id('loadButton')).tap();
        await expectCredentialsLoadedMessage(
          'testUsernameAny',
          'testPasswordAny'
        );
      }
    );

    it(
      'should retrieve username and password after app launch without biometrics - ' +
        type,
      async () => {
        await expect(element(by.id('appTitle'))).toExist();
        await selectOption(`type-${type}`);
        await expect(
          element(
            by.id(
              type === 'genericPassword'
                ? 'genericPresence'
                : 'internetPresence'
            )
          )
        ).toHaveText('Stored');
        await element(by.id('loadButton')).tap();
        await expectCredentialsLoadedMessage(
          'testUsernameAny',
          'testPasswordAny'
        );
      }
    );
  });

  it('should reset all credentials', async () => {
    await expect(element(by.id('appTitle'))).toExist();
    // Hide keyboard

    await element(by.id('resetButton')).tap();
    await expectCredentialsResetMessage();
  });
});
