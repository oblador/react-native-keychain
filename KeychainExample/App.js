import React, { Component } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  StyleSheet,
  Text,
  TextInput,
  TouchableHighlight,
  View,
} from 'react-native';

import * as Keychain from 'react-native-keychain';

export default class KeychainExample extends Component {
  state = {
    username: '',
    password: '',
    status: '',
    biometryType: null,
  };

  componentDidMount() {
    Keychain.getSupportedBiometryType().then(biometryType => {
      this.setState({ biometryType });
    });
  }

  async save() {
    try {
      if (this.state.biometryType) {
        await Keychain.setPasswordWithAuthentication(
          this.state.username,
          this.state.password,
          {
            accessControl:
              Keychain.ACCESS_CONTROL.TOUCH_ID_ANY_OR_DEVICE_PASSCODE,
            authenticationType: Keychain.AUTHENTICATION_TYPE.BIOMETRICS,
          }
        );
      } else {
        await Keychain.setGenericPassword(
          this.state.username,
          this.state.password
        );
      }
      this.setState({ status: 'Credentials saved!' });
    } catch (err) {
      this.setState({ status: 'Could not save credentials, ' + err });
    }
  }

  async load() {
    try {
      const credentials = await (this.state.biometryType
        ? Keychain.getPasswordWithAuthentication({
            accessControl:
              Keychain.ACCESS_CONTROL.TOUCH_ID_ANY_OR_DEVICE_PASSCODE,
            authenticationType: Keychain.AUTHENTICATION_TYPE.BIOMETRICS,
          })
        : Keychain.getGenericPassword());
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

  render() {
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
              onChange={event =>
                this.setState({ username: event.nativeEvent.text })}
              underlineColorAndroid="transparent"
            />
          </View>
          <View style={styles.field}>
            <Text style={styles.label}>Password</Text>
            <TextInput
              style={styles.input}
              password={true}
              autoCapitalize="none"
              value={this.state.password}
              onChange={event =>
                this.setState({ password: event.nativeEvent.text })}
              underlineColorAndroid="transparent"
            />
          </View>
          {!!this.state.status && (
            <Text style={styles.status}>{this.state.status}</Text>
          )}
          {!!this.state.biometryType && (
            <Text style={styles.biometryType}>
              Supported biometry: {this.state.biometryType}
            </Text>
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
        </View>
      </KeyboardAvoidingView>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#F5FCFF',
  },
  content: {
    width: 250,
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
