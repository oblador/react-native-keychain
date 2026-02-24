import { by, element, expect, device } from 'detox';
import { enterBiometrics, waitForAuthValidity } from '../utils/authHelpers';

import {
    expectCredentialsLoadedMessage,
    expectCredentialsSavedMessage,
} from '../utils/statusMessageHelpers';

describe(':android:Knox Storage', () => {
    beforeEach(async () => {
        await device.launchApp({ newInstance: true });
    });

    it(':android:should save with Knox storage', async () => {
        // Check if Knox option is available
        const isKnoxAvailable = await element(by.text('Knox')).atIndex(0).exists();

        if (!isKnoxAvailable) {
            console.warn('Knox option not found, skipping test.');
            return; // Skip the test if Knox is not available
        }

        await expect(element(by.text('Keychain Example'))).toExist();
        await element(by.id('usernameInput')).typeText('testUsernameKnox');
        await element(by.id('passwordInput')).typeText('testPasswordKnox');
        // Hide keyboard
        await element(by.text('Keychain Example')).tap();

        // Select Knox Storage
        await element(by.text('Knox')).tap();

        await expect(element(by.id('saveButton'))).toBeVisible();
        await element(by.id('saveButton')).tap();

        // Knox usually requires auth, so we might need to enter biometrics depending on implementation
        // But based on App.tsx, we are using the default Access Control (Biometry + Passcode)
        // which requires auth.
        await enterBiometrics();

        await expectCredentialsSavedMessage();
        await waitForAuthValidity();

        await element(by.id('loadButton')).tap();
        await enterBiometrics();

        await expectCredentialsLoadedMessage(
            'testUsernameKnox',
            'testPasswordKnox',
            'KnoxAES'
        );
    });
});
