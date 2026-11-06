import { by, element, expect, waitFor } from 'detox';

// Controls move below the fold on small phones and with larger text sizes.
export async function scrollToControl(id: string) {
  const target = element(by.id(id));
  try {
    await expect(target).toBeVisible();
    return;
  } catch {
    await element(by.id('workbenchScroll')).scrollTo('top');
  }
  await waitFor(target)
    .toBeVisible()
    .whileElement(by.id('workbenchScroll'))
    .scroll(160, 'down');
}

export async function selectOption(id: string) {
  const group = id.split('-')[0];
  if (group !== 'type') {
    const pickerID = `${group}Picker`;
    await scrollToControl(pickerID);
    await element(by.id(pickerID)).tap();
    await waitFor(element(by.id(id)))
      .toBeVisible()
      .withTimeout(3000);
  } else {
    await scrollToControl(id);
  }
  await element(by.id(id)).tap();
  if (group === 'type') {
    await waitFor(element(by.id('credentialScope')))
      .toHaveText(
        id === 'type-internetCredentials'
          ? 'https://example.com'
          : 'Default app service'
      )
      .withTimeout(3000);
  }
}
