import { by, device, element, expect } from 'detox';
import { matchLoadInfo } from '../utils/matchLoadInfo';

describe('None Access Control', () => {
  beforeEach(async () => {
    await device.launchApp({ newInstance: true });
  });

  it('should save and retrieve username and password', async () => {
    await expect(element(by.text('Keychain Example'))).toExist();
    await element(by.id('usernameInput')).typeText('testUsername');
    await element(by.id('passwordInput')).typeText('testPassword');
    // Hide keyboard
    await element(by.text('Keychain Example')).tap();
    await element(by.text('None')).tap();

    if (device.getPlatform() === 'android') {
      await element(by.text('Software')).tap();
    }

    await expect(element(by.text('Save'))).toBeVisible();
    await element(by.text('Save')).tap();
    await expect(element(by.text(/^Credentials saved! .*$/))).toBeVisible();
    await element(by.text('Load')).tap();
    await matchLoadInfo('testUsername', 'testPassword');
  });

  it('should retrieve username and password after app launch', async () => {
    await expect(element(by.text('Keychain Example'))).toExist();
    await expect(element(by.text('hasGenericPassword: true'))).toBeVisible();
    await element(by.text('Load')).tap();
    await matchLoadInfo('testUsername', 'testPassword');
  });
});
