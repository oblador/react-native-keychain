import path from 'path';
import { device } from 'detox';
import cp from 'child_process';

export const enrollBiometric = async () => {
  if (device.getPlatform() === 'android') {
    const script = path.resolve(__dirname, './enrollFingerprintAndroid.sh');
    const result = cp.spawnSync('sh', [script], {
      stdio: 'inherit',
    });

    // Check for errors
    if (result.error) {
      console.error('Error executing script:', result.error);
    }
  } else {
    await device.setBiometricEnrollment(true);
  }
};
