/* eslint-disable @typescript-eslint/ban-types */
// RNKeychainManager.ts
import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

import type { Result, UserCredentials, SharedWebCredentials } from './types';
import type { BIOMETRY_TYPE, SECURITY_LEVEL } from './enums';

export interface Spec extends TurboModule {
  setGenericPasswordForOptions(
    options: Object | null | undefined,
    username: string,
    password: string
  ): Promise<false | Result>;
  getGenericPasswordForOptions(
    options?: Object | null
  ): Promise<false | UserCredentials>;
  hasGenericPasswordForOptions(options?: Object | null): Promise<boolean>;
  resetGenericPasswordForOptions(options?: Object | null): Promise<boolean>;
  getAllGenericPasswordServices(options?: Object | null): Promise<string[]>;
  hasInternetCredentialsForOptions(options: string | Object): Promise<boolean>;
  setInternetCredentialsForServer(
    server: string,
    username: string,
    password: string,
    options?: Object | null
  ): Promise<false | Result>;
  getInternetCredentialsForServer(
    server: string,
    options?: Object | null
  ): Promise<false | UserCredentials>;
  resetInternetCredentialsForOptions(options: Object): Promise<void>;
  getSupportedBiometryType?(): Promise<null | BIOMETRY_TYPE>;
  canCheckAuthentication?(options?: Object | null): Promise<boolean>;
  getSecurityLevel?(options?: Object | null): Promise<null | SECURITY_LEVEL>;
  isPasscodeAuthAvailable?(): Promise<boolean>;
  requestSharedWebCredentials?(): Promise<false | SharedWebCredentials>;
  setSharedWebCredentialsForServer?(
    server: string,
    username: string,
    password?: string
  ): Promise<void>;
  addListener?(eventName: string): void;
  removeListeners?(count: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'RNKeychainManager'
) as Spec;
