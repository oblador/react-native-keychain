import { by, device, element, expect } from 'detox';
import { selectOption } from '../utils/workbenchHelpers';

describe('Example protection controls', () => {
  it('selects options and keeps the selection when the picker is dismissed', async () => {
    await device.launchApp({ newInstance: true });
    await selectOption('access-Passcode');
    await expect(element(by.id('accessValue'))).toHaveText('Passcode');
    await element(by.id('accessPicker')).tap();
    await element(by.id('accessPickerClose')).tap();
    await expect(element(by.id('accessValue'))).toHaveText('Passcode');
    await selectOption('access-None');
    await expect(element(by.id('accessValue'))).toHaveText('None');

    if (device.getPlatform() === 'ios') {
      await selectOption('sync-Enabled');
      await expect(element(by.id('syncValue'))).toHaveText('Enabled');
      await selectOption('sync-Default');
      await expect(element(by.id('syncValue'))).toHaveText('Default');
    } else {
      await selectOption('security-Software');
      await expect(element(by.id('securityValue'))).toHaveText('Software');
      await selectOption('storage-AES_GCM_NO_AUTH');
      await expect(element(by.id('storageValue'))).toHaveText(
        'AES-GCM · no auth'
      );
      await element(by.id('storagePicker')).tap();
      await device.pressBack();
      await expect(element(by.id('storageValue'))).toHaveText(
        'AES-GCM · no auth'
      );
    }

    await selectOption('type-internetCredentials');
    await expect(element(by.id('credentialScope'))).toHaveText(
      'https://example.com'
    );
    await selectOption('type-genericPassword');
    await expect(element(by.id('credentialScope'))).toHaveText(
      'Default app service'
    );
  });
});
