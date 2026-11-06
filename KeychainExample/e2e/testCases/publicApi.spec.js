import { by, device, element, waitFor } from 'detox';
import {
  enterBiometrics,
  enterPasscode,
  cancelBiometrics,
  waitForAuthValidity,
  dismissAuthenticationIfShown,
} from '../utils/authHelpers';

async function result(action, expected = 'ok', timeout = 15000) {
  await waitFor(element(by.id('status')))
    .toHaveText(`${action}:${expected}`)
    .withTimeout(timeout);
}

async function run(action, timeout) {
  await element(by.id(action)).tap();
  await result(action, 'ok', timeout);
}

async function select(kind, mode = 'biometric') {
  await element(by.id(kind)).tap();
  await element(by.id(mode)).tap();
}

describe('Public API through TurboModules and native storage', () => {
  beforeEach(async () => {
    await device.launchApp({ newInstance: true });
    await run('cleanup');
  });

  afterEach(async () => {
    await dismissAuthenticationIfShown();
    await run('cleanup');
  });

  it('preserves result shapes, identifiers, overwrite and isolation for both credential APIs', async () => {
    // This batch runs both APIs' storage lifecycles sequentially. Hosted iOS
    // simulators can exceed the per-action wait while the batch is still pending.
    await run('contract', device.getPlatform() === 'ios' ? 60000 : 15000);
    if (device.getPlatform() === 'android') {
      for (const kind of ['generic', 'internet']) {
        await select(kind);
        await run('invalid');
      }
    }
  });

  it('reads both credential types after a real process restart', async () => {
    await run('seed');
    await device.launchApp({ newInstance: true });
    await run('persisted');
  });

  for (const kind of ['generic', 'internet']) {
    it(`:android:authenticates ${kind} reads after restart without accessControl`, async () => {
      await select(kind);
      await waitForAuthValidity();
      await element(by.id('save')).tap();
      await enterBiometrics();
      await result('save');
      await device.launchApp({ newInstance: true });
      await select(kind);
      await waitForAuthValidity();
      // These operations must complete without opening authentication UI.
      await run('presence');
      await element(by.id('load')).tap();
      await cancelBiometrics();
      await result('load', 'error:E_AUTH_CANCELED');
      await run('presence');
      await element(by.id('load')).tap();
      await enterBiometrics();
      await result('load');
    });

    it(`:android:restores the ${kind} passcode-only prompt policy after restart`, async () => {
      await select(kind, 'passcode');
      await waitForAuthValidity();
      await element(by.id('save')).tap();
      await enterPasscode();
      await result('save');
      await device.launchApp({ newInstance: true });
      await select(kind);
      await waitForAuthValidity();
      await element(by.id('load')).tap();
      await enterPasscode();
      await result('load');
    });
  }

  it(':android:reads explicitly selected RSA storage without repeating the storage option', async () => {
    await select('generic', 'rsa');
    // RSA encrypts with a public key; only decrypting requires authentication.
    await run('save');
    await waitForAuthValidity();
    await element(by.id('load')).tap();
    await enterBiometrics();
    await result('load');
  });
});
