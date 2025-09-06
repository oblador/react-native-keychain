import { ERROR_CODE } from './enums';

interface NativeErrorObject {
  code: ERROR_CODE;
  message: string;
}

const isNativeErrorObject = (err: unknown): err is NativeErrorObject => {
  if (!err || typeof err !== "object") {
    return false;
  }

  return "code" in err && "message" in err;
};

/**
 * Custom error class for encapsulating the native error objects.
 */
class KeychainError extends Error {
  readonly code: ERROR_CODE;

  constructor(message: string, code: ERROR_CODE) {
    super(message);

    this.name = 'KeychainError';
    this.code = code;
  }

  static parse(err: unknown) {
    if (err instanceof Error) {
      return err;
    }

    if (isNativeErrorObject(err)) {
      return new KeychainError(err.message, err.code);
    }

    return new KeychainError('An unknown error occurred', ERROR_CODE.INTERNAL_ERROR);
  }
}

export default KeychainError;
