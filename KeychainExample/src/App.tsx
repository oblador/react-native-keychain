import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  TextInput,
  TouchableOpacity,
  ScrollView,
  Platform,
  KeyboardAvoidingView,
  Keyboard,
} from 'react-native';
import SegmentedControlTab from 'react-native-segmented-control-tab';
import * as Keychain from 'react-native-keychain';

// Storage Type Options
const STORAGE_TYPES = [
  { label: 'Default', value: undefined },
  { label: 'AES (No Auth)', value: Keychain.STORAGE_TYPE.AES_GCM_NO_AUTH },
  { label: 'AES (Auth)', value: Keychain.STORAGE_TYPE.AES_GCM },
  { label: 'RSA', value: Keychain.STORAGE_TYPE.RSA },
  { label: 'Knox', value: Keychain.STORAGE_TYPE.KNOX },
];

// Access Control Options
const ACCESS_CONTROLS_IOS = [
  { label: 'None', value: undefined },
  { label: 'Biometry', value: Keychain.ACCESS_CONTROL.BIOMETRY_ANY },
  { label: 'Biometry + Passcode', value: Keychain.ACCESS_CONTROL.BIOMETRY_ANY_OR_DEVICE_PASSCODE },
  { label: 'Passcode', value: Keychain.ACCESS_CONTROL.DEVICE_PASSCODE },
];

const ACCESS_CONTROLS_ANDROID = [
  { label: 'None', value: undefined },
  { label: 'Biometry', value: Keychain.ACCESS_CONTROL.BIOMETRY_CURRENT_SET },
  { label: 'Biometry + Passcode', value: Keychain.ACCESS_CONTROL.BIOMETRY_ANY_OR_DEVICE_PASSCODE },
  { label: 'Passcode', value: Keychain.ACCESS_CONTROL.DEVICE_PASSCODE },
];

// Security Levels
const SECURITY_LEVELS = [
  { label: 'Any', value: Keychain.SECURITY_LEVEL.ANY },
  { label: 'Software', value: Keychain.SECURITY_LEVEL.SECURE_SOFTWARE },
  { label: 'Hardware', value: Keychain.SECURITY_LEVEL.SECURE_HARDWARE },
];

