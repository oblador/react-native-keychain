// Detox forwards the selected configuration so the default CI commands honor
// the platform tags used by the native-only test cases.
const configuration = process.env.DETOX_CONFIGURATION || '';
const excludedTag = configuration.startsWith('ios.')
  ? ':android:'
  : configuration.startsWith('android.')
    ? ':ios:'
    : null;

/** @type {import('@jest/types').Config.InitialOptions} */
module.exports = {
  rootDir: '..',
  testMatch: ['<rootDir>/e2e/**/*.spec.js'],
  testTimeout: 120000,
  testNamePattern: excludedTag ? `^(?!.*${excludedTag})` : undefined,
  maxWorkers: 1,
  globalSetup: 'detox/runners/jest/globalSetup',
  globalTeardown: 'detox/runners/jest/globalTeardown',
  testEnvironment: 'detox/runners/jest/testEnvironment',
  setupFilesAfterEnv: ['<rootDir>/e2e/setup.ts'],
  reporters: [
    'detox/runners/jest/reporter',
    ['jest-junit', { outputDirectory: 'e2e/output', outputName: 'report.xml' }],
  ],
  verbose: true,
};
