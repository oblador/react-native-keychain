import React, { Component } from 'react';
import {
  Alert,
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

export default class KeychainExample extends Component {
  state = {
    username: '',
    password: '',
    status: '',
    biometryType: undefined,
    accessControl: undefined as undefined | Keychain.ACCESS_CONTROL,
    securityLevel: undefined as undefined | Keychain.SECURITY_LEVEL,
    storage: undefined as undefined | Keychain.STORAGE_TYPE,
    rules: undefined as undefined | Keychain.SECURITY_RULES,
    selectedStorageIndex: 0,
    selectedSecurityIndex: 0,
    selectedAccessControlIndex: 0,
    selectedRulesIndex: 0,
    hasGenericPassword: false,
  };

  componentDidMount() {
    Keychain.getSupportedBiometryType().then((biometryType) => {
      this.setState({ biometryType });
    });
    Keychain.hasGenericPassword().then((hasGenericPassword) => {
      this.setState({ hasGenericPassword });
    });
  }

  async save() {
    try {
      const start = new Date();
      await Keychain.setGenericPassword(
        this.state.username,
        this.state.password,
        {
          accessControl: this.state.accessControl,
          securityLevel: this.state.securityLevel,
          storage: this.state.storage,
          rules: this.state.rules,
        }
      );

      const end = new Date();

      this.setState({
        username: '',
        password: '',
        status: `Credentials saved! takes: ${
          end.getTime() - start.getTime()
        } millis`,
      });
    } catch (err) {
      this.setState({ status: 'Could not save credentials, ' + err });
    }
  }

  async load() {
    try {
      const options = {
        authenticationPrompt: {
          title: 'Authentication needed',
          subtitle: 'Subtitle',
          description: 'Some descriptive text',
          cancel: 'Cancel',
        },
      };
      const credentials = await Keychain.getGenericPassword({
        ...options,
        rules: this.state.rules,
      });
      if (credentials) {
        this.setState({
          status: 'Credentials loaded! ' + JSON.stringify(credentials),
        });
      } else {
        this.setState({ status: 'No credentials stored.' });
      }
    } catch (err) {
      this.setState({ status: 'Could not load credentials. ' + err });
    }
  }

  async reset() {
    try {
      await Keychain.resetGenericPassword();
      this.setState({
        status: 'Credentials Reset!',
        username: '',
        password: '',
      });
    } catch (err) {
      this.setState({ status: 'Could not reset credentials, ' + err });
    }
  }

  async getAll() {
    try {
      const result = await Keychain.getAllGenericPasswordServices();
      this.setState({
        status: `All keys successfully fetched! Found: ${result.length} keys.`,
      });
    } catch (err) {
      this.setState({ status: 'Could not get all keys. ' + err });
    }
  }

  async ios_specifics() {
    try {
      const reply = await Keychain.setSharedWebCredentials(
        'server',
        'username',
        'password'
      );
      console.log(`setSharedWebCredentials: ${JSON.stringify(reply)}`);
    } catch (err) {
      Alert.alert('setSharedWebCredentials error', (err as Error).message);
    }

    try {
      const reply = await Keychain.requestSharedWebCredentials();
      console.log(`requestSharedWebCredentials: ${JSON.stringify(reply)}`);
    } catch (err) {
      Alert.alert('requestSharedWebCredentials error', (err as Error).message);
    }
  }

  render() {
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
              value={this.state.username}
              onChange={(event) =>
                this.setState({ username: event.nativeEvent.text })
              }
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
              value={this.state.password}
              onChange={(event) =>
                this.setState({ password: event.nativeEvent.text })
              }
              underlineColorAndroid="transparent"
            />
          </View>
          <View style={styles.field}>
            <Text style={styles.label}>Access Control</Text>
            <SegmentedControlTab
              selectedIndex={this.state.selectedAccessControlIndex}
              values={
                this.state.biometryType
                  ? [...AC_VALUES, this.state.biometryType]
                  : AC_VALUES
              }
              onTabPress={(index) =>
                this.setState({
                  ...this.state,
                  accessControl: AC_MAP[index],
                  selectedAccessControlIndex: index,
                })
              }
            />
          </View>
          {Platform.OS === 'android' && (
            <View style={styles.field}>
              <Text style={styles.label}>Security Level</Text>
              <SegmentedControlTab
                selectedIndex={this.state.selectedSecurityIndex}
                values={SECURITY_LEVEL_OPTIONS}
                onTabPress={(index) =>
                  this.setState({
                    ...this.state,
                    securityLevel: SECURITY_LEVEL_MAP[index],
                    selectedSecurityIndex: index,
                  })
                }
              />
              <Text style={styles.label}>Storage</Text>
              <SegmentedControlTab
                selectedIndex={this.state.selectedStorageIndex}
                values={SECURITY_STORAGE_OPTIONS}
                onTabPress={(index) =>
                  this.setState({
                    ...this.state,
                    storage: SECURITY_STORAGE_MAP[index],
                    selectedStorageIndex: index,
                  })
                }
              />
              <Text style={styles.label}>Rules</Text>
              <SegmentedControlTab
                selectedIndex={this.state.selectedRulesIndex}
                values={SECURITY_RULES_OPTIONS}
                onTabPress={(index) =>
                  this.setState({
                    ...this.state,
                    rules: SECURITY_RULES_MAP[index],
                    selectedRulesIndex: index,
                  })
                }
              />
            </View>
          )}
          {!!this.state.status && (
            <Text style={styles.status}>{this.state.status}</Text>
          )}

          <View style={styles.buttons}>
            <TouchableHighlight
              onPress={() => this.save()}
              style={styles.button}
            >
              <View style={styles.save}>
                <Text style={styles.buttonText}>Save</Text>
              </View>
            </TouchableHighlight>

            <TouchableHighlight
              onPress={() => this.load()}
              style={styles.button}
            >
              <View style={styles.load}>
                <Text style={styles.buttonText}>Load</Text>
              </View>
            </TouchableHighlight>

            <TouchableHighlight
              onPress={() => this.reset()}
              style={styles.button}
            >
              <View style={styles.reset}>
                <Text style={styles.buttonText}>Reset</Text>
              </View>
            </TouchableHighlight>
          </View>

          <View style={styles.buttons}>
            <TouchableHighlight
              onPress={() => this.getAll()}
              style={styles.button}
            >
              <View style={styles.load}>
                <Text style={styles.buttonText}>Get Used Keys</Text>
              </View>
            </TouchableHighlight>
            {Platform.OS === 'android' && (
              <TouchableHighlight
                onPress={async () => {
                  const level = await Keychain.getSecurityLevel();
                  if (level !== null) {
                    Alert.alert('Security Level', JSON.stringify(level));
                  }
                }}
                style={styles.button}
              >
                <View style={styles.load}>
                  <Text style={styles.buttonText}>Get security level</Text>
                </View>
              </TouchableHighlight>
            )}
            {Platform.OS === 'ios' && (
              <TouchableHighlight
                onPress={() => this.ios_specifics()}
                style={styles.button}
              >
                <View style={styles.load}>
                  <Text style={styles.buttonText}>Test Other APIs</Text>
                </View>
              </TouchableHighlight>
            )}
          </View>
          <Text style={styles.status}>
            hasGenericPassword: {String(this.state.hasGenericPassword)}
          </Text>
        </View>
      </KeyboardAvoidingView>
    );
  }
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
