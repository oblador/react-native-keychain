import { by, device, element, expect, waitFor } from 'detox';
import { matchLoadInfo } from '../utils/matchLoadInfo';
import {
  waitForAuthValidity,
  enterBiometrics,
  enterPasscode,
} from '../utils/authHelpers';

describe('Access Control', () => {
  beforeEach(async () => {
    await device.launchApp({ newInstance: true });
  });
  ['genericPassword', 'internetCredentials'].forEach((type) => {
    it(
      ':android:should save and retrieve username and password with passcode - ' +
        type,
      async () => {
        await expect(element(by.text('Keychain Example'))).toExist();
        await element(by.id('usernameInput')).typeText('testUsernamePasscode');
        await element(by.id('passwordInput')).typeText('testPasswordPasscode');
        // Hide keyboard
        await element(by.text('Keychain Example')).tap();
        await element(by.text('Passcode')).tap();

        await expect(element(by.text('Save'))).toBeVisible();

        await element(by.text('Save')).tap();
        await enterPasscode();
        // Hide keyboard if open
        await element(by.text('Keychain Example')).tap();
        await waitFor(element(by.text(/^Credentials saved! .*$/)))
          .toExist()
          .withTimeout(5000);

        await waitForAuthValidity();
        await element(by.text('Load')).tap();
        await enterPasscode();
        // Hide keyboard if open
        await element(by.text('Keychain Example')).tap();
        await matchLoadInfo(
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
        await expect(element(by.text('Keychain Example'))).toExist();
        await element(by.id('usernameInput')).typeText(
          'testUsernameBiometrics'
        );
        await element(by.id('passwordInput')).typeText(
          'testPasswordBiometrics'
        );
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
        await enterBiometrics();

        await waitFor(element(by.text(/^Credentials saved! .*$/)))
          .toExist()
          .withTimeout(5000);

        await waitForAuthValidity();
        await element(by.text('Load')).tap();
        await enterBiometrics();

        await matchLoadInfo('testUsernameBiometrics', 'testPasswordBiometrics');
      }
    );

    it(
      'should retrieve username and password after app launch with biometrics - ' +
        type,
      async () => {
        await expect(element(by.text('Keychain Example'))).toExist();
        await expect(
          element(by.text('hasGenericPassword: true'))
        ).toBeVisible();
        await element(by.text('Load')).tap();
        await enterBiometrics();
        await matchLoadInfo('testUsernameBiometrics', 'testPasswordBiometrics');
      }
    );

    it(
      'should save and retrieve username and password without biometrics - ' +
        type,
      async () => {
        await expect(element(by.text('Keychain Example'))).toExist();
        await element(by.id('usernameInput')).typeText('testUsernameAny');
        await element(by.id('passwordInput')).typeText('testPasswordAny');
        // Hide keyboard
        await element(by.text('Keychain Example')).tap();
        await element(by.text('None')).tap();

        if (device.getPlatform() === 'android') {
          await element(by.text('Software')).tap();
        }

        await expect(element(by.text('Save'))).toBeVisible();
        await element(by.text('Save')).tap();
        await waitFor(element(by.text(/^Credentials saved! .*$/)))
          .toExist()
          .withTimeout(5000);
        await element(by.text('Load')).tap();
        await matchLoadInfo('testUsernameAny', 'testPasswordAny');
      }
    );

    it(
      'should retrieve username and password after app launch without biometrics - ' +
        type,
      async () => {
        await expect(element(by.text('Keychain Example'))).toExist();
        await expect(
          element(by.text('hasGenericPassword: true'))
        ).toBeVisible();
        await element(by.text('Load')).tap();
        await matchLoadInfo('testUsernameAny', 'testPasswordAny');
      }
    );
  });

  it('should reset all credentials', async () => {
    await expect(element(by.text('Keychain Example'))).toExist();
    // Hide keyboard

    await element(by.text('Reset')).tap();
    await expect(element(by.text(/^Credentials Reset!$/))).toBeVisible();
  });
});
