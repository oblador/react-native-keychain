import { by, element, expect, device } from 'detox';

export const ResetDevice = async () => {
  await device.launchApp({ delete: true, newInstance: true });
};

export function expectRegexText(regex: RegExp) {
  return expect(element(by.text(regex)));
}
