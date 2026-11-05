import { by, device, element, waitFor } from 'detox';
import { execFileSync } from 'child_process';
import { enterBiometrics, waitForAuthValidity } from '../utils/authHelpers';
import {
  expectCredentialsSavedMessage,
  expectCredentialsResetMessage,
} from '../utils/statusMessageHelpers';
import { scrollToControl, selectOption } from '../utils/workbenchHelpers';

const servers = '["https://example.com"]';

async function list(expected, title = 'Servers listed') {
  await element(by.id('listButton')).tap();
  await waitFor(element(by.id('statusMessage')))
    .toHaveText(title)
    .withTimeout(10000);
  await waitFor(element(by.id('statusDetail')))
    .toHaveText(expected)
    .withTimeout(10000);
}

async function save(username) {
  await scrollToControl('usernameInput');
  await element(by.id('usernameInput')).replaceText(username);
  await element(by.id('appTitle')).tap();
  await scrollToControl('passwordInput');
  await element(by.id('passwordInput')).replaceText('listing-secret');
  await element(by.id('appTitle')).tap();
  await element(by.id('saveButton')).tap();
  await expectCredentialsSavedMessage();
}

async function reset() {
  await element(by.id('resetButton')).tap();
  await expectCredentialsResetMessage();
}

describe('Credential listing', () => {
  beforeEach(async () => {
    await device.launchApp({ newInstance: true });
    await reset();
    if (device.getPlatform() === 'ios') {
      await selectOption('sync-Enabled');
      await reset();
      await selectOption('sync-Default');
    }
  });

  afterEach(async () => {
    await reset();
  });

  it('lists server identifiers, excludes generic entries, and survives restart and overwrite', async () => {
    await save('generic-user');
    if (device.getPlatform() === 'android') {
      await list('[""]', 'Services listed');
    }
    await selectOption('type-internetCredentials');
    await list('[]');
    await save('username-is-not-the-server');
    await list(servers);
    await device.launchApp({ newInstance: true });
    await selectOption('type-internetCredentials');
    await list(servers);
    await save('replacement-user');
    await list(servers);
    await reset();
    await list('[]');
  });

  it(':android:lists protected entries without authentication and recovers after canceled reads', async () => {
    if (device.getPlatform() !== 'android') return;
    await selectOption('type-internetCredentials');
    await scrollToControl('usernameInput');
    await element(by.id('usernameInput')).replaceText('protected-user');
    await element(by.id('appTitle')).tap();
    await scrollToControl('passwordInput');
    await element(by.id('passwordInput')).replaceText('protected-secret');
    await element(by.id('appTitle')).tap();
    await selectOption('access-Fingerprint');
    await element(by.id('saveButton')).tap();
    await enterBiometrics();
    await expectCredentialsSavedMessage();
    await waitForAuthValidity();
    await list(servers);
    await element(by.id('loadButton')).tap();
    execFileSync('adb', ['-s', device.id, 'shell', 'input', 'keyevent', '4']);
    await waitFor(element(by.id('statusMessage')))
      .toHaveText('Could not load credentials')
      .withTimeout(10000);
    await list(servers);
  });

  it(':ios:respects the synchronization scope', async () => {
    if (device.getPlatform() !== 'ios') return;
    await selectOption('type-internetCredentials');
    await selectOption('sync-Enabled');
    await save('cloud-user');
    await list(servers);
    await selectOption('sync-Disabled');
    await list('[]');
    await selectOption('sync-Default');
    await list('[]');
    await selectOption('sync-Enabled');
    await list(servers);
  });
});
