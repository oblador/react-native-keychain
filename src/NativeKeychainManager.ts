import type { TurboModule, CodegenTypes } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  setGenericPasswordForOptions(
    options: CodegenTypes.UnsafeObject | null | undefined,
    username: string,
    password: string
  ): Promise<CodegenTypes.UnsafeObject | null>;
  getGenericPasswordForOptions(
    options?: CodegenTypes.UnsafeObject | null
  ): Promise<false | CodegenTypes.UnsafeObject>;
  hasGenericPasswordForOptions(
    options?: CodegenTypes.UnsafeObject | null
  ): Promise<boolean>;
  resetGenericPasswordForOptions(
    options?: CodegenTypes.UnsafeObject | null
  ): Promise<boolean>;
  getAllGenericPasswordServices(
    options?: CodegenTypes.UnsafeObject | null
  ): Promise<string[]>;
  getAllInternetCredentialServers(
    options?: CodegenTypes.UnsafeObject | null
  ): Promise<string[]>;
  hasInternetCredentialsForOptions(
    options: CodegenTypes.UnsafeObject
  ): Promise<boolean>;
  setInternetCredentialsForServer(
    server: string,
    username: string,
    password: string,
    options?: CodegenTypes.UnsafeObject | null
  ): Promise<CodegenTypes.UnsafeObject | null>;
  getInternetCredentialsForServer(
    server: string,
    options?: CodegenTypes.UnsafeObject | null
  ): Promise<CodegenTypes.UnsafeObject | null>;
  resetInternetCredentialsForOptions(
    options: CodegenTypes.UnsafeObject
  ): Promise<boolean>;
  getSupportedBiometryType?(): Promise<string | null>;
  canCheckAuthentication?(
    options?: CodegenTypes.UnsafeObject | null
  ): Promise<boolean>;
  getSecurityLevel?(
    options?: CodegenTypes.UnsafeObject | null
  ): Promise<string | null>;
  isPasscodeAuthAvailable?(): Promise<boolean>;
  requestSharedWebCredentials?(): Promise<CodegenTypes.UnsafeObject | null>;
  setSharedWebCredentialsForServer?(
    server: string,
    username: string,
    password?: string
  ): Promise<boolean>;
}

export default TurboModuleRegistry.getEnforcing<Spec>(
  'RNKeychainManager'
) as Spec;