export default function App() {
  // State
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [status, setStatus] = useState('');

  // Options
  const [selectedStorageIndex, setSelectedStorageIndex] = useState(0);
  const [selectedAccessIndex, setSelectedAccessIndex] = useState(2);
  const [selectedSecurityIndex, setSelectedSecurityIndex] = useState(0);
  const [useKnox, setUseKnox] = useState(false);

  // Info
  const [biometryType, setBiometryType] = useState<Keychain.BIOMETRY_TYPE | null>(null);
  const [isKnoxAvailable, setIsKnoxAvailable] = useState(false);
  const [hasCredentials, setHasCredentials] = useState(false);

  const accessControls = Platform.OS === 'ios' ? ACCESS_CONTROLS_IOS : ACCESS_CONTROLS_ANDROID;

  useEffect(() => {
    // Check capabilities
    Keychain.getSupportedBiometryType().then(setBiometryType);
    Keychain.hasGenericPassword().then(setHasCredentials);

    if (Platform.OS === 'android') {
      Keychain.isKnoxAvailable().then((available) => {
        setIsKnoxAvailable(available);
        // Auto-select Knox in storage if available
        if (available) {
          const knoxIndex = STORAGE_TYPES.findIndex(t => t.value === Keychain.STORAGE_TYPE.KNOX);
          if (knoxIndex > 0) setSelectedStorageIndex(knoxIndex);
        }
      });
    }
  }, []);

  // Build options object
  const getOptions = () => {
    const options: any = {};

    // Storage type
    const selectedStorage = STORAGE_TYPES[selectedStorageIndex];
    if (selectedStorage.value) {
      options.storage = selectedStorage.value;
    }

    // Access control
    const selectedAccess = accessControls[selectedAccessIndex];
    if (selectedAccess.value) {
      options.accessControl = selectedAccess.value;
    }

    // Security level
    const selectedSecurity = SECURITY_LEVELS[selectedSecurityIndex];
    options.securityLevel = selectedSecurity.value;

    // Knox flag (alternative to storage type)
    if (useKnox && Platform.OS === 'android') {
      options.useKnox = true;
    }

    return options;
  };

  // Save credentials
  const save = async () => {
    try {
      setStatus('💾 Saving...');
      const options = getOptions();

      console.log('Save options:', options);

      await Keychain.setGenericPassword(username, password, options);

      setStatus(`✅ Saved successfully!\nOptions: ${JSON.stringify(options, null, 2)}`);
      setHasCredentials(true);
    } catch (err: any) {
      setStatus(`❌ Error: ${err.message || err}`);
      console.error('Save error:', err);
    }
  };

  // Load credentials
  const load = async () => {
    try {
      setStatus('📂 Loading...');
      const creds = await Keychain.getGenericPassword();
      console.log('Save options:', creds)

      if (creds) {
        setUsername(creds.username);
        setPassword(creds.password);
        setStatus(
          `✅ Loaded!\n` +
          `Username: ${creds.username}\n` +
          `Password: ${creds.password}\n` +
          `Storage: ${creds.storage || 'default'}`
        );
      } else {
        setStatus('❌ No credentials found');
      }
    } catch (err: any) {
      setStatus(`❌ Error: ${err.message || err}`);
      console.error('Load error:', err);
    }
  };

  // Reset credentials
  const reset = async () => {
    try {
      setStatus('🗑️ Clearing...');
      await Keychain.resetGenericPassword();
      setUsername('');
      setPassword('');
      setStatus('✅ Credentials cleared');
      setHasCredentials(false);
    } catch (err: any) {
      setStatus(`❌ Error: ${err.message || err}`);
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={styles.container}
    >
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title} onPress={Keyboard.dismiss}>
          🔐 React Native Keychain
        </Text>

        {/* Device Info */}
        <View style={styles.infoCard}>
          <Text style={styles.infoTitle}>📱 Device Info</Text>
          <Text style={styles.infoText}>
            Biometry: <Text style={styles.bold}>{biometryType || 'Not available'}</Text>
          </Text>
          {Platform.OS === 'android' && (
            <Text style={styles.infoText}>
              Knox: <Text style={styles.bold}>{isKnoxAvailable ? '✅ Available' : '❌ Not available'}</Text>
            </Text>
          )}
          <Text style={styles.infoText}>
            Saved: <Text style={styles.bold}>{hasCredentials ? '✅ Yes' : '❌ No'}</Text>
          </Text>
        </View>

        {/* Credentials Input */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>📝 Credentials</Text>
          <TextInput
            style={styles.input}
            placeholder="Username"
            value={username}
            onChangeText={setUsername}
            autoCapitalize="none"
          />
          <TextInput
            style={styles.input}
            placeholder="Password"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
          />
        </View>

        {/* Storage Type */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>💾 Storage Type</Text>
          <Text style={styles.hint}>Choose encryption method</Text>
          <SegmentedControlTab
            tabTextStyle={styles.tabText}
            tabStyle={styles.tab}
            activeTabStyle={styles.activeTab}
            selectedIndex={selectedStorageIndex}
            values={STORAGE_TYPES.map(t => t.label)}
            onTabPress={setSelectedStorageIndex}
          />
          {STORAGE_TYPES[selectedStorageIndex].value === Keychain.STORAGE_TYPE.KNOX && (
            <Text style={styles.infoSmall}>
              ℹ️ Knox uses hardware-backed encryption on Samsung devices
            </Text>
          )}
        </View>

        {/* Access Control */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>🔒 Access Control</Text>
          <Text style={styles.hint}>Authentication requirement</Text>
          <SegmentedControlTab
            tabTextStyle={styles.tabText}
            tabStyle={styles.tab}
            activeTabStyle={styles.activeTab}
            selectedIndex={selectedAccessIndex}
            values={accessControls.map(a => a.label)}
            onTabPress={setSelectedAccessIndex}
          />
          {selectedAccessIndex > 0 && (
            <Text style={styles.infoSmall}>
              ℹ️ Will require {accessControls[selectedAccessIndex].label.toLowerCase()} to access
            </Text>
          )}
        </View>

        {/* Security Level */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>🛡️ Security Level</Text>
          <Text style={styles.hint}>Hardware vs Software encryption</Text>
          <SegmentedControlTab
            tabTextStyle={styles.tabText}
            tabStyle={styles.tab}
            activeTabStyle={styles.activeTab}
            selectedIndex={selectedSecurityIndex}
            values={SECURITY_LEVELS.map(s => s.label)}
            onTabPress={setSelectedSecurityIndex}
          />
        </View>

        {/* Knox Toggle (Android only) */}
        {Platform.OS === 'android' && isKnoxAvailable && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>🔐 Knox Options</Text>
            <Text style={styles.hint}>Alternative: Use Knox flag instead of storage type</Text>
            <SegmentedControlTab
              tabTextStyle={styles.tabText}
              tabStyle={styles.tab}
              activeTabStyle={styles.activeTab}
              selectedIndex={useKnox ? 0 : 1}
              values={['Use Knox', "Don't Use Knox"]}
              onTabPress={(index) => setUseKnox(index === 0)}
            />
            <Text style={styles.infoSmall}>
              ℹ️ When enabled, Knox hardware security will be used regardless of storage type selection
            </Text>
          </View>
        )}

        {/* Action Buttons */}
        <View style={styles.buttonContainer}>
          <TouchableOpacity style={[styles.button, styles.saveButton]} onPress={save}>
            <Text style={styles.buttonText}>💾 Save</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[styles.button, styles.loadButton]} onPress={load}>
            <Text style={styles.buttonText}>📂 Load</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[styles.button, styles.resetButton]} onPress={reset}>
            <Text style={styles.buttonText}>🗑️ Clear</Text>
          </TouchableOpacity>
        </View>

        {/* Status Display */}
        {!!status && (
          <View style={styles.statusCard}>
            <Text style={styles.statusText}>{status}</Text>
          </View>
        )}

        {/* Help Section */}
        <View style={styles.helpCard}>
          <Text style={styles.helpTitle}>💡 Quick Guide</Text>
          <Text style={styles.helpText}>
            • <Text style={styles.bold}>Storage Type:</Text> Encryption method (AES, RSA, Knox)
          </Text>
          <Text style={styles.helpText}>
            • <Text style={styles.bold}>Access Control:</Text> When to ask for biometric/passcode
          </Text>
          <Text style={styles.helpText}>
            • <Text style={styles.bold}>Security Level:</Text> Hardware (secure) vs Software
          </Text>
          <Text style={styles.helpText}>
            • <Text style={styles.bold}>Knox:</Text> Samsung hardware security (if available)
          </Text>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  content: {
    padding: 20,
    paddingBottom: 40,
  },
  title: {
    fontSize: 26,
    fontWeight: 'bold',
    textAlign: 'center',
    marginVertical: 20,
    color: '#212529',
  },

  // Cards & Sections
  infoCard: {
    backgroundColor: '#e7f3ff',
    padding: 15,
    borderRadius: 12,
    marginBottom: 15,
    borderLeftWidth: 4,
    borderLeftColor: '#0066cc',
  },
  section: {
    backgroundColor: 'white',
    padding: 16,
    borderRadius: 12,
    marginBottom: 15,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 4,
    elevation: 3,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '700',
    marginBottom: 4,
    color: '#212529',
  },
  hint: {
    fontSize: 13,
    color: '#6c757d',
    marginBottom: 12,
  },
  infoTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
    color: '#212529',
  },
  infoText: {
    fontSize: 14,
    color: '#495057',
    marginBottom: 4,
  },
  infoSmall: {
    fontSize: 12,
    color: '#6c757d',
    marginTop: 10,
    fontStyle: 'italic',
  },
  bold: {
    fontWeight: '700',
  },

  // Input
  input: {
    borderWidth: 1,
    borderColor: '#dee2e6',
    borderRadius: 8,
    padding: 12,
    marginBottom: 10,
    fontSize: 16,
    backgroundColor: '#ffffff',
    color: '#212529',
  },

  // Tabs
  tabText: {
    fontSize: 13,
    fontWeight: '500',
  },
  tab: {
    padding: 8,
    borderColor: '#dee2e6',
    backgroundColor: '#f8f9fa',
  },
  activeTab: {
    backgroundColor: '#0066cc',
  },

  // Buttons
  buttonContainer: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 10,
  },
  button: {
    flex: 1,
    padding: 16,
    borderRadius: 10,
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 3,
    elevation: 2,
  },
  saveButton: {
    backgroundColor: '#28a745',
  },
  loadButton: {
    backgroundColor: '#007bff',
  },
  resetButton: {
    backgroundColor: '#dc3545',
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '700',
  },

  // Status
  statusCard: {
    backgroundColor: '#fff3cd',
    padding: 15,
    borderRadius: 12,
    marginTop: 15,
    borderLeftWidth: 4,
    borderLeftColor: '#ffc107',
  },
  statusText: {
    fontSize: 14,
    color: '#212529',
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
  },

  // Help
  helpCard: {
    backgroundColor: '#f1f3f5',
    padding: 15,
    borderRadius: 12,
    marginTop: 15,
  },
  helpTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 10,
    color: '#212529',
  },
  helpText: {
    fontSize: 13,
    color: '#495057',
    marginBottom: 6,
    lineHeight: 20,
  },
});
