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
    ): Promise<void>;

    function getGenericPassword(
        service?: string
    ): Promise<void>;

    function requestSharedWebCredentials (
    ): Promise<SharedWebCredentials>;

    function setSharedWebCredentials(
        server: string,
        username: string,
        password: string
    ): Promise<void>;

}
