import React, { Component } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  SegmentedControlIOS,
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

export default class KeychainExample extends Component {
  state = {
    username: '',
    password: '',
    status: '',
    biometryType: null,
    accessControl: null,
    securityLevel: null,
    storage: null,
  };

  componentDidMount() {
    Keychain.getSupportedBiometryType({}).then(biometryType => {
      this.setState({ biometryType });
    });
  }

  async save() {
    try {
      let start = new Date();

      await Keychain.setGenericPassword(
        this.state.username,
        this.state.password,
        {
          accessControl: this.state.accessControl,
          securityLevel: this.state.securityLevel,
          storage: this.state.storageSelection,
        }
      );

      let end = new Date();

      this.setState({
        username: '',
        password: '',
        status: `Credentials saved! takes: ${end.getTime() -
          start.getTime()} millis`,
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
          negativeBtnText: 'Cancel',
        },
      };
      const credentials = await Keychain.getGenericPassword(options);
      if (credentials) {
        this.setState({ ...credentials, status: 'Credentials loaded!' });
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

  async ios_specifics() {
    try {
      const reply = await Keychain.setSharedWebCredentials(
        'server',
        'username',
        'password'
      );
      console.log(`setSharedWebCredentials: ${JSON.stringify(reply)}`);
    } catch (err) {
      alert(`setSharedWebCredentials: ${err}`);
    }

    try {
      const reply = await Keychain.requestSharedWebCredentials();
      console.log(`requestSharedWebCredentials: ${JSON.stringify(reply)}`);
    } catch (err) {
      alert(`requestSharedWebCredentials: ${err}`);
    }
  }

  render() {
    const VALUES =
      Platform.OS === 'ios'
        ? ACCESS_CONTROL_OPTIONS
        : ACCESS_CONTROL_OPTIONS_ANDROID;
    const AC_MAP =
      Platform.OS === 'ios' ? ACCESS_CONTROL_MAP : ACCESS_CONTROL_MAP_ANDROID;
    const SL_MAP = Platform.OS === 'ios' ? [] : SECURITY_LEVEL_MAP;
    const ST_MAP = Platform.OS === 'ios' ? [] : SECURITY_STORAGE_MAP;

    return (
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.container}
      >
        <View style={styles.content}>
          <Text style={styles.title}>Keychain Example</Text>
          <View style={styles.field}>
            <Text style={styles.label}>Username</Text>
            <TextInput
              style={styles.input}
              autoFocus={true}
              autoCapitalize="none"
              value={this.state.username}
              onSubmitEditing={() => {
                this.passwordTextInput.focus();
              }}
              onChange={event =>
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
              password={true}
              autoCapitalize="none"
              value={this.state.password}
              ref={input => {
                this.passwordTextInput = input;
              }}
              onChange={event =>
                this.setState({ password: event.nativeEvent.text })
              }
              underlineColorAndroid="transparent"
            />
          </View>
          <View style={styles.field}>
            <Text style={styles.label}>Access Control</Text>
            <SegmentedControlTab
              selectedIndex={this.state.selectedIndex}
              values={
                this.state.biometryType
                  ? [...VALUES, this.state.biometryType]
                  : VALUES
              }
              onTabPress={index =>
                this.setState({
                  ...this.state,
                  accessControl: AC_MAP[index],
                  selectedIndex: index,
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
                onTabPress={index =>
                  this.setState({
                    ...this.state,
                    securityLevel: SL_MAP[index],
                    selectedSecurityIndex: index,
                  })
                }
              />

              <Text style={styles.label}>Storage</Text>
              <SegmentedControlTab
                selectedIndex={this.state.selectedStorageIndex}
                values={SECURITY_STORAGE_OPTIONS}
                onTabPress={index =>
                  this.setState({
                    ...this.state,
                    storageSelection: ST_MAP[index],
                    selectedStorageIndex: index,
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

          {Platform.OS === 'android' && (
            <View style={styles.buttons}>
              <TouchableHighlight
                onPress={async () => {
                  const level = await Keychain.getSecurityLevel();
                  alert(level);
                }}
                style={styles.button}
              >
                <View style={styles.load}>
                  <Text style={styles.buttonText}>Get security level</Text>
                </View>
              </TouchableHighlight>
            </View>
          )}

          {Platform.OS === 'ios' && (
            <View style={styles.buttons}>
              <TouchableHighlight
                onPress={() => this.ios_specifics()}
                style={styles.button}
              >
                <View style={styles.load}>
                  <Text style={styles.buttonText}>Test Other APIs</Text>
                </View>
              </TouchableHighlight>
            </View>
          )}
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
