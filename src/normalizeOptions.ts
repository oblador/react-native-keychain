import type { AuthenticationPrompt, GetOptions, SetOptions } from './types';

// Default authentication prompt options
export const AUTH_PROMPT_DEFAULTS: AuthenticationPrompt = {
  title: 'Authenticate to retrieve secret',
  cancel: 'Cancel',
};
  const { authenticationPrompt } = options;

  options.authenticationPrompt = {
    ...AUTH_PROMPT_DEFAULTS,
    ...authenticationPrompt,
  };

  return options;
}
