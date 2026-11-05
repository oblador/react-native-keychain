import { selectOption, scrollToControl } from '../utils/workbenchHelpers';
import { by, device, element, expect, waitFor } from 'detox';
import {
  expectCredentialsLoadedMessage,
  expectCredentialsSavedMessage,
  expectCredentialsResetMessage,
} from '../utils/statusMessageHelpers';

// These multi-scope scenarios repeatedly open native picker sheets. On hosted
// simulators they can exceed the default two-minute budget while still progressing.
jest.setTimeout(180000);

async function selectCloudSync(mode) {
  await selectOption(`sync-${mode}`);
}

async function saveCredentials(username) {
  await scrollToControl('usernameInput');
  await element(by.id('usernameInput')).replaceText(username);
  await element(by.id('appTitle')).tap();
  await scrollToControl('passwordInput');
  await element(by.id('passwordInput')).replaceText('test-password');
  await element(by.id('appTitle')).tap();
  await element(by.id('saveButton')).tap();
  await expectCredentialsSavedMessage();
}

async function loadCredentials(username) {
  await element(by.id('loadButton')).tap();
  await expectCredentialsLoadedMessage(username, 'test-password');
}

async function resetCredentials() {
  await element(by.id('resetButton')).tap();
  await expectCredentialsResetMessage();
}

async function expectMissing() {
  await element(by.id('loadButton')).tap();
  await expect(element(by.id('statusMessage'))).toHaveText(
    'No credentials stored'
  );
}

async function clearBothSyncModes() {
  for (const mode of ['Enabled', 'Default']) {
    await selectCloudSync(mode);
    await resetCredentials();
  }
}

// These tests exercise Keychain query matching, without requiring an iCloud
// account or claiming to verify synchronization between physical devices.
describe(':ios:cloudSync', () => {
  beforeEach(async () => {
    await device.launchApp({ newInstance: true });
    await clearBothSyncModes();
  });

  afterEach(clearBothSyncModes);

  for (const type of ['genericPassword', 'internetCredentials']) {
    const presenceID =
      type === 'genericPassword' ? 'genericPresence' : 'internetPresence';

    async function expectExists(value) {
      await waitFor(element(by.id(presenceID)))
        .toHaveText(value ? 'Stored' : 'Empty')
        .withTimeout(10000);
    }

    it(`:ios:false and omitted options access the same ${type}`, async () => {
      await selectOption(`type-${type}`);
      await selectCloudSync('Disabled');
      await saveCredentials('explicit-false');

      await selectCloudSync('Default');
      await expectExists(true);
      await loadCredentials('explicit-false');
      await resetCredentials();

      await selectCloudSync('Disabled');
      await expectExists(false);
      await expectMissing();

      await selectCloudSync('Default');
      await saveCredentials('omitted-option');
      await selectCloudSync('Disabled');
      await expectExists(true);
      await loadCredentials('omitted-option');
      await resetCredentials();

      await selectCloudSync('Default');
      await expectExists(false);
      await expectMissing();
    });

    it(`:ios:true and false keep ${type} separate on read, replace and reset`, async () => {
      await selectOption(`type-${type}`);
      await selectCloudSync('Enabled');
      await saveCredentials('synchronized');

      await selectCloudSync('Disabled');
      await expectExists(false);
      await expectMissing();
      await saveCredentials('local');
      await saveCredentials('local-replacement');

      await selectCloudSync('Enabled');
      await expectExists(true);
      await loadCredentials('synchronized');
      await resetCredentials();
      await expectExists(false);
      await expectMissing();

      await selectCloudSync('Disabled');
      await expectExists(true);
      await loadCredentials('local-replacement');

      await selectCloudSync('Enabled');
      await saveCredentials('synchronized-replacement');
      await selectCloudSync('Disabled');
      await resetCredentials();
      await expectExists(false);
      await expectMissing();

      await selectCloudSync('Enabled');
      await expectExists(true);
      await loadCredentials('synchronized-replacement');
    });
  }
});
