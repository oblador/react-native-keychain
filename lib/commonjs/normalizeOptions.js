"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AUTH_PROMPT_DEFAULTS = void 0;
exports.normalizeAuthPrompt = normalizeAuthPrompt;
// Default authentication prompt options
const AUTH_PROMPT_DEFAULTS = exports.AUTH_PROMPT_DEFAULTS = {
  title: 'Authenticate to retrieve secret',
  cancel: 'Cancel'
};
function normalizeAuthPrompt(
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