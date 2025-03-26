import { by, element, waitFor, expect } from 'detox';

async function retry<T>(
  operation: () => Promise<T>,
  maxAttempts: number = 3,
  delayMs: number = 1000
): Promise<T> {
  let attempts = 0;

  while (attempts < maxAttempts) {
    try {
      return await operation();
    } catch (error) {
      attempts++;
      if (attempts === maxAttempts) {
        throw error;
      }
      await new Promise((resolve) => setTimeout(resolve, delayMs));
    }
  }
  throw new Error('Unreachable code');
}

export async function expectRegexText(regex: RegExp, timeout?: number) {
  try {
    return await retry(async () =>
      timeout
        ? waitFor(element(by.text(regex)))
            .toBeVisible()
            .withTimeout(timeout)
        : expect(element(by.text(regex))).toBeVisible()
    );
  } catch (error) {
    throw new Error(`Failed to find text matching ${regex}: ${error}`);
  }
}
