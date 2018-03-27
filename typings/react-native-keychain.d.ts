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

    export enum SecAccessible {
        WHEN_UNLOCKED = "AccessibleWhenUnlocked",
        AFTER_FIRST_UNLOCK = "AccessibleAfterFirstUnlock",
        ALWAYS = "AccessibleAlways",
        WHEN_PASSCODE_SET_THIS_DEVICE_ONLY = "AccessibleWhenPasscodeSetThisDeviceOnly",
        WHEN_UNLOCKED_THIS_DEVICE_ONLY = "AccessibleWhenUnlockedThisDeviceOnly",
        AFTER_FIRST_UNLOCK_THIS_DEVICE_ONLY = "AccessibleAfterFirstUnlockThisDeviceOnly",
        ALWAYS_THIS_DEVICE_ONLY = "AccessibleAlwaysThisDeviceOnly"
    }

    export enum SecAccessControl {
        USER_PRESENCE = "UserPresence",
        BIOMETRY_ANY = "BiometryAny",
        BIOMETRY_CURRENT_SET = "BiometryCurrentSet",
        DEVICE_PASSCODE = "DevicePasscode",
        APPLICATION_PASSWORD = "ApplicationPassword",
        BIOMETRY_ANY_OR_DEVICE_PASSCODE = "BiometryAnyOrDevicePasscode",
        BIOMETRY_CURRENT_SET_OR_DEVICE_PASSCODE = "BiometryCurrentSetOrDevicePasscode"
    }

    export enum LAPolicy {
        DEVICE_PASSCODE_OR_BIOMETRICS = "AuthenticationWithBiometricsDevicePasscode",
        BIOMETRICS = "AuthenticationWithBiometrics"
    }

    export interface Options {
        accessControl?: SecAccessControl;
        accessGroup?: string;
        accessible?: SecAccessible;
        authenticationPrompt?: string;
        authenticationType?: LAPolicy;
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
        server: string
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
