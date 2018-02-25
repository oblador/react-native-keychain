declare module 'react-native-keychain' {

    export interface UserCredentials {
        username: string;
        password: string;
    }

    export interface SharedWebCredentials {
        server: string;
        username: string;
        password: string;
    }

    export interface SecureOptions {
        service?: string;
        authenticationPrompt?: string;
        authenticationType?: string;
        accessControl?: string;
    }

    function canImplyAuthentication(
        options?: SecureOptions
    ): Promise<boolean>;

    function getSupportedBiometryType(
    ): Promise<string>;

    function setSecurePassword(
        username: string,
        password: string,
        options?: SecureOptions
    ): Promise<boolean>;

    function getSecurePassword(
        options?: SecureOptions
    ): Promise<boolean | {service: string, username: string, password: string}>;

    function setInternetCredentials(
        server: string,
        username: string,
        password: string
    ): Promise<void>;

    function getInternetCredentials(
        server: string
    ): Promise<UserCredentials>;

    function resetInternetCredentials(
        server: string
    ): Promise<void>;

    function setGenericPassword(
        username: string,
        password: string,
        service?: string
    ): Promise<boolean>;

    function getGenericPassword(
        service?: string
    ): Promise<boolean | {service: string, username: string, password: string}>;

    function resetGenericPassword(
        service?: string
    ): Promise<boolean>

    function requestSharedWebCredentials (
    ): Promise<SharedWebCredentials>;

    function setSharedWebCredentials(
        server: string,
        username: string,
        password: string
    ): Promise<void>;

}
