import { device } from 'detox';

beforeAll(async () => {
  if (device.getPlatform() === 'ios') {
    await device.setBiometricEnrollment(true);
  }
});
