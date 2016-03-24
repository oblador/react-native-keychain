/**
 * @providesModule Keychain
 */
'use strict';

var { NativeModules } = require('react-native');
var RNKeychainManager = NativeModules.RNKeychainManager;

var Keychain = {
  /**
   * Saves the `username` and `password` combination for `server`
   * and calls `callback` with an `Error` if there is any.
   * Returns a `Promise` object.
   */
  setInternetCredentials: function(
    server: string,
    username: string,
    password: string,
    callback?: ?(error: ?Error) => void
  ): Promise {
    return new Promise((resolve, reject) => {
      RNKeychainManager.setInternetCredentialsForServer(server, username, password, function(err) {
        callback && callback((err && convertError(err)) || null);
        if (err) {
          reject(convertError(err));
        } else {
          resolve();
        }
      });
    });
  },

  /**
   * Fetches login combination for `server` as an object with the format `{ username, password }`
   * and passes the result to `callback`, along with an `Error` if there is any.
   * Returns a `Promise` object.
   */
  getInternetCredentials: function(
    server: string,
    callback?: ?(error: ?Error, result: ?string) => void
  ): Promise {
    return new Promise((resolve, reject) => {
      RNKeychainManager.getInternetCredentialsForServer(server, function(err, username, password) {
        err = convertError(err);
        if(!err && arguments.length === 1) {
          err = new Error('No keychain entry found for server "' + server + '"');
        }
        callback && callback((err && convertError(err)) || null, username, password);
        if (err) {
          reject(convertError(err));
        } else {
          resolve({ username, password });
        }
      });
    });
  },

  /**
   * Deletes all internet password keychain entries for `server` and calls `callback` with an
   * `Error` if there is any.
   * Returns a `Promise` object.
   */
  resetInternetCredentials: function(
    server: string,
    callback?: ?(error: ?Error) => void
  ): Promise {
    return new Promise((resolve, reject) => {
      RNKeychainManager.resetInternetCredentialsForServer(server, function(err) {
        callback && callback((err && convertError(err)) || null);
        if (err) {
          reject(convertError(err));
        } else {
          resolve();
        }
      });
    });
  },

  /**
   * Saves the `username` and `password` combination for `service` (defaults to `bundleId`)
   * and calls `callback` with an `Error` if there is any.
   * Returns a `Promise` object.
   */
  setGenericPassword: function(
    username: string,
    password: string,
    service?: string,
    callback?: ?(error: ?Error) => void
  ): Promise {
    return new Promise((resolve, reject) => {
      RNKeychainManager.setGenericPasswordForService(service, username, password, function(err) {
        callback && callback((err && convertError(err)) || null);
        if (err) {
          reject(convertError(err));
        } else {
          resolve();
        }
      });
    });
  },

  /**
   * Fetches login combination for `service` (defaults to `bundleId`) as an object with the format
   * `{ username, password }` and passes the result to `callback`, along with an `Error` if
   * there is any.
   * Returns a `Promise` object.
   */
  getGenericPassword: function(
    service?: string,
    callback?: ?(error: ?Error, result: ?string) => void
  ): Promise {
    return new Promise((resolve, reject) => {
      RNKeychainManager.getGenericPasswordForService(service, function(err, username, password) {
        err = convertError(err);
        if(!err && arguments.length === 1) {
          err = new Error('No keychain entry found' + (service ? ' for service "' + service + '"' : ''));
        }
        callback && callback((err && convertError(err)) || null, username, password);
        if (err) {
          reject(convertError(err));
        } else {
          resolve({ username, password });
        }
      });
    });
  },

  /**
   * Deletes all generic password keychain entries for `service` (defaults to `bundleId`) and calls
   * `callback` with an `Error` if there is any.
   * Returns a `Promise` object.
   */
  resetGenericPassword: function(
    service?: string,
    callback?: ?(error: ?Error) => void
  ): Promise {
    return new Promise((resolve, reject) => {
      RNKeychainManager.resetGenericPasswordForService(service, function(err) {
        callback && callback((err && convertError(err)) || null);
        if (err) {
          reject(convertError(err));
        } else {
          resolve();
        }
      });
    });
  },

  /**
   * Saves the `key` and `value` combination for the current `bundleId`
   * and calls `callback` with an `Error` if there is any.
   * Returns a `Promise` object.
   */
  setSecureString: function(
    key: string,
    value: string,
    callback?: ?(error: ?Error) => void
  ): Promise {
    return new Promise((resolve, reject) => {
      RNKeychainManager.setSecureString(key, value, function(err) {
        callback && callback((err && convertError(err)) || null);
        if (err) {
          reject(convertError(err));
        } else {
          resolve();
        }
      });
    });
  },

  /**
   * Fetches value for the provided key in the current `bundleId` as an object with the format
   * `{ key, value }` and passes the result to `callback`, along with an `Error` if
   * there is any.
   * Returns a `Promise` object.
   */
  getSecureString: function(
    key: string,
    callback?: ?(error: ?Error, result: ?string) => void
  ): Promise {
    return new Promise((resolve, reject) => {
      RNKeychainManager.getSecureStringForKey(key, function(err, key, value) {
        err = convertError(err);
        if(!err && arguments.length === 1) {
          err = new Error('No keychain entry found' + (key ? ' for key "' + key + '"' : ''));
        }
        callback && callback((err && convertError(err)) || null, key, value);
        if (err) {
          reject(convertError(err));
        } else {
          resolve({ key, value });
        }
      });
    });
  },

  /**
   * Deletes the entry for the specified key in the current `bundleId` and calls
   * `callback` with an `Error` if there is any.
   * Returns a `Promise` object.
   */
  resetSecureString: function(
    key: string,
    callback?: ?(error: ?Error) => void
  ): Promise {
    return new Promise((resolve, reject) => {
      RNKeychainManager.resetSecureStringForKey(key, function(err) {
        callback && callback((err && convertError(err)) || null);
        if (err) {
          reject(convertError(err));
        } else {
          resolve();
        }
      });
    });
  },

};

function convertError(err) {
  if (!err) {
    return null;
  }
  var out = new Error(err.message);
  out.key = err.key;
  return out;
}

module.exports = Keychain;
