"use strict";

// Default authentication prompt options
export const AUTH_PROMPT_DEFAULTS = {
  title: 'Authenticate to retrieve secret',
  cancel: 'Cancel'
};
export function normalizeAuthPrompt(
// eslint-disable-next-line @typescript-eslint/no-explicit-any
options = {}) {
  const {
    authenticationPrompt
  } = options;
  options.authenticationPrompt = {
    ...AUTH_PROMPT_DEFAULTS,
    ...authenticationPrompt
  };
  return options;
}
//# sourceMappingURL=normalizeOptions.js.map