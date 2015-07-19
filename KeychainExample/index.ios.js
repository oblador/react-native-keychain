/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 */
'use strict';

var React = require('react-native');
var {
  AppRegistry,
  StyleSheet,
  Text,
  TextInput,
  View,
  TouchableHighlight,
} = React;

var Keychain = require('react-native-keychain');

var KeychainExample = React.createClass({
  getInitialState: function() {
    return {
      username: '',
      password: '',
      status: '',
    };
  },

  _save: function() {
    Keychain
      .setGenericPassword(this.state.username, this.state.password)
      .then(() => {
        this.setState({status: 'Credentials saved!'});
      })
      .catch((err) => {
        this.setState({status: 'Could not save credentials, ' + err});
      });
  },

  _load: function() {
    Keychain
      .getGenericPassword()
      .then((credentials) => {
        this.setState(credentials);
        this.setState({status: 'Credentials loaded!'});
      })
      .catch((err) => {
        this.setState({status: 'Could not load credentials. ' + err});
      });
  },

  _reset: function() {
    Keychain
      .resetGenericPassword()
      .then(() => {
        this.setState({status: 'Credentials Reset!', username: '', password: '' });
      })
      .catch((err) => {
        this.setState({status: 'Could not reset credentials, ' + err});
      });
  },

  render: function() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          React Native Keychain Example
        </Text>
        <View style={styles.field}>
          <Text style={styles.label}>Username</Text>
          <TextInput
            style={styles.input}
            autoFocus={true}
            autoCapitalize="none"
            value={this.state.username}
            onChange={(event) => this.setState({ username: event.nativeEvent.text })}
           />
        </View>
        <View style={styles.field}>
          <Text style={styles.label}>Password</Text>
          <TextInput
            style={styles.input}
            password={true}
            autoCapitalize="none"
            value={this.state.password}
            onChange={(event) => this.setState({ password: event.nativeEvent.text })}
           />
        </View>
        <Text style={styles.status}>{this.state.status}</Text>
        <View style={styles.buttons}>
          <TouchableHighlight onPress={this._save} style={[styles.button, styles.save]}>
            <View>
              <Text style={styles.buttonText}>Save</Text>
            </View>
          </TouchableHighlight>
          <TouchableHighlight onPress={this._load} style={styles.button}>
            <View>
              <Text style={styles.buttonText}>Load</Text>
            </View>
          </TouchableHighlight>
          <TouchableHighlight onPress={this._reset} style={[styles.button, styles.reset]}>
            <View>
              <Text style={styles.buttonText}>Reset</Text>
            </View>
          </TouchableHighlight>
        </View>
      </View>
    );
  }
});

var styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
    paddingTop: 30,
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  field: {
    marginVertical: 5,
  },
  label: {
    fontWeight: '500',
  },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    height: 30,
    fontSize: 14,
    padding: 4,
    width: 250,
  },
  status: {
    width: 250,
  },
  buttons: {
    flexDirection: 'row',
    marginTop: 15,
  },
  button: {
    marginHorizontal: 5,
    paddingHorizontal: 8,
    borderRadius: 4,
    backgroundColor: '#333',
    overflow: 'hidden',
  },
  save: {
    backgroundColor: '#0c0',
  },
  reset: {
    backgroundColor: '#c00',
  },
  buttonText: {
    color: 'white',
    padding: 5,
  }
});

AppRegistry.registerComponent('KeychainExample', () => KeychainExample);
