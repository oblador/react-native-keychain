import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import * as Keychain from 'react-native-keychain';

// Exercise the module through its published JS entry point in a SwiftPM app.
async function smokeTest() {
  const service = 'org.react-native-keychain.spm-smoke';
  const server = 'https://spm-smoke.example.invalid';
  const options = { service };
  const assert = (condition: unknown, message: string) => {
    if (!condition) {
      throw new Error(message);
    }
  };

  try {
    await Keychain.resetGenericPassword(options);
    await Keychain.resetInternetCredentials({ server });
    assert(!(await Keychain.getGenericPassword(options)), 'generic missing');
    assert(
      !(await Keychain.getInternetCredentials(server)),
      'internet missing'
    );

    await Keychain.setGenericPassword('spm', 'generic-test-value', options);
    await Keychain.setInternetCredentials(server, 'spm', 'internet-test-value');
    const generic = await Keychain.getGenericPassword(options);
    const internet = await Keychain.getInternetCredentials(server);
    assert(
      generic &&
        generic.username === 'spm' &&
        generic.password === 'generic-test-value',
      'generic round trip'
    );
    assert(
      internet &&
        internet.username === 'spm' &&
        internet.password === 'internet-test-value',
      'internet round trip'
    );
    assert(await Keychain.hasGenericPassword(options), 'generic existence');
    assert(
      await Keychain.hasInternetCredentials({ server }),
      'internet existence'
    );
  } finally {
    await Keychain.resetGenericPassword(options);
    await Keychain.resetInternetCredentials({ server });
  }
  assert(!(await Keychain.hasGenericPassword(options)), 'generic reset');
  assert(
    !(await Keychain.hasInternetCredentials({ server })),
    'internet reset'
  );
}

export default function App() {
  const [status, setStatus] = useState('Running SPM smoke test');
  useEffect(() => {
    smokeTest().then(
      () => setStatus('SPM PASS: generic and internet credentials'),
      (error) => setStatus(`SPM FAIL: ${String(error)}`)
    );
  }, []);

  return (
    <View style={styles.container}>
      <Text testID="spm-result">{status}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', padding: 24 },
});
