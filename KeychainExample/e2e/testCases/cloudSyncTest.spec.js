import { by, device, element, expect, waitFor } from 'detox';
import {
  expectCredentialsLoadedMessage,
  expectCredentialsSavedMessage,
  expectCredentialsResetMessage,
} from '../utils/statusMessageHelpers';

async function selectCloudSync(mode) {
  await element(by.text(mode)).tap();
}

async function saveCredentials(username) {
  await element(by.id('usernameInput')).replaceText(username);
  await element(by.id('passwordInput')).replaceText('test-password');
  await element(by.text('Keychain Example')).tap();
  await element(by.text('Save')).tap();
  await expectCredentialsSavedMessage();
}

async function loadCredentials(username) {
  await element(by.text('Load')).tap();
  await expectCredentialsLoadedMessage(username, 'test-password');
}

async function resetCredentials() {
  await element(by.text('Reset')).tap();
  await expectCredentialsResetMessage();
}

async function expectMissing() {
  await element(by.text('Load')).tap();
  await expect(element(by.text('No credentials stored.'))).toBeVisible();
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
    const hasLabel =
      type === 'genericPassword'
        ? 'hasGenericPassword'
        : 'hasInternetCredentials';

    async function expectExists(value) {
      await waitFor(element(by.text(`${hasLabel}: ${value}`)))
        .toBeVisible()
        .withTimeout(10000);
    }

    it(`:ios:false and omitted options access the same ${type}`, async () => {
      await element(by.text(type)).tap();
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
      await element(by.text(type)).tap();
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
