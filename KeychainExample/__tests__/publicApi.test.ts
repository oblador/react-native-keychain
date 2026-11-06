import { Platform } from 'react-native';
import Keychain, * as API from 'react-native-keychain';
import RNKeychainManager from '../../src/NativeKeychainManager';

jest.mock('react-native', () => ({ Platform: { OS: 'ios' } }));

jest.mock('../../src/NativeKeychainManager', () => ({
  __esModule: true,
  default: {
    setGenericPasswordForOptions: jest.fn(),
    getGenericPasswordForOptions: jest.fn(),
    setInternetCredentialsForServer: jest.fn(),
    getInternetCredentialsForServer: jest.fn(),
    hasGenericPasswordForOptions: jest.fn(),
    hasInternetCredentialsForOptions: jest.fn(),
    getAllInternetCredentialServers: jest.fn(),
    resetInternetCredentialsForOptions: jest.fn(),
    requestSharedWebCredentials: jest.fn(),
    setSharedWebCredentialsForServer: jest.fn(),
  },
}));

const native = jest.mocked(RNKeychainManager);
const server = 'https://example.com';
const sharedWebMethods = {
  requestSharedWebCredentials: native.requestSharedWebCredentials,
  setSharedWebCredentialsForServer: native.setSharedWebCredentialsForServer,
};

afterEach(() => {
  jest.restoreAllMocks();
  jest.resetAllMocks();
  Object.assign(native, sharedWebMethods);
});

describe('authentication prompt defaults', () => {
  it.each([
    ['omitted', undefined, 'Authenticate to retrieve secret', 'Cancel'],
    ['empty object', {}, 'Authenticate to retrieve secret', 'Cancel'],
    [
      'undefined fields',
      { title: undefined, cancel: undefined },
      'Authenticate to retrieve secret',
      'Cancel',
    ],
    ['custom fields', { title: 'Unlock', cancel: 'Back' }, 'Unlock', 'Back'],
    ['empty strings', { title: '', cancel: '' }, '', ''],
    ['partial override', { title: 'Unlock' }, 'Unlock', 'Cancel'],
  ] as const)(
    'preserves %s across credential set/get calls',
    async (_, prompt, title, cancel) => {
      const options = Object.freeze({
        service: 'test-service',
        cloudSync: false,
        authenticationPrompt:
          prompt === undefined ? undefined : Object.freeze({ ...prompt }),
      });
      const expected = {
        ...options,
        authenticationPrompt: { title, cancel },
      };

      await Keychain.setGenericPassword('username', 'password', options);
      await Keychain.getGenericPassword(options);
      await Keychain.setInternetCredentials(
        server,
        'username',
        'password',
        options
      );
      await Keychain.getInternetCredentials(server, options);

      expect(native.setGenericPasswordForOptions).toHaveBeenCalledWith(
        expected,
        'username',
        'password'
      );
      expect(native.getGenericPasswordForOptions).toHaveBeenCalledWith(
        expected
      );
      expect(native.setInternetCredentialsForServer).toHaveBeenCalledWith(
        server,
        'username',
        'password',
        expected
      );
      expect(native.getInternetCredentialsForServer).toHaveBeenCalledWith(
        server,
        expected
      );
      expect(options.authenticationPrompt).toEqual(prompt);
    }
  );

  it('accepts omitted options and preserves extra prompt fields', async () => {
    await Keychain.getGenericPassword();
    expect(native.getGenericPasswordForOptions).toHaveBeenLastCalledWith({
      authenticationPrompt: {
        title: 'Authenticate to retrieve secret',
        cancel: 'Cancel',
      },
    });
    await Keychain.getGenericPassword({
      authenticationPrompt: {
        subtitle: 'Account',
        description: 'Unlock saved credentials',
      },
    });
    expect(native.getGenericPasswordForOptions).toHaveBeenLastCalledWith({
      authenticationPrompt: {
        title: 'Authenticate to retrieve secret',
        cancel: 'Cancel',
        subtitle: 'Account',
        description: 'Unlock saved credentials',
      },
    });
  });
});

