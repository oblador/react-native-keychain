import type { AuthenticationPrompt, GetOptions, SetOptions } from './types';

// Default authentication prompt options
export const AUTH_PROMPT_DEFAULTS: AuthenticationPrompt = {
  title: 'Authenticate to retrieve secret',
  cancel: 'Cancel',
};

export function normalizeAuthPrompt<T extends SetOptions | GetOptions>(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  options: T = {} as any
) {
  return {
    ...options,
    authenticationPrompt: {
      ...AUTH_PROMPT_DEFAULTS,
      ...options.authenticationPrompt,
    },
  };
}
