declare module 'react-native-keychain' {

    export interface UserCredentials {
        username: string;
        password: string;
    }

    function setInternetCredentials(
        server: string,
        username: string,
        password: string,
        callback?: (error?: Error) => void
    ): Promise<void>;

    function getInternetCredentials(
        server: string,
        callback?: (error?: Error, username?: string, password?: string) => void
    ): Promise<UserCredentials>;

    function resetInternetCredentials(
        server: string,
        callback?: (error?: Error) => void
    ): Promise<void>;

    function setGenericPassword(
        username: string,
        password: string,
        service?: string,
        callback?: (error?: Error) => void
    ): Promise<void>;


  function getGenericPassword(
    service?: string,
    callback?: (error?: Error, result?: string) => void
  ): Promise<void>;

  function resetGenericPassword (
    service?: string,
    callback?: (error?: Error) => void
  ): Promise<void>;

}
