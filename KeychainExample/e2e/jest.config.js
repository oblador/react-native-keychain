/** @type {import('@jest/types').Config.InitialOptions} */
module.exports = {
  rootDir: '..',
  testMatch: ['<rootDir>/e2e/**/*.spec.js'],
  // Native sheets, authentication and result checks can take over two minutes
  // on hosted simulators, especially during the first run after boot.
  testTimeout: 180000,
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
