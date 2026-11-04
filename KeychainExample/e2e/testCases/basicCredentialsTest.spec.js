import { by, device, element, expect, waitFor } from 'detox';
import {
  expectCredentialsLoadedMessage,
  expectCredentialsSavedMessage,
  expectCredentialsResetMessage,
} from '../utils/statusMessageHelpers';

const credentials = [
  {
    type: 'genericPassword',
    username: 'generic-user',
    password: 'generic-secret',
  },
  {
    type: 'internetCredentials',
    username: 'internet-user',
    password: 'internet-secret',
  },
];

describe('Basic credential persistence', () => {
  it('stores both credential types independently across a restart and deletes them', async () => {
    await device.launchApp({ newInstance: true });
    await element(by.text('Reset')).tap();
    await expectCredentialsResetMessage();

    for (const { type, username, password } of credentials) {
      await element(by.text(type)).tap();
      await element(by.id('usernameInput')).replaceText(username);
      await element(by.id('passwordInput')).replaceText(password);
      await element(by.text('Keychain Example')).tap();
      await element(by.text('Save')).tap();
      await expectCredentialsSavedMessage();
      await element(by.text('Load')).tap();
      await expectCredentialsLoadedMessage(username, password);
    }

    await device.launchApp({ newInstance: true });
    await waitFor(element(by.text('hasGenericPassword: true')))
      .toBeVisible()
      .withTimeout(10000);
    await waitFor(element(by.text('hasInternetCredentials: true')))
      .toBeVisible()
      .withTimeout(10000);
    for (const { type, username, password } of credentials) {
      await element(by.text(type)).tap();
      await element(by.text('Load')).tap();
      await expectCredentialsLoadedMessage(username, password);
    }

    await element(by.text('Reset')).tap();
    await expectCredentialsResetMessage();
    for (const { type } of credentials) {
      await element(by.text(type)).tap();
      await element(by.text('Load')).tap();
      await expect(element(by.text('No credentials stored.'))).toBeVisible();
    }
    await device.launchApp({ newInstance: true });
    await expect(element(by.text('hasGenericPassword: false'))).toBeVisible();
    await expect(
      element(by.text('hasInternetCredentials: false'))
    ).toBeVisible();
  });
});
