import React, { useState, useEffect } from 'react';
import {
  Keyboard,
  KeyboardAvoidingView,
  Platform,
  StyleSheet,
  Text,
  TextInput,
  TouchableHighlight,
  View,
} from 'react-native';
import SegmentedControlTab from 'react-native-segmented-control-tab';
import * as Keychain from 'react-native-keychain';

const ACCESS_CONTROL_OPTIONS = ['None', 'Passcode', 'Password'];
const ACCESS_CONTROL_OPTIONS_ANDROID = ['None'];
const ACCESS_CONTROL_MAP = [
  null,
  Keychain.ACCESS_CONTROL.DEVICE_PASSCODE,
  Keychain.ACCESS_CONTROL.APPLICATION_PASSWORD,
  Keychain.ACCESS_CONTROL.BIOMETRY_CURRENT_SET,
];
const ACCESS_CONTROL_MAP_ANDROID = [
  null,
  Keychain.ACCESS_CONTROL.BIOMETRY_CURRENT_SET,
];
const SECURITY_LEVEL_OPTIONS = ['Any', 'Software', 'Hardware'];
const SECURITY_LEVEL_MAP = [
  Keychain.SECURITY_LEVEL.ANY,
  Keychain.SECURITY_LEVEL.SECURE_SOFTWARE,
  Keychain.SECURITY_LEVEL.SECURE_HARDWARE,
];

const SECURITY_STORAGE_OPTIONS = ['Best', 'FB', 'AES', 'RSA'];
const SECURITY_STORAGE_MAP = [
  null,
  Keychain.STORAGE_TYPE.FB,
  Keychain.STORAGE_TYPE.AES,
  Keychain.STORAGE_TYPE.RSA,
];
const SECURITY_RULES_OPTIONS = ['No upgrade', 'Automatic upgrade'];
const SECURITY_RULES_MAP = [null, Keychain.SECURITY_RULES.AUTOMATIC_UPGRADE];

const TYPE_OPTIONS = ['genericPassword', 'internetCredentials'];

