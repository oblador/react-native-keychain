import React, { useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Keyboard,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import * as Keychain from 'react-native-keychain';
import { colors } from './theme';
import { OptionPicker } from './components/OptionPicker';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';

const ACCESS_CONTROL_OPTIONS = ['None', 'Passcode', 'Password'];
const ACCESS_CONTROL_OPTIONS_ANDROID = ['None', 'Passcode'];
const ACCESS_CONTROL_MAP = [
  null,
  Keychain.ACCESS_CONTROL.DEVICE_PASSCODE,
  Keychain.ACCESS_CONTROL.APPLICATION_PASSWORD,
  Keychain.ACCESS_CONTROL.BIOMETRY_CURRENT_SET,
];
const ACCESS_CONTROL_MAP_ANDROID = [
  null,
  Keychain.ACCESS_CONTROL.DEVICE_PASSCODE,
  Keychain.ACCESS_CONTROL.BIOMETRY_CURRENT_SET,
];
const SECURITY_LEVEL_OPTIONS = ['Any', 'Software', 'Hardware'];
const SECURITY_LEVEL_MAP = [
  Keychain.SECURITY_LEVEL.ANY,
  Keychain.SECURITY_LEVEL.SECURE_SOFTWARE,
  Keychain.SECURITY_LEVEL.SECURE_HARDWARE,
];

const SECURITY_STORAGE_OPTIONS = [
  'Best',
  'AES_CBC',
  'AES_GCM',
  'AES_GCM_NO_AUTH',
  'RSA',
];
const SECURITY_STORAGE_MAP = [
  null,
  Keychain.STORAGE_TYPE.AES_CBC,
  Keychain.STORAGE_TYPE.AES_GCM,
  Keychain.STORAGE_TYPE.AES_GCM_NO_AUTH,
  Keychain.STORAGE_TYPE.RSA,
];

const TYPE_OPTIONS = ['genericPassword', 'internetCredentials'];
const CLOUD_SYNC_OPTIONS = ['Default', 'Disabled', 'Enabled'];
const CLOUD_SYNC_MAP = [undefined, false, true];

const SERVER = 'https://example.com'; // taste-skill-ignore: reserved E2E credential namespace.
const MASK = '••••••••';
const mono = Platform.OS === 'ios' ? 'Menlo' : 'monospace';
const authenticationPrompt = {
  title: 'Authentication required',
  subtitle: 'React Native Keychain',
  description: 'Authenticate to continue with this credential.',
  cancel: 'Cancel',
};

type Operation = 'save' | 'load' | 'list' | 'reset';
type Outcome = {
  title: string;
  detail: string;
  error?: boolean;
  duration?: number;
};

function Choices({
  id,
  values,
  labels = values,
  selected,
  onSelect,
  disabled,
}: {
  id: string;
  values: string[];
  labels?: string[];
  selected: number;
  onSelect: (index: number) => void;
  disabled: boolean;
}) {
  return (
    <View style={styles.choices}>
      {values.map((value, index) => (
        <Pressable
          key={value}
          testID={`${id}-${value}`}
          accessibilityRole="radio"
          accessibilityLabel={labels[index]}
          accessibilityState={{ selected: index === selected, disabled }}
          disabled={disabled}
          onPress={() => {
            Keyboard.dismiss();
            onSelect(index);
          }}
          style={({ pressed }) => [
            styles.choice,
            index === selected && styles.choiceSelected,
            pressed && styles.pressed,
          ]}
        >
          <Text
            style={[
              styles.choiceText,
              index === selected && styles.selectedText,
            ]}
          >
            {labels[index]}
          </Text>
        </Pressable>
      ))}
    </View>
  );
}

function ResultRow({
  label,
  value,
  id,
}: {
  label: string;
  value: string;
  id: string;
}) {
  return (
    <View style={styles.resultRow}>
      <Text style={styles.meta}>{label}</Text>
      <Text testID={id} selectable style={styles.resultValue}>
        {value}
      </Text>
    </View>
  );
}

export default function App() {
  return (
    <SafeAreaProvider>
      <Workbench />
    </SafeAreaProvider>
  );
}

function Workbench() {
  const scroll = useRef<ScrollView>(null);
  const passwordInput = useRef<TextInput>(null);
  const operationInFlight = useRef(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [type, setType] = useState(TYPE_OPTIONS[0]);
  const [biometryType, setBiometryType] =
    useState<Keychain.BIOMETRY_TYPE | null>(null);
  const [selectedAccessControlIndex, setSelectedAccessControlIndex] =
    useState(0);
  const [selectedSecurityIndex, setSelectedSecurityIndex] = useState(0);
  const [selectedStorageIndex, setSelectedStorageIndex] = useState(0);
  const [selectedCloudSyncIndex, setSelectedCloudSyncIndex] = useState(0);
  const [presence, setPresence] = useState<{
    generic: boolean | null;
    internet: boolean | null;
  }>({ generic: null, internet: null });
  const [refresh, setRefresh] = useState(0);
  const [busy, setBusy] = useState<Operation | null>(null);
  const [outcome, setOutcome] = useState<Outcome | null>(null);
  const [loaded, setLoaded] = useState<Keychain.UserCredentials | null>(null);
  const [revealed, setRevealed] = useState(false);
  const cloudSync = CLOUD_SYNC_MAP[selectedCloudSyncIndex];
  const cloudSyncOptions = cloudSync === undefined ? {} : { cloudSync };
  const accessValues =
    Platform.OS === 'ios'
      ? ACCESS_CONTROL_OPTIONS
      : ACCESS_CONTROL_OPTIONS_ANDROID;
  const accessMap =
    Platform.OS === 'ios' ? ACCESS_CONTROL_MAP : ACCESS_CONTROL_MAP_ANDROID;
  const accessControl = accessMap[selectedAccessControlIndex] || undefined;
  const disabled = busy !== null;

  useEffect(() => {
    let active = true;
    Keychain.getSupportedBiometryType()
      .then((result) => {
        if (active) {
          setBiometryType(result);
        }
      })
      .catch(() => {
        /* Unavailable biometrics leave the manual options usable. */
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    const options = cloudSync === undefined ? {} : { cloudSync };
    setPresence({ generic: null, internet: null });
    Promise.all([
      Keychain.hasGenericPassword(options).catch(() => null),
      Keychain.hasInternetCredentials({ ...options, server: SERVER }).catch(
        () => null
      ),
    ]).then(([generic, internet]) => {
      if (active) {
        setPresence({ generic, internet });
      }
    });
    return () => {
      active = false;
    };
  }, [cloudSync, refresh]);

  useEffect(() => {
    if (outcome) {
      const frame = requestAnimationFrame(() =>
        scroll.current?.scrollToEnd({ animated: false })
      );
      return () => cancelAnimationFrame(frame);
    }
    return undefined;
  }, [outcome]);

  const clearResult = () => {
    setOutcome(null);
    setLoaded(null);
    setRevealed(false);
  };

  const run = async (operation: Operation) => {
    if (operationInFlight.current) {
      return;
    }
    operationInFlight.current = true;
    Keyboard.dismiss();
    setBusy(operation);
    clearResult();
    const start = Date.now();
    try {
      const options = {
        ...cloudSyncOptions,
        accessControl,
        authenticationPrompt,
      };
      if (operation === 'save') {
        const setOptions = {
          ...options,
          securityLevel: SECURITY_LEVEL_MAP[selectedSecurityIndex],
          storage: SECURITY_STORAGE_MAP[selectedStorageIndex] || undefined,
        };
        if (type === 'internetCredentials') {
          await Keychain.setInternetCredentials(
            SERVER,
            username,
            password,
            setOptions
          );
        } else {
          await Keychain.setGenericPassword(username, password, setOptions);
        }
        setUsername('');
        setPassword('');
        setOutcome({
          title: 'Credentials saved',
          detail: 'Ready to retrieve from secure storage.',
          duration: Date.now() - start,
        });
      } else if (operation === 'load') {
        const credentials =
          type === 'internetCredentials'
            ? await Keychain.getInternetCredentials(SERVER, options)
            : await Keychain.getGenericPassword(options);
        if (credentials) {
          setLoaded(credentials);
          setOutcome({
            title: 'Credentials loaded',
            detail: 'Retrieved from secure storage.',
            duration: Date.now() - start,
          });
        } else {
          setOutcome({
            title: 'No credentials stored',
            detail: 'Save a credential to try it out.',
          });
        }
      } else if (operation === 'list') {
        const identifiers =
          type === 'internetCredentials'
            ? await Keychain.getAllInternetCredentialServers(cloudSyncOptions)
            : await Keychain.getAllGenericPasswordServices();
        setOutcome({
          title:
            type === 'internetCredentials'
              ? 'Servers listed'
              : 'Services listed',
          detail: JSON.stringify(identifiers),
          duration: Date.now() - start,
        });
      } else {
        await Keychain.resetGenericPassword(cloudSyncOptions);
        await Keychain.resetInternetCredentials({
          ...cloudSyncOptions,
          server: SERVER,
        });
        setUsername('');
        setPassword('');
        setOutcome({
          title: 'Credentials reset',
          detail: 'Both credential types cleared for this sync setting.',
        });
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      setOutcome({
        title: `Could not ${operation} credentials`,
        detail: message,
        error: true,
      });
    } finally {
      operationInFlight.current = false;
      setBusy(null);
      setRefresh((value) => value + 1);
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar barStyle="light-content" backgroundColor={colors.ground} />
      <KeyboardAvoidingView
        style={styles.container}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <View style={styles.header}>
          <Pressable
            testID="appTitle"
            accessibilityRole="button"
            accessibilityLabel="Dismiss keyboard"
            onPress={Keyboard.dismiss}
            style={styles.brand}
          >
            <View style={styles.keyMark} accessible={false}>
              <View style={styles.keyRing} />
              <View style={styles.keyStem} />
              <View style={styles.keyTooth} />
            </View>
            <Text accessibilityRole="header" style={styles.brandName}>
              Keychain Example
            </Text>
          </Pressable>
        </View>
        <View style={styles.typeTabs}>
          <Choices
            id="type"
            values={TYPE_OPTIONS}
            labels={['Generic password', 'Internet credentials']}
            selected={TYPE_OPTIONS.indexOf(type)}
            disabled={disabled}
            onSelect={(index) => {
              setType(TYPE_OPTIONS[index]);
              clearResult();
            }}
          />
        </View>
        <ScrollView
          ref={scroll}
          testID="workbenchScroll"
          style={styles.scroll}
          contentContainerStyle={styles.content}
          keyboardShouldPersistTaps="handled"
          keyboardDismissMode="on-drag"
          showsVerticalScrollIndicator={false}
        >
          <Text testID="credentialScope" style={styles.scope}>
            {type === 'internetCredentials' ? SERVER : 'Default app service'}
          </Text>
          <View style={styles.credentialPanel}>
            <Text style={styles.label}>Username</Text>
            <TextInput
              testID="usernameInput"
              accessibilityLabel="Username"
              style={styles.input}
              value={username}
              onChangeText={setUsername}
              placeholder="Your username"
              placeholderTextColor={colors.faint}
              selectionColor={colors.accent}
              keyboardAppearance="dark"
              autoCapitalize="none"
              autoCorrect={false}
              editable={!disabled}
              returnKeyType="next"
              submitBehavior="submit"
              onSubmitEditing={() => passwordInput.current?.focus()}
            />
            <Text style={styles.label}>Password</Text>
            <TextInput
              ref={passwordInput}
              testID="passwordInput"
              accessibilityLabel="Password"
              style={styles.input}
              value={password}
              onChangeText={setPassword}
              placeholder="Enter password"
              placeholderTextColor={colors.faint}
              selectionColor={colors.accent}
              keyboardAppearance="dark"
              secureTextEntry
              autoCapitalize="none"
              autoCorrect={false}
              editable={!disabled}
              returnKeyType="done"
              onSubmitEditing={Keyboard.dismiss}
            />
          </View>
          <View style={styles.sectionHeading}>
            <Text style={styles.sectionTitle}>Protection</Text>
            <Text style={styles.meta}>
              {Platform.OS === 'ios' ? 'Apple Keychain' : 'Android Keystore'}
            </Text>
          </View>
          <View style={styles.settingsPanel}>
            <OptionPicker
              id="access"
              title="Access control"
              values={
                biometryType ? [...accessValues, biometryType] : accessValues
              }
              labels={
                biometryType
                  ? [...accessValues, biometryType.replace('ID', ' ID')]
                  : accessValues
              }
              descriptions={[
                'No additional authentication.',
                'Require the device passcode.',
                ...(Platform.OS === 'ios'
                  ? ['Use an application-provided password.']
                  : []),
                ...(biometryType
                  ? ['Require the current enrolled biometrics.']
                  : []),
              ]}
              selected={selectedAccessControlIndex}
              disabled={disabled}
              onSelect={setSelectedAccessControlIndex}
            />
            {Platform.OS === 'ios' ? (
              <OptionPicker
                id="sync"
                title="iCloud sync"
                last
                values={CLOUD_SYNC_OPTIONS}
                selected={selectedCloudSyncIndex}
                disabled={disabled}
                descriptions={[
                  'Use the library default (off).',
                  'Store only on this device.',
                  'Allow synchronization through iCloud Keychain.',
                ]}
                onSelect={(index) => {
                  setSelectedCloudSyncIndex(index);
                  clearResult();
                }}
              />
            ) : (
              <>
                <OptionPicker
                  id="security"
                  title="Security level"
                  values={SECURITY_LEVEL_OPTIONS}
                  selected={selectedSecurityIndex}
                  disabled={disabled}
                  descriptions={[
                    'Use the available security level.',
                    'Require software protection or better.',
                    'Require hardware-backed key storage.',
                  ]}
                  onSelect={setSelectedSecurityIndex}
                />
                <OptionPicker
                  id="storage"
                  title="Cipher storage"
                  last
                  values={SECURITY_STORAGE_OPTIONS}
                  labels={[
                    'Automatic',
                    'AES-CBC',
                    'AES-GCM',
                    'AES-GCM · no auth',
                    'RSA',
                  ]}
                  descriptions={[
                    'Let Keychain select the cipher.',
                    'Legacy AES-CBC encryption.',
                    'AES-GCM with authentication.',
                    'AES-GCM without authentication.',
                    'Asymmetric RSA encryption.',
                  ]}
                  selected={selectedStorageIndex}
                  disabled={disabled}
                  onSelect={setSelectedStorageIndex}
                />
              </>
            )}
          </View>
          <View style={styles.presence}>
            {(['generic', 'internet'] as const).map((key) => (
              <View key={key} style={styles.presenceItem}>
                <View
                  style={[
                    styles.dot,
                    presence[key] === true && styles.storedDot,
                  ]}
                />
                <Text style={styles.meta}>
                  {key === 'generic' ? 'Generic' : 'Internet'}
                </Text>
                <Text testID={`${key}Presence`} style={styles.presenceValue}>
                  {presence[key] === null
                    ? '—'
                    : presence[key]
                      ? 'Stored'
                      : 'Empty'}
                </Text>
              </View>
            ))}
          </View>
          <View style={styles.sectionHeading}>
            <Text style={styles.sectionTitle}>Result</Text>
            {outcome?.duration !== undefined && (
              <Text style={styles.duration}>{outcome.duration} ms</Text>
            )}
          </View>
          <View testID="resultPanel" style={styles.panel}>
            <View style={styles.resultHeading}>
              {busy && <ActivityIndicator color={colors.accent} size="small" />}
              <Text
                testID="statusMessage"
                accessibilityLiveRegion="polite"
                style={[styles.resultTitle, outcome?.error && styles.error]}
              >
                {busy
                  ? 'Waiting for secure storage…'
                  : (outcome?.title ?? 'No operation yet')}
              </Text>
            </View>
            <Text testID="statusDetail" style={styles.resultDetail}>
              {outcome?.detail ??
                'Save, load, and test credentials on this device.'}
            </Text>
            {loaded && (
              <View style={styles.loaded}>
                <ResultRow
                  id="loadedUsername"
                  label="Username"
                  value={loaded.username}
                />
                <ResultRow
                  id="loadedPassword"
                  label="Password"
                  value={revealed ? loaded.password : MASK}
                />
                <Pressable
                  testID="revealPassword"
                  accessibilityRole="button"
                  onPress={() => setRevealed((value) => !value)}
                  style={styles.reveal}
                >
                  <Text style={styles.revealText}>
                    {revealed ? 'Hide password' : 'Reveal password'}
                  </Text>
                </Pressable>
                <ResultRow
                  id="loadedStorage"
                  label="Storage"
                  value={loaded.storage}
                />
                <ResultRow
                  id="loadedService"
                  label={type === 'internetCredentials' ? 'Server' : 'Service'}
                  value={
                    ('server' in loaded && typeof loaded.server === 'string'
                      ? loaded.server
                      : loaded.service) || 'Default app service'
                  }
                />
              </View>
            )}
          </View>
        </ScrollView>
        <View style={styles.actionBar}>
          <View style={styles.actions}>
            {(['save', 'load', 'list', 'reset'] as const).map((operation) => (
              <Pressable
                key={operation}
                testID={`${operation}Button`}
                accessibilityRole="button"
                accessibilityLabel={
                  operation === 'reset'
                    ? 'Reset both credential types'
                    : `${operation === 'save' ? 'Save' : operation === 'load' ? 'Load' : 'List'} credentials`
                }
                accessibilityState={{ disabled, busy: busy === operation }}
                disabled={disabled}
                onPress={() => run(operation)}
                style={({ pressed }) => [
                  styles.action,
                  operation === 'save' && styles.primaryAction,
                  operation === 'reset' && styles.resetAction,
                  pressed && styles.pressed,
                ]}
              >
                <Text
                  style={[
                    styles.actionText,
                    operation === 'save' && styles.primaryText,
                  ]}
                >
                  {operation === 'save'
                    ? 'Save'
                    : operation === 'load'
                      ? 'Load'
                      : operation === 'list'
                        ? 'List'
                        : 'Reset'}
                </Text>
              </Pressable>
            ))}
          </View>
          <Text style={styles.actionHint}>
            {busy
              ? 'Complete authentication to continue.'
              : 'Reset clears both credential types in this sync setting.'}
          </Text>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

// One centered column: screen width − 2 × 22pt gutters, capped at 560pt on tablets.
const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.ground,
  },
  container: { flex: 1 },
  header: {
    width: '100%',
    maxWidth: 604,
    alignSelf: 'center',
    paddingHorizontal: 22,
    paddingVertical: 12,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  brand: { minHeight: 44, flexDirection: 'row', alignItems: 'center', gap: 12 },
  brandName: {
    color: colors.text,
    fontSize: 20,
    fontWeight: '600',
    letterSpacing: -0.5,
  },
  keyMark: { width: 28, height: 32, transform: [{ rotate: '-35deg' }] },
  keyRing: {
    position: 'absolute',
    top: 0,
    left: 5,
    width: 17,
    height: 17,
    borderWidth: 2.5,
    borderColor: colors.accent,
    borderRadius: 9,
  },
  keyStem: {
    position: 'absolute',
    top: 15,
    left: 12,
    width: 3,
    height: 16,
    backgroundColor: colors.accent,
  },
  keyTooth: {
    position: 'absolute',
    top: 24,
    left: 14,
    width: 7,
    height: 3,
    backgroundColor: colors.accent,
  },
  typeTabs: {
    width: '100%',
    maxWidth: 604,
    alignSelf: 'center',
    paddingHorizontal: 22,
  },
  scroll: { flex: 1 },
  content: {
    width: '100%',
    maxWidth: 604,
    alignSelf: 'center',
    paddingHorizontal: 22,
    paddingBottom: 12,
  },
  choices: { flexDirection: 'row', flexWrap: 'wrap', gap: 4 },
  choice: {
    minHeight: 44,
    flexGrow: 1,
    paddingHorizontal: 12,
    paddingVertical: 12,
    justifyContent: 'center',
    alignItems: 'center',
    borderRadius: 22,
  },
  choiceSelected: { backgroundColor: colors.raised },
  choiceText: { color: colors.muted, fontSize: 14, fontWeight: '500' },
  selectedText: { color: colors.text },
  pressed: { opacity: 0.65 },
  scope: {
    color: colors.faint,
    fontFamily: mono,
    fontSize: 12,
    marginTop: 10,
    marginBottom: 20,
  },
  credentialPanel: { gap: 8 },
  label: {
    color: colors.muted,
    fontSize: 12,
    fontWeight: '500',
    marginBottom: 4,
  },
  input: {
    color: colors.text,
    backgroundColor: colors.surface,
    minHeight: 48,
    borderRadius: 8,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 16,
    marginBottom: 6,
  },
  sectionHeading: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 14,
    marginTop: 22,
    marginBottom: 12,
  },
  sectionTitle: { color: colors.text, fontSize: 16, fontWeight: '600' },
  meta: { color: colors.faint, fontSize: 12, lineHeight: 18 },
  panel: { backgroundColor: colors.surface, borderRadius: 20, padding: 16 },
  settingsPanel: {
    backgroundColor: colors.surface,
    borderRadius: 16,
    paddingHorizontal: 16,
    paddingVertical: 2,
  },
  presence: { flexDirection: 'row', flexWrap: 'wrap', gap: 20, paddingTop: 14 },
  presenceItem: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  presenceValue: { color: colors.muted, fontSize: 12 },
  dot: { width: 5, height: 5, borderRadius: 3, backgroundColor: colors.faint },
  storedDot: { backgroundColor: colors.accent },
  duration: {
    color: colors.accent,
    fontSize: 12,
    fontFamily: mono,
    fontVariant: ['tabular-nums'],
  },
  resultHeading: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  resultTitle: {
    color: colors.text,
    fontSize: 16,
    fontWeight: '500',
    flexShrink: 1,
  },
  error: { color: colors.negative },
  resultDetail: {
    color: colors.muted,
    fontSize: 14,
    lineHeight: 20,
    marginTop: 6,
  },
  loaded: { marginTop: 16 },
  resultRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 14,
    paddingVertical: 6,
  },
  resultValue: {
    color: colors.text,
    fontFamily: mono,
    fontSize: 12,
    lineHeight: 18,
    textAlign: 'right',
    flexShrink: 1,
  },
  reveal: { minHeight: 44, justifyContent: 'center', alignSelf: 'flex-end' },
  revealText: {
    color: colors.muted,
    fontSize: 12,
    textDecorationLine: 'underline',
  },
  actionBar: {
    width: '100%',
    maxWidth: 604,
    alignSelf: 'center',
    paddingHorizontal: 22,
    paddingTop: 12,
    paddingBottom: 8,
    backgroundColor: colors.ground,
  },
  actions: { flexDirection: 'row', gap: 10 },
  action: {
    flex: 1,
    minHeight: 48,
    borderRadius: 24,
    backgroundColor: colors.raised,
    alignItems: 'center',
    justifyContent: 'center',
  },
  primaryAction: { backgroundColor: colors.text },
  resetAction: { backgroundColor: colors.surface, flex: 0.8 },
  actionText: { color: colors.text, fontSize: 14, fontWeight: '600' },
  primaryText: { color: colors.ground },
  actionHint: {
    color: colors.faint,
    fontSize: 11,
    lineHeight: 16,
    textAlign: 'center',
    marginTop: 10,
  },
});
