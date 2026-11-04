import { selectOption, scrollToControl } from '../utils/workbenchHelpers';
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
    await element(by.id('resetButton')).tap();
    await expectCredentialsResetMessage();

    for (const { type, username, password } of credentials) {
      await selectOption(`type-${type}`);
      await scrollToControl('usernameInput');
      await element(by.id('usernameInput')).replaceText(username);
      await element(by.id('appTitle')).tap();
      await scrollToControl('passwordInput');
      await element(by.id('passwordInput')).replaceText(password);
      await element(by.id('appTitle')).tap();
      await element(by.id('saveButton')).tap();
      await expectCredentialsSavedMessage();
      await element(by.id('loadButton')).tap();
      await expectCredentialsLoadedMessage(
        username,
        password,
        undefined,
        type === 'internetCredentials' ? 'https://example.com' : undefined
      );
    }

    await device.launchApp({ newInstance: true });
    await waitFor(element(by.id('genericPresence')))
      .toHaveText('Stored')
      .withTimeout(10000);
    await waitFor(element(by.id('internetPresence')))
      .toHaveText('Stored')
      .withTimeout(10000);
    for (const { type, username, password } of credentials) {
      await selectOption(`type-${type}`);
      await element(by.id('loadButton')).tap();
      await expectCredentialsLoadedMessage(
        username,
        password,
        undefined,
        type === 'internetCredentials' ? 'https://example.com' : undefined
      );
    }

    await element(by.id('resetButton')).tap();
    await expectCredentialsResetMessage();
    for (const { type } of credentials) {
      await selectOption(`type-${type}`);
      await element(by.id('loadButton')).tap();
      await expect(element(by.id('statusMessage'))).toHaveText(
        'No credentials stored'
      );
    }
    await device.launchApp({ newInstance: true });
    await expect(element(by.id('genericPresence'))).toHaveText('Empty');
    await expect(element(by.id('internetPresence'))).toHaveText('Empty');
  });
});
