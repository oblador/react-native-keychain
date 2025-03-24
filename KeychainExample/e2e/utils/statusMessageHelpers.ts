import { waitForRegexText } from './detoxHelpers';

const TIMEOUT = 10000;

function buildLoadedCredentialsRegex(
  username: string,
  password: string,
  storage?: string,
  service?: string
): RegExp {
  let pattern = '^Credentials loaded! .*';
  // Conditionally add storage if provided.
  if (storage) {
    pattern += `"storage":"${storage}",`;
  }
  // Always add password and username.
  pattern += `"password":"${password}","username":"${username}"`;
  // Conditionally add service if provided.
  if (service) {
    pattern += `,"service":"${service}"`;
  }
  pattern += '.*$';
  return new RegExp(pattern);
}

export async function expectCredentialsSavedMessage() {
  const regex = /^Credentials saved! .*$/;
  await waitForRegexText(regex, TIMEOUT);
}

export async function expectCredentialsResetMessage() {
  const regex = /^Credentials Reset!$/;
  await waitForRegexText(regex, TIMEOUT);
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
  await waitForRegexText(regex, TIMEOUT);
}
