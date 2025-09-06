import { ERROR_CODE } from './enums';

const RETRYABLE_ERROR_CODES = [
  ERROR_CODE.BIOMETRIC_TIMEOUT,
  ERROR_CODE.BIOMETRIC_LOCKOUT,
  ERROR_CODE.BIOMETRIC_TEMPORARILY_UNAVAILABLE,
];

/**
 * Custom error class for encapsulating the native error objects.
 */
export class KeychainError extends Error {
  readonly code: ERROR_CODE;
  readonly retryable: boolean;
  readonly cause: Error | null;

  constructor(message: string, code: ERROR_CODE, cause?: Error) {
    super(message);

    this.name = 'KeychainError';
    this.code = code;
    this.retryable = RETRYABLE_ERROR_CODES.includes(code);
    this.cause = cause ?? null;
  }

  static parse(err: unknown) {
    if (err instanceof Error) {
      const code =
        'code' in err ? (err.code as ERROR_CODE) : ERROR_CODE.INTERNAL_ERROR;

      return new KeychainError(err.message, code, err);
    }

    return err;
  }
}

export default KeychainError;
