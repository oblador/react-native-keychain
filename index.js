import { NativeModules, Platform } from 'react-native';
const { RNKeychainManager } = NativeModules;

function convertError(err) {
  if (!err) {
    return null;
  }
  if (Platform.OS === 'android') {
    return new Error(err);
  }
  var out = new Error(err.message);
  out.key = err.key;
  return out;
}

/**
 * Saves the `username` and `password` combination for `server`
 * and calls `callback` with an `Error` if there is any.
 * Returns a `Promise` object.
 */
export function setInternetCredentials(
  server: string,
  username: string,
  password: string,
  callback?: ?(error: ?Error) => void
): Promise {
  return new Promise((resolve, reject) => {
    RNKeychainManager.setInternetCredentialsForServer(server, username, password, function(err) {
      err = convertError(err);
      callback && callback(err || null);
      if (err) {
        reject(err);
      } else {
        resolve();
      }
    });
  });
}

/**
 * Fetches login combination for `server` as an object with the format `{ username, password }`
 * and passes the result to `callback`, along with an `Error` if there is any.
 * Returns a `Promise` object.
 */
export function getInternetCredentials(
  server: string,
  callback?: ?(error: ?Error, username: ?string, password: ?string) => void
): Promise {
  return new Promise((resolve, reject) => {
    RNKeychainManager.getInternetCredentialsForServer(server, function(err, username, password) {
      err = convertError(err);
      if(!err && arguments.length === 1) {
        err = new Error('No keychain entry found for server "' + server + '"');
      }
      callback && callback(err || null, username, password);
      if (err) {
        reject(err);
      } else {
        resolve({ username, password });
      }
    });
  });
}

/**
 * Deletes all internet password keychain entries for `server` and calls `callback` with an
 * `Error` if there is any.
 * Returns a `Promise` object.
 */
export function resetInternetCredentials(
  server: string,
  callback?: ?(error: ?Error) => void
): Promise {
  return new Promise((resolve, reject) => {
    RNKeychainManager.resetInternetCredentialsForServer(server, function(err) {
      err = convertError(err);
      callback && callback(err || null);
      if (err) {
        reject(err);
      } else {
        resolve();
      }
    });
  });
}

/**
 * Saves the `username` and `password` combination for `service` (defaults to `bundleId`)
 * and calls `callback` with an `Error` if there is any.
 * Returns a `Promise` object.
 */
export function setGenericPassword(
  username: string,
  password: string,
  service?: string,
  callback?: ?(error: ?Error) => void
): Promise {
  return new Promise((resolve, reject) => {
    RNKeychainManager.setGenericPasswordForService(service, username, password, function(err) {
      err = convertError(err);
      callback && callback(err || null);
      if (err) {
        reject(err);
      } else {
        resolve();
      }
    });
  });
}

/**
 * Fetches login combination for `service` (defaults to `bundleId`) as an object with the format
 * `{ username, password }` and passes the result to `callback`, along with an `Error` if
 * there is any.
 * Returns a `Promise` object.
 */
export function getGenericPassword(
  service?: string,
  callback?: ?(error: ?Error, username: ?string, password: ?string) => void
): Promise {
  return new Promise((resolve, reject) => {
    RNKeychainManager.getGenericPasswordForService(service, function(err, username, password) {
      err = convertError(err);
      if(!err && arguments.length === 1) {
        err = new Error('No keychain entry found' + (service ? ' for service "' + service + '"' : ''));
      }
      callback && callback(err || null, username, password);
      if (err) {
        reject(err);
      } else {
        resolve({ username, password });
      }
    });
  });
}

/**
 * Deletes all generic password keychain entries for `service` (defaults to `bundleId`) and calls
 * `callback` with an `Error` if there is any.
 * Returns a `Promise` object.
 */
export function resetGenericPassword(
  service?: string,
  callback?: ?(error: ?Error) => void
): Promise {
  return new Promise((resolve, reject) => {
    RNKeychainManager.resetGenericPasswordForService(service, function(err) {
      err = convertError(err);
      callback && callback(err || null);
      if (err) {
        reject(err);
      } else {
        resolve();
      }
    });
  });
}

/**
 * Asks the user for a shared web credential, resolves to `{ server, username, password }` if approved
 * `false` if denied and throws an error if not supported on platform or there's no shared credentials.
 * Returns a `Promise` object.
 */
export function requestSharedWebCredentials() : Promise {
  if (Platform.OS !== 'ios') {
    return Promise.reject(new Error(`requestSharedWebCredentials() is not supported on ${Platform.OS} yet`));
  }
  return RNKeychainManager.requestSharedWebCredentials();
}

/**
 * Sets a shared web credential.
 * Returns a `Promise` object.
 */
export function setSharedWebCredentials(
  server: string,
  username: string,
  password: string
) : Promise {
  if (Platform.OS !== 'ios') {
    return Promise.reject(new Error(`setSharedWebCredentials() is not supported on ${Platform.OS} yet`));
  }
  return RNKeychainManager.setSharedWebCredentialsForServer(server, username, password);
}
