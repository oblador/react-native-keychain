/* eslint-disable @typescript-eslint/ban-types */
// RNKeychainManager.ts
import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  setGenericPasswordForOptions(
    options: Object | null | undefined,
    username: string,
    password: string
  ): Promise<Object | null>;
  getGenericPasswordForOptions(
    options?: Object | null
  ): Promise<false | Object>;
  hasGenericPasswordForOptions(options?: Object | null): Promise<boolean>;
  resetGenericPasswordForOptions(options?: Object | null): Promise<boolean>;
  getAllGenericPasswordServices(options?: Object | null): Promise<string[]>;
  hasInternetCredentialsForOptions(options: Object): Promise<boolean>;
  setInternetCredentialsForServer(
    server: string,
    username: string,
    password: string,
    options?: Object | null
  ): Promise<Object | null>;
  getInternetCredentialsForServer(
    server: string,
    options?: Object | null
  ): Promise<Object | null>;
  resetInternetCredentialsForOptions(options: Object): Promise<void>;
  getSupportedBiometryType?(): Promise<string | null>;
  canCheckAuthentication?(options?: Object | null): Promise<boolean>;
  getSecurityLevel?(options?: Object | null): Promise<string | null>;
  isPasscodeAuthAvailable?(): Promise<boolean>;
  requestSharedWebCredentials?(): Promise<Object | null>;
  setSharedWebCredentialsForServer?(
    server: string,
    username: string,
    password?: string
  ): Promise<void>;
  isKnoxAvailable?(): Promise<boolean>;
  generateKnoxKey?(alias: string): Promise<boolean>;
  signWithKnoxKey?(alias: string, data: string): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'RNKeychainManager'
) as Spec;
