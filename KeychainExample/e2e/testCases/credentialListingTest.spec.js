import { by, device, element, expect, waitFor } from 'detox';
import { execFileSync } from 'child_process';
import { enterBiometrics, waitForAuthValidity } from '../utils/authHelpers';
import {
  expectCredentialsSavedMessage,
  expectCredentialsResetMessage,
} from '../utils/statusMessageHelpers';

const servers = 'Servers: ["https://example.com"]';

async function list(expected) {
  await element(by.text('List')).tap();
  await expect(element(by.id('statusMessage'))).toHaveText(expected);
}

async function save(username) {
  await element(by.id('usernameInput')).replaceText(username);
  await element(by.id('passwordInput')).replaceText('listing-secret');
  await element(by.text('Keychain Example')).tap();
  await element(by.text('Save')).tap();
  await expectCredentialsSavedMessage();
}

async function reset() {
  await element(by.text('Reset')).tap();
  await expectCredentialsResetMessage();
}

describe('Credential listing', () => {
  beforeEach(async () => {
    await device.launchApp({ newInstance: true });
    await reset();
    if (device.getPlatform() === 'ios') {
      await element(by.text('Enabled')).tap();
      await reset();
      await element(by.text('Default')).tap();
    }
  });

  afterEach(async () => {
    await reset();
  });

  it('lists server identifiers, excludes generic entries, and survives restart and overwrite', async () => {
    await save('generic-user');
    if (device.getPlatform() === 'android') {
      await list('Services: [""]');
    }
    await element(by.text('internetCredentials')).tap();
    await list('Servers: []');
    await save('username-is-not-the-server');
    await list(servers);
    await device.launchApp({ newInstance: true });
    await element(by.text('internetCredentials')).tap();
    await list(servers);
    await save('replacement-user');
    await list(servers);
    await reset();
    await list('Servers: []');
  });

  it(':android:lists protected entries without authentication and recovers after canceled reads', async () => {
    if (device.getPlatform() !== 'android') return;
    await element(by.text('internetCredentials')).tap();
    await element(by.id('usernameInput')).replaceText('protected-user');
    await element(by.id('passwordInput')).replaceText('protected-secret');
    await element(by.text('Keychain Example')).tap();
    await element(by.text('Fingerprint')).tap();
    await element(by.text('Save')).tap();
    await enterBiometrics();
    await expectCredentialsSavedMessage();
    await waitForAuthValidity();
    await list(servers);
    await element(by.text('Load')).tap();
    execFileSync('adb', ['-s', device.id, 'shell', 'input', 'keyevent', '4']);
    await waitFor(element(by.text(/^Could not load credentials\.[\s\S]*$/)))
      .toBeVisible()
      .withTimeout(10000);
    await list(servers);
  });

  it(':ios:respects the synchronization scope', async () => {
    if (device.getPlatform() !== 'ios') return;
    await element(by.text('internetCredentials')).tap();
    await element(by.text('Enabled')).tap();
    await save('cloud-user');
    await list(servers);
    await element(by.text('Disabled')).tap();
    await list('Servers: []');
    await element(by.text('Default')).tap();
    await list('Servers: []');
    await element(by.text('Enabled')).tap();
    await list(servers);
  });
});
