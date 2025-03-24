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

async function expectRegexText(regex: RegExp) {
  // toHaveText does not support regex on iOS
  // by.text(regex) is flakey on Android
  if (device.getPlatform() === 'android') {
    const text = await expectCredentialsMessage();
    // @ts-expect-error - regex pattern is not recognized by TS
    await expect(text).toHaveText(regex);
    return;
  }
  await expect(element(by.text(regex))).toBeVisible();
}

export async function expectCredentialsSavedMessage() {
  const regex = /^Credentials saved! .*$/;
  await expectRegexText(regex);
}

export async function expectCredentialsResetMessage() {
  const regex = /^Credentials Reset!$/;
  expectRegexText(regex);
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
  expectRegexText(regex);
}
