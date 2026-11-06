import Keychain, { STORAGE_TYPE } from 'react-native-keychain';
import type {
  InternetCredentials,
  Result,
  SharedWebCredentials,
  UserCredentials,
} from 'react-native-keychain';

export const iosResult: Result = {
  service: 'test-service',
  storage: 'keychain',
};
export const generic: UserCredentials = {
  ...iosResult,
  username: 'username',
  password: 'password',
};
export const iosInternet: InternetCredentials = {
  server: 'https://example.com',
  username: 'username',
  password: 'password',
  storage: 'keychain',
};
export const androidInternet: InternetCredentials = {
  service: 'https://example.com',
  username: 'username',
  password: 'password',
  storage: STORAGE_TYPE.AES_GCM_NO_AUTH,
};
export const shared: SharedWebCredentials = {
  server: 'https://example.com',
  username: 'username',
  password: 'password',
  storage: 'keychain',
};

// @ts-expect-error Internet credentials must identify a server or service.
export const missingScope: InternetCredentials = {
  username: 'username',
  password: 'password',
  storage: 'keychain',
};
// @ts-expect-error Generic credentials still require a service.
export const invalidGeneric: UserCredentials = iosInternet;
export const invalidStorage: import('react-native-keychain').SetOptions = {
  // @ts-expect-error iOS storage identifiers cannot select an Android cipher.
  storage: 'keychain',
};

export function requireInternetService(
  credentials: InternetCredentials
): string {
  // @ts-expect-error iOS results do not guarantee an Android service field.
  return credentials.service;
}

export async function checkPublicSignatures() {
  const server = 'https://example.com';
  const genericExists: boolean = await Keychain.hasGenericPassword();
  const internetExists: boolean = await Keychain.hasInternetCredentials({
    server,
  });
  const servers: string[] = await Keychain.getAllInternetCredentialServers({
    includeLegacy: true,
  });
  const reset: boolean = await Keychain.resetInternetCredentials({ server });
  const setShared: boolean = await Keychain.setSharedWebCredentials(
    server,
    'username'
  );
  const internet: false | InternetCredentials =
    await Keychain.getInternetCredentials(server);
  const web: false | SharedWebCredentials =
    await Keychain.requestSharedWebCredentials();
  return {
    genericExists,
    internetExists,
    servers,
    reset,
    setShared,
    internet,
    web,
  };
}
