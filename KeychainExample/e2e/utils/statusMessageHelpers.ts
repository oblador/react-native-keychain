import { by, element, expect, waitFor } from 'detox';
import { scrollToControl } from './workbenchHelpers';

const TIMEOUT = 10000;

export async function expectCredentialsSavedMessage() {
  await waitFor(element(by.id('statusMessage')))
    .toHaveText('Credentials saved')
    .withTimeout(TIMEOUT);
}

export async function expectCredentialsResetMessage() {
  await waitFor(element(by.id('statusMessage')))
    .toHaveText('Credentials reset')
    .withTimeout(TIMEOUT);
}

export async function expectCredentialsLoadedMessage(
  username: string,
  password: string,
  storage?: string,
  service?: string
) {
  await waitFor(element(by.id('statusMessage')))
    .toHaveText('Credentials loaded')
    .withTimeout(TIMEOUT);
  await expect(element(by.id('loadedUsername'))).toHaveText(username);
  await expect(element(by.id('loadedPassword'))).toHaveText('••••••••');
  await scrollToControl('revealPassword');
  await element(by.id('revealPassword')).tap();
  await expect(element(by.id('loadedPassword'))).toHaveText(password);
  await element(by.id('revealPassword')).tap();
  await expect(element(by.id('loadedPassword'))).toHaveText('••••••••');
  if (storage) {
    await expect(element(by.id('loadedStorage'))).toHaveText(storage);
  }
  if (service) {
    await expect(element(by.id('loadedService'))).toHaveText(service);
  }
}
