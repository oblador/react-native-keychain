import React, { useState } from 'react';
import { Button, Platform, StyleSheet, Text, View } from 'react-native';
import * as Keychain from 'react-native-keychain';

// Only bundled by build:e2e:*. These calls use the public API and real native storage.
const generic = 'keychain.e2e:generic';
const internet = 'https://keychain.e2e:8443';
const protectedId = 'keychain.e2e:protected';
const username = 'użytkownik 🔑';
const password = '密碼\nsecret';
const prompt = { title: 'Keychain test', cancel: 'Cancel' };
type Kind = 'generic' | 'internet';
type Mode = 'biometric' | 'passcode' | 'rsa';

function check(condition: unknown, label: string): asserts condition {
  if (!condition) throw new Error(label);
}

function credentials(value: unknown, label: string) {
  check(typeof value === 'object' && value !== null, `${label}: result shape`);
  const result = value as {
    username: string;
    password: string;
    storage: string;
  };
  check(result.username === username, `${label}: username mismatch`);
  check(result.password === password, `${label}: password mismatch`);
  check(typeof result.storage === 'string', `${label}: missing storage`);
}

const api = (kind: Kind, id: string) => ({
  set: (options: Keychain.SetOptions = {}, user = username) =>
    kind === 'generic'
      ? Keychain.setGenericPassword(user, password, { ...options, service: id })
      : Keychain.setInternetCredentials(id, user, password, options),
  get: (options: Keychain.GetOptions = {}) =>
    kind === 'generic'
      ? Keychain.getGenericPassword({ ...options, service: id })
      : Keychain.getInternetCredentials(id, options),
  has: () =>
    kind === 'generic'
      ? Keychain.hasGenericPassword({ service: id })
      : Keychain.hasInternetCredentials({ server: id }),
  reset: () =>
    kind === 'generic'
      ? Keychain.resetGenericPassword({ service: id })
      : Keychain.resetInternetCredentials({ server: id }),
});

async function cleanup() {
  for (const id of [generic, internet, protectedId]) {
    for (const kind of ['generic', 'internet'] as const)
      await api(kind, id).reset();
  }
}

async function contract() {
  for (const kind of ['generic', 'internet'] as const) {
    const id = kind === 'generic' ? generic : internet;
    const store = api(kind, id);
    check((await store.get()) === false, `${kind}: missing get`);
    check((await store.has()) === false, `${kind}: missing has`);
    const options = Object.freeze({
      authenticationPrompt: Object.freeze({ title: 'Custom prompt' }),
    });
    check(await store.set(options), `${kind}: save result`);
    check(
      !('cancel' in options.authenticationPrompt),
      'caller options mutated'
    );
    credentials(await store.get(), kind);
    check(await store.has(), `${kind}: has after save`);
    await store.set({}, 'replacement');
    const replaced = await store.get();
    check(
      replaced && replaced.username === 'replacement',
      `${kind}: overwrite`
    );
  }
  const servers = await Keychain.getAllInternetCredentialServers();
  check(
    servers.filter((server) => server === internet).length === 1,
    'server identifier or duplicates'
  );
  check(!servers.includes(generic), 'generic credential leaked into servers');
  await api('generic', generic).reset();
  check(await api('internet', internet).has(), 'reset changed unrelated entry');
  await api('internet', internet).reset();
  for (const [kind, id] of [
    ['generic', generic],
    ['internet', internet],
  ] as const) {
    check(
      (await api(kind, id).get()) === false,
      `${kind}: reset did not remove entry`
    );
    check((await api(kind, id).has()) === false, `${kind}: has after reset`);
    await api(kind, id).reset();
  }
}

export default function TestApp() {
  const [status, setStatus] = useState('ready');
  const [kind, setKind] = useState<Kind>('generic');
  const [mode, setMode] = useState<Mode>('biometric');
  const store = api(kind, protectedId);
  const actions: Record<string, () => Promise<unknown>> = {
    cleanup,
    contract,
    seed: async () => {
      await api('generic', generic).set();
      await api('internet', internet).set();
    },
    persisted: async () => {
      credentials(await api('generic', generic).get(), 'generic after restart');
      credentials(
        await api('internet', internet).get(),
        'internet after restart'
      );
      check(
        (await Keychain.getAllInternetCredentialServers()).includes(internet),
        'listing after restart'
      );
    },
    save: () =>
      store.set({
        accessControl:
          mode === 'passcode'
            ? Keychain.ACCESS_CONTROL.DEVICE_PASSCODE
            : Keychain.ACCESS_CONTROL.BIOMETRY_CURRENT_SET,
        ...(Platform.OS === 'android' && mode === 'rsa'
          ? { storage: Keychain.STORAGE_TYPE.RSA }
          : {}),
        authenticationPrompt: prompt,
      }),
    // No accessControl on read: Android must recover the persisted prompt policy.
    load: async () =>
      credentials(
        await store.get({ authenticationPrompt: prompt }),
        'protected read'
      ),
    presence: async () => {
      check(await store.has(), 'protected has');
      const entries =
        kind === 'internet'
          ? await Keychain.getAllInternetCredentialServers()
          : await Keychain.getAllGenericPasswordServices();
      check(entries.includes(protectedId), 'protected listing');
    },
    invalid: async () => {
      try {
        await store.set({}, '');
      } catch (error) {
        check(
          (error as { code?: string }).code ===
            Keychain.ERROR_CODE.INVALID_PARAMETERS,
          'invalid parameter error code'
        );
        check((await store.has()) === false, 'failed write created a record');
        return;
      }
      throw new Error('invalid write unexpectedly succeeded');
    },
  };
  const run = async (name: string) => {
    setStatus(`${name}:pending`);
    try {
      await actions[name]();
      setStatus(`${name}:ok`);
    } catch (error) {
      // Never render credential values or native error messages in test artifacts.
      const code = (error as { code?: string }).code;
      setStatus(`${name}:error:${code ?? (error as Error).message}`);
    }
  };
  return (
    <View style={styles.screen}>
      <Text testID="status">{status}</Text>
      <Text testID="selection">
        {kind}:{mode}
      </Text>
      {(['generic', 'internet'] as const).map((value) => (
        <Button
          key={value}
          testID={value}
          title={value}
          onPress={() => setKind(value)}
        />
      ))}
      {(['biometric', 'passcode', 'rsa'] as const).map((value) => (
        <Button
          key={value}
          testID={value}
          title={value}
          onPress={() => setMode(value)}
        />
      ))}
      {Object.keys(actions).map((name) => (
        <Button
          key={name}
          testID={name}
          title={name}
          onPress={() => {
            run(name);
          }}
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, paddingTop: 60, paddingHorizontal: 16, gap: 3 },
});
