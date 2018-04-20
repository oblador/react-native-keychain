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

    export interface Options {
        accessControl?: string;
        accessGroup?: string;
        authenticationPrompt?: string;
        authenticationType?: string;
        service?: string;
    }

    function canImplyAuthentication(
        options?: Options
    ): Promise<boolean>;

    function getSupportedBiometryType(
    ): Promise<string>;

    function setInternetCredentials(
        server: string,
        username: string,
        password: string,
        options?: Options
    ): Promise<void>;

    function getInternetCredentials(
        server: string,
        options?: Options
    ): Promise<UserCredentials>;

    function resetInternetCredentials(
        server: string
    ): Promise<void>;

    function setGenericPassword(
        username: string,
        password: string,
        options?: Options
    ): Promise<boolean>;

    function getGenericPassword(
        options?: Options
    ): Promise<boolean | {service: string, username: string, password: string}>;

    function resetGenericPassword(
        options?: Options
    ): Promise<boolean>

    function requestSharedWebCredentials (
    ): Promise<SharedWebCredentials>;

    function setSharedWebCredentials(
        server: string,
        username: string,
        password: string
    ): Promise<void>;

}
