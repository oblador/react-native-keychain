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

var Keychain = require('Keychain');

var KeychainExample = React.createClass({
  getInitialState: function() {
    return {
      server: 'http://localhost/',
      username: '',
      password: '',
      status: '',
    };
  },

  _save: function() {
    Keychain
      .setInternetCredentials(this.state.server, this.state.username, this.state.password)
      .then(function() {
        this.setState({status: 'Credentials saved!'});
      }.bind(this))
      .catch(function(err) {
        this.setState({status: 'Could not save credentials, ' + err});
      }.bind(this));
  },

  _load: function() {
    Keychain
      .getInternetCredentials(this.state.server)
      .then(function(credentials) {
        this.setState(credentials);
        this.setState({status: 'Credentials loaded!'});
      }.bind(this))
      .catch(function(err) {
        this.setState({status: 'Could not load credentials. ' + err});
      }.bind(this));
  },

  _reset: function() {
    Keychain
      .resetInternetCredentials(this.state.server)
      .then(function() {
        this.setState({status: 'Credentials Reset!', username: '', password: '' });
      }.bind(this))
      .catch(function(err) {
        this.setState({status: 'Could not save credentials, ' + err});
      }.bind(this));
  },

  render: function() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          React Native Keychain Example
        </Text>
        <View style={styles.field}>
          <Text style={styles.label}>Server</Text>
          <TextInput 
            style={styles.input} 
            keyboardType="url" 
            autoCapitalize="none" 
            value={this.state.server}
            onChange={(event) => this.setState({ server: event.nativeEvent.text })}
           />
        </View>
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
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
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