it('exposes existence checks and server listing through both import styles', async () => {
  const options = {
    server,
    service: 'test-service',
    cloudSync: false,
    accessGroup: 'test-group',
  };
  native.hasGenericPasswordForOptions.mockResolvedValue(true);
  native.hasInternetCredentialsForOptions.mockResolvedValue(false);
  native.getAllInternetCredentialServers.mockResolvedValue([server]);

  expect(Keychain.hasGenericPassword).toBe(API.hasGenericPassword);
  expect(Keychain.hasInternetCredentials).toBe(API.hasInternetCredentials);
  expect(Keychain.getAllInternetCredentialServers).toBe(
    API.getAllInternetCredentialServers
  );
  await expect(Keychain.hasGenericPassword(options)).resolves.toBe(true);
  await expect(Keychain.hasInternetCredentials(options)).resolves.toBe(false);
  await expect(
    Keychain.getAllInternetCredentialServers(options)
  ).resolves.toEqual([server]);
  expect(native.hasGenericPasswordForOptions).toHaveBeenCalledWith(options);
  expect(native.hasInternetCredentialsForOptions).toHaveBeenCalledWith(options);
  expect(native.getAllInternetCredentialServers).toHaveBeenCalledWith(options);
});

describe('shared web credentials', () => {
  it.each(['ios', 'android'] as const)(
    'rejects absent native methods asynchronously on %s',
    async (os) => {
      jest.replaceProperty(Platform, 'OS', os);
      native.requestSharedWebCredentials = undefined;
      native.setSharedWebCredentialsForServer = undefined;

      const request = Keychain.requestSharedWebCredentials();
      const set = Keychain.setSharedWebCredentials(server, 'username');
      expect(request).toBeInstanceOf(Promise);
      expect(set).toBeInstanceOf(Promise);
      await expect(request).rejects.toThrow('not supported');
      await expect(set).rejects.toThrow('not supported');
    }
  );

  it('forwards supported iOS results and the optional password unchanged', async () => {
    jest.replaceProperty(Platform, 'OS', 'ios');
    const credentials = {
      server,
      username: 'username',
      password: 'password',
      storage: 'keychain',
    };
    native.requestSharedWebCredentials!.mockResolvedValue(credentials);
    native.setSharedWebCredentialsForServer!.mockResolvedValue(true);

    await expect(Keychain.requestSharedWebCredentials()).resolves.toBe(
      credentials
    );
    await expect(
      Keychain.setSharedWebCredentials(server, 'username')
    ).resolves.toBe(true);
    expect(native.setSharedWebCredentialsForServer).toHaveBeenLastCalledWith(
      server,
      'username',
      undefined
    );
    await Keychain.setSharedWebCredentials(server, 'username', 'password');
    expect(native.setSharedWebCredentialsForServer).toHaveBeenLastCalledWith(
      server,
      'username',
      'password'
    );
  });
});

it('preserves native payloads, missing results, and rejected operations', async () => {
  const credentials = {
    server,
    username: 'username',
    password: 'password',
    storage: 'keychain',
  };
  native.getInternetCredentialsForServer.mockResolvedValue(credentials);
  native.getGenericPasswordForOptions.mockResolvedValue(false);
  native.resetInternetCredentialsForOptions.mockResolvedValue(true);
  await expect(Keychain.getInternetCredentials(server)).resolves.toBe(
    credentials
  );
  await expect(Keychain.getGenericPassword()).resolves.toBe(false);
  await expect(Keychain.resetInternetCredentials({ server })).resolves.toBe(
    true
  );
  expect(native.resetInternetCredentialsForOptions).toHaveBeenCalledWith({
    server,
  });

  const error = new Error('Authentication canceled');
  native.getInternetCredentialsForServer.mockRejectedValue(error);
  await expect(Keychain.getInternetCredentials(server)).rejects.toBe(error);
});
