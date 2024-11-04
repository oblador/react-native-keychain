import { device } from 'detox';

beforeAll(async () => {
  if (device.getPlatform() === 'ios') {
    await device.setBiometricEnrollment(true);
  }
});

afterAll(async () => {
  await device.uninstallApp();
  await device.installApp();
});
