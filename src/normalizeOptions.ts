import type { AuthenticationPrompt, Options } from './types';

// Default authentication prompt options
export const AUTH_PROMPT_DEFAULTS: AuthenticationPrompt = {
  title: 'Authenticate to retrieve secret',
  cancel: 'Cancel',
};

export function normalizeServiceOption(
  serviceOrOptions?: string | Options
): Options {
  if (typeof serviceOrOptions === 'string') {
    console.warn(
      `You passed a service string as an argument to one of the react-native-keychain functions.
            This way of passing service is deprecated and will be removed in a future major.
            Please update your code to use { service: ${JSON.stringify(
              serviceOrOptions
            )} }`
    );
    return { service: serviceOrOptions };
  }
  return serviceOrOptions || {};
}

export function normalizeServerOption(
  serverOrOptions?: string | Options
): Options {
  if (typeof serverOrOptions === 'string') {
    console.warn(
      `You passed a server string as an argument to one of the react-native-keychain functions.
            This way of passing service is deprecated and will be removed in a future major.
            Please update your code to use { service: ${JSON.stringify(
              serverOrOptions
            )} }`
    );
    return { server: serverOrOptions };
  }
  return serverOrOptions || {};
}

export function normalizeOptions(serviceOrOptions?: string | Options): Options {
  const options = {
    ...normalizeServiceOption(serviceOrOptions),
  } as Options;
  const { authenticationPrompt } = options;

  if (typeof authenticationPrompt === 'string') {
    console.warn(
      `You passed a authenticationPrompt string as an argument to one of the react-native-keychain functions.
            This way of passing authenticationPrompt is deprecated and will be removed in a future major.
            Please update your code to use { authenticationPrompt: { title: ${JSON.stringify(
              authenticationPrompt
            )} }`
    );
    options.authenticationPrompt = {
      ...AUTH_PROMPT_DEFAULTS,
      title: authenticationPrompt,
    };
  } else {
    options.authenticationPrompt = {
      ...AUTH_PROMPT_DEFAULTS,
      ...authenticationPrompt,
    };
  }

  return options;
}
