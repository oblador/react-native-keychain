import { by, element, expect, waitFor } from 'detox';
const statusTestID = 'statusMessage';

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

async function expectCredentialsMessage() {
  await waitFor(element(by.id(statusTestID))).toBeVisible();
  return element(by.id(statusTestID));
}

export async function expectCredentialsSavedMessage() {
  const text = await expectCredentialsMessage();
  const regex = /^Credentials saved! .*$/;
  // @ts-expect-error - regex pattern is not recognized by TS
  await expect(text).toHaveText(regex);
}

export async function expectCredentialsResetMessage() {
  const text = await expectCredentialsMessage();
  const regex = /^Credentials Reset!$/;
  // @ts-expect-error - regex pattern is not recognized by TS
  await expect(text).toHaveText(regex);
}

export async function expectCredentialsLoadedMessage(
  username: string,
  password: string,
  storage?: string,
  service?: string
) {
  const text = await expectCredentialsMessage();
  const regex = buildLoadedCredentialsRegex(
    username,
    password,
    storage,
    service
  );
  // @ts-expect-error - regex pattern is not recognized by TS
  await expect(text).toHaveText(regex);
}