export default function App() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [status, setStatus] = useState('');
  const [type, setType] = useState(TYPE_OPTIONS[0]);
  const [biometryType, setBiometryType] =
    useState<Keychain.BIOMETRY_TYPE | null>(null);
  const [accessControl, setAccessControl] = useState<
    Keychain.ACCESS_CONTROL | undefined
  >(undefined);
  const [securityLevel, setSecurityLevel] = useState<
    Keychain.SECURITY_LEVEL | undefined
  >(undefined);
  const [storage, setStorage] = useState<Keychain.STORAGE_TYPE | undefined>(
    undefined
  );
  const [rules, setRules] = useState<Keychain.SECURITY_RULES | undefined>(
    undefined
  );
  const [selectedStorageIndex, setSelectedStorageIndex] = useState(0);
  const [selectedSecurityIndex, setSelectedSecurityIndex] = useState(0);
  const [selectedAccessControlIndex, setSelectedAccessControlIndex] =
    useState(0);
  const [selectedRulesIndex, setSelectedRulesIndex] = useState(0);
  const [hasGenericPassword, setHasGenericPassword] = useState(false);
  const [hasInternetCredentials, setHasInternetCredentials] = useState(false);

  useEffect(() => {
    Keychain.getSupportedBiometryType().then((result) => {
      setBiometryType(result);
    });
    Keychain.hasGenericPassword().then((result) => {
      setHasGenericPassword(result);
    });
    Keychain.hasInternetCredentials({ server: 'https://example.com' }).then(
      (result) => {
        setHasInternetCredentials(result);
      }
    );
  }, []);

  const save = async () => {
    try {
      const start = new Date();
      if (type === 'internetCredentials') {
        await Keychain.setInternetCredentials(
          'https://example.com',
          username,
          password,
          {
            accessControl,
            securityLevel,
            storage,
            rules,
          }
        );
      } else {
        await Keychain.setGenericPassword(username, password, {
          accessControl,
          securityLevel,
          storage,
          rules,
        });
      }

      const end = new Date();
      setUsername('');
      setPassword('');
      setStatus(
        `Credentials saved! takes: ${end.getTime() - start.getTime()} millis`
      );
    } catch (err) {
      setStatus('Could not save credentials, ' + err);
    }
  };

  const load = async () => {
    try {
      const options = {
        authenticationPrompt: {
          title: 'Authentication needed',
          subtitle: 'Subtitle',
          description: 'Some descriptive text',
          cancel: 'Cancel',
        },
      };
      let credentials;
      if (type === 'internetCredentials') {
        credentials = await Keychain.getInternetCredentials(
          'https://example.com',
          {
            ...options,
            rules: rules,
          }
        );
      } else {
        credentials = await Keychain.getGenericPassword({
          ...options,
          rules: rules,
        });
      }
      if (credentials) {
        setStatus('Credentials loaded! ' + JSON.stringify(credentials));
      } else {
        setStatus('No credentials stored.');
      }
    } catch (err) {
      setStatus('Could not load credentials. ' + err);
    }
  };

  const reset = async () => {
    try {
      await Keychain.resetGenericPassword();
      await Keychain.resetInternetCredentials('https://example.com');
      setStatus('Credentials Reset!');
      setUsername('');
      setPassword('');
    } catch (err) {
      setStatus('Could not reset credentials, ' + err);
    }
  };

  const AC_VALUES =
    Platform.OS === 'ios'
      ? ACCESS_CONTROL_OPTIONS
      : ACCESS_CONTROL_OPTIONS_ANDROID;
  const AC_MAP =
    Platform.OS === 'ios' ? ACCESS_CONTROL_MAP : ACCESS_CONTROL_MAP_ANDROID;

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={styles.container}
    >
      <View style={styles.content}>
        <Text style={styles.title} onPress={() => Keyboard.dismiss()}>
          Keychain Example
        </Text>
        <View style={styles.field}>
          <Text style={styles.label}>Username</Text>
          <TextInput
            style={styles.input}
            testID="usernameInput"
            autoCapitalize="none"
            value={username}
            onChange={(event) => setUsername(event.nativeEvent.text)}
            underlineColorAndroid="transparent"
            blurOnSubmit={false}
            returnKeyType="next"
          />
        </View>
        <View style={styles.field}>
          <Text style={styles.label}>Password</Text>
          <TextInput
            style={styles.input}
            testID="passwordInput"
            secureTextEntry
            autoCapitalize="none"
            value={password}
            onChange={(event) => setPassword(event.nativeEvent.text)}
            underlineColorAndroid="transparent"
          />
        </View>
        <View style={styles.field}>
          <Text style={styles.label}>Type</Text>
          <SegmentedControlTab
            selectedIndex={TYPE_OPTIONS.indexOf(type)}
            values={TYPE_OPTIONS}
            onTabPress={(index) => {
              setType(TYPE_OPTIONS[index]);
            }}
          />
        </View>
        <View style={styles.field}>
          <Text style={styles.label}>Access Control</Text>
          <SegmentedControlTab
            selectedIndex={selectedAccessControlIndex}
            values={biometryType ? [...AC_VALUES, biometryType] : AC_VALUES}
            onTabPress={(index) => {
              setAccessControl(AC_MAP[index] || undefined);
              setSelectedAccessControlIndex(index);
            }}
          />
        </View>
        {Platform.OS === 'android' && (
          <View style={styles.field}>
            <Text style={styles.label}>Security Level</Text>
            <SegmentedControlTab
              selectedIndex={selectedSecurityIndex}
              values={SECURITY_LEVEL_OPTIONS}
              onTabPress={(index) => {
                setSecurityLevel(SECURITY_LEVEL_MAP[index] || undefined);
                setSelectedSecurityIndex(index);
              }}
            />
            <Text style={styles.label}>Storage</Text>
            <SegmentedControlTab
              selectedIndex={selectedStorageIndex}
              values={SECURITY_STORAGE_OPTIONS}
              onTabPress={(index) => {
                setStorage(SECURITY_STORAGE_MAP[index] || undefined);
                setSelectedStorageIndex(index);
              }}
            />
            <Text style={styles.label}>Rules</Text>
            <SegmentedControlTab
              selectedIndex={selectedRulesIndex}
              values={SECURITY_RULES_OPTIONS}
              onTabPress={(index) => {
                setRules(SECURITY_RULES_MAP[index] || undefined);
                setSelectedRulesIndex(index);
              }}
            />
          </View>
        )}
        {!!status && <Text style={styles.status}>{status}</Text>}

        <View style={styles.buttons}>
          <TouchableHighlight onPress={save} style={styles.button}>
            <View style={styles.save}>
              <Text style={styles.buttonText}>Save</Text>
            </View>
          </TouchableHighlight>

          <TouchableHighlight onPress={load} style={styles.button}>
            <View style={styles.load}>
              <Text style={styles.buttonText}>Load</Text>
            </View>
          </TouchableHighlight>

          <TouchableHighlight onPress={reset} style={styles.button}>
            <View style={styles.reset}>
              <Text style={styles.buttonText}>Reset</Text>
            </View>
          </TouchableHighlight>
        </View>

        <Text style={styles.status}>
          hasGenericPassword: {String(hasGenericPassword)}
        </Text>
        <Text style={styles.status}>
          hasInternetCredentials: {String(hasInternetCredentials)}
        </Text>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    backgroundColor: '#F5FCFF',
  },
  content: {
    marginHorizontal: 20,
  },
  title: {
    fontSize: 28,
    fontWeight: '200',
    textAlign: 'center',
    marginBottom: 20,
  },
  field: {
    marginVertical: 5,
  },
  label: {
    fontWeight: '500',
    fontSize: 15,
    marginBottom: 5,
  },
  input: {
    color: '#000',
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#ccc',
    backgroundColor: 'white',
    height: 32,
    fontSize: 14,
    padding: 8,
  },
  status: {
    color: '#333',
    fontSize: 12,
    marginTop: 15,
  },
  biometryType: {
    color: '#333',
    fontSize: 12,
    marginTop: 15,
  },
  buttons: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 20,
  },
  button: {
    borderRadius: 3,
    padding: 2,
    overflow: 'hidden',
  },
  save: {
    backgroundColor: '#0c0',
  },
  load: {
    backgroundColor: '#333',
  },
  reset: {
    backgroundColor: '#c00',
  },
  buttonText: {
    color: 'white',
    fontSize: 14,
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
});
