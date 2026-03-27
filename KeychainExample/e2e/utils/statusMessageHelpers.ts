import { expectRegexText } from './detoxHelpers';

const TIMEOUT = 10000;

function buildLoadedCredentialsRegex(
  username: string,
  password: string,
  storage?: string,
  service?: string
): RegExp {
  // Use lookaheads to match each field regardless of key order in JSON output
  let pattern = '^Credentials loaded! ';
  if (storage) {
    pattern += `(?=.*"storage":"${storage}")`;
  }
  pattern += `(?=.*"password":"${password}")`;
  pattern += `(?=.*"username":"${username}")`;
  if (service) {
    pattern += `(?=.*"service":"${service}")`;
  }
  pattern += '.*$';
  return new RegExp(pattern);
}

export async function expectCredentialsSavedMessage() {
  const regex = /^Credentials saved! .*$/;
  await expectRegexText(regex, TIMEOUT);
}

export async function expectCredentialsResetMessage() {
  const regex = /^Credentials Reset!$/;
  await expectRegexText(regex, TIMEOUT);
}

export async function expectCredentialsLoadedMessage(
  username: string,
  password: string,
  storage?: string,
  service?: string
) {
  const regex = buildLoadedCredentialsRegex(
    username,
    password,
    storage,
    service
  );
  await expectRegexText(regex, TIMEOUT);
}
