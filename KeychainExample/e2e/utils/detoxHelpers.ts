import { by, element, waitFor, device } from 'detox';

export const ResetDevice = async () => {
  await device.launchApp({ newInstance: true });
};

export function waitForRegexText(regex: RegExp, timeout?: number) {
  return timeout
    ? waitFor(element(by.text(regex)))
        .toBeVisible()
        .withTimeout(timeout)
    : waitFor(element(by.text(regex))).toBeVisible();
}
